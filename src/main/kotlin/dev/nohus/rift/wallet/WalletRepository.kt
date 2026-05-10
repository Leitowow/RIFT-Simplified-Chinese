package dev.nohus.rift.wallet

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.contacts.ContactsRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.location.LocationRepository
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.ContextIdType
import dev.nohus.rift.network.esi.models.UniverseNamesCategory
import dev.nohus.rift.network.esi.models.WalletJournalEntry
import dev.nohus.rift.network.esi.models.WalletTransaction
import dev.nohus.rift.network.esi.pagination.fetchOffsetIdPaginated
import dev.nohus.rift.network.esi.pagination.fetchPagePaginated
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.CelestialsRepository
import dev.nohus.rift.repositories.FactionNames
import dev.nohus.rift.repositories.GetSystemDistanceUseCase
import dev.nohus.rift.repositories.IdRanges
import dev.nohus.rift.repositories.NamesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.StationsRepository
import dev.nohus.rift.repositories.StationsRepository.Station
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.sso.scopes.ScopeGroups
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.skiko.MainUIDispatcher
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Single
class WalletRepository(
    private val localCharactersRepository: LocalCharactersRepository,
    private val locationRepository: LocationRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
    private val contactsRepository: ContactsRepository,
    private val esiApi: EsiApi,
    private val walletLocalRepository: WalletLocalRepository,
    private val namesRepository: NamesRepository,
    private val celestialsRepository: CelestialsRepository,
    private val walletDivisionsRepository: WalletDivisionsRepository,
    private val stationsRepository: StationsRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val getSystemDistanceUseCase: GetSystemDistanceUseCase,
    private val settings: Settings,
) {

    data class State(
        val loadedState: Result<LoadedState>? = null,
        val loading: LoadingState = LoadingState(),
    )

    data class LoadedState(
        val journal: List<WalletJournalItem>,
        val balances: List<WalletBalance>,
        val loyaltyPoints: List<CharacterLoyaltyPoints>,
        val characters: List<Character>,
        val corporations: List<Corporation>,
    )

    data class LoadingState(
        val stage: LoadingStage? = null,
        val characters: List<LoadingCharacter> = emptyList(),
        val corporationDivisions: List<LoadingCorporationDivision> = emptyList(),
        val totalJournalItems: Int = 0,
        val typeDetails: LoadingTypeDetails = LoadingTypeDetails(),
    )

    data class LoadingCharacter(
        val characterId: Int,
        val name: String,
        val hasCorpWalletScopes: Boolean,
        val loadedJournalItems: Int = 0,
        val loadedTransactions: Int = 0,
        val isJournalLoaded: Boolean = false,
    )

    data class LoadingCorporationDivision(
        val corporationId: Int,
        val divisionId: Int,
        val name: String,
        val loadedJournalItems: Int = 0,
        val loadedTransactions: Int = 0,
        val isJournalLoaded: Boolean = false,
    )

    data class LoadingTypeDetails(
        val structureIds: Int = 0,
        val characterIds: Int = 0,
        val groupIds: Int = 0,
        val systemIds: Int = 0,
        val typeIds: Int = 0,
        val isStructuresLoaded: Boolean = false,
        val isCharactersLoaded: Boolean = false,
        val isGroupsLoaded: Boolean = false,
    )

    sealed interface LoadingStage {
        data object LoadingJournal : LoadingStage
        data object LoadingDatabase : LoadingStage
        data object LoadingTypeDetails : LoadingStage
        data object LoadingDivisionNames : LoadingStage
        data object LoadingBalances : LoadingStage
    }

    sealed class WalletBalance(open val balance: Double) {
        data class Character(
            val characterId: Int,
            override val balance: Double,
        ) : WalletBalance(balance)

        data class Corporation(
            val corporationId: Int,
            val divisionId: Int,
            override val balance: Double,
        ) : WalletBalance(balance)
    }

    data class CharacterLoyaltyPoints(
        /**
         * Character the LP belong to, or null if this is a total from all characters
         */
        val characterId: Int?,
        val balances: List<LoyaltyPoints>,
    )

    data class LoyaltyPoints(
        val corporationId: Int,
        val name: String,
        val closestLoyaltyPointStore: Station?,
        val balance: Long,
    )

    data class Character(
        val id: Int,
        val name: String,
    )

    data class Corporation(
        val id: Int,
        val name: String,
    )

    /**
     * All raw entries in a wallet, and responsible character (owner of a personal wallet or accountant of a corp division wallet)
     */
    private data class WalletEntries(
        val wallet: Wallet,
        val characterId: Int,
        val journal: List<WalletJournalEntry>,
        val transactions: List<WalletTransaction>,
    )

    private val _state = MutableStateFlow<State>(State())
    val state = _state.asStateFlow()

    private val reloadFlow = MutableSharedFlow<Unit>()
    private val loadingMutex = Mutex()
    private var isRealtime = false

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                delay(1.minutes)
                if (isRealtime) reloadFlow.emit(Unit)
            }
        }
        launch {
            while (true) {
                delay(15.minutes)
                reloadFlow.emit(Unit)
            }
        }
        launch {
            localCharactersRepository.characters.debounce(500).collectLatest { characters ->
                // Wait for contacts to load before loading wallets, unless they take too long
                withTimeoutOrNull(10_000) {
                    contactsRepository.finishedLoading.filter { it }.first()
                }
                reloadFlow.emit(Unit)
            }
        }
        launch {
            reloadFlow.collect {
                load()
            }
        }
    }

    suspend fun reload() {
        if (!loadingMutex.isLocked) reloadFlow.emit(Unit)
    }

    suspend fun setNeedsRealtimeUpdates(isRealtime: Boolean) {
        this.isRealtime = isRealtime
        if (isRealtime) {
            reloadFlow.emit(Unit)
        }
    }

    private suspend fun load() = withContext(Dispatchers.Default) {
        loadingMutex.withLock {
            val localCharacters = localCharactersRepository.characters.value
            if (localCharacters.isEmpty()) return@withContext

            val charactersWithWalletScopes = localCharacters
                .filter { ScopeGroups.readWallet in it.scopes }
            val charactersWithLoyaltyPointScopes = localCharacters
                .filter { ScopeGroups.readLoyaltyPoints in it.scopes }
            val characterWithCorpWalletScopes = localCharacters
                .filter { ScopeGroups.readCorporationWallet in it.scopes }

            // Update loading progress
            val loadingCharacters = localCharacters
                .filter { it in charactersWithWalletScopes || it in characterWithCorpWalletScopes }
                .map { character ->
                    LoadingCharacter(
                        characterId = character.characterId,
                        name = character.info?.name ?: character.characterId.toString(),
                        hasCorpWalletScopes = characterWithCorpWalletScopes.any { it.characterId == character.characterId },
                    )
                }
            _state.update {
                it.copy(loading = LoadingState(characters = loadingCharacters))
            }

            val accountants = characterWithCorpWalletScopes.filter { character ->
                val roles = character.info?.corporationRoles ?: emptyList()
                "Accountant" in roles || "Junior_Accountant" in roles
            }.map { it.characterId }
            val directors = characterWithCorpWalletScopes.filter { character ->
                val roles = character.info?.corporationRoles ?: emptyList()
                "Director" in roles
            }

            val divisionNamesJob = launch {
                walletDivisionsRepository.load(directors)
            }

            val corporationsToAccountantIds = localCharacters
                .filter { it.characterId in accountants }
                .mapNotNull { character ->
                    val corporationId = character.info?.corporationId
                    val corporationName = character.info?.corporationName
                    if (corporationId == null || corporationName == null) {
                        logger.error { "Character ${character.characterId} has no corporation ID or name" }
                        return@mapNotNull null
                    }
                    Corporation(corporationId, corporationName) to character.characterId
                }
                .groupBy { it.first }
                .mapValues { it.value.map { it.second }.first() }

            val walletBalancesDeferred = async { getWalletBalances(charactersWithWalletScopes, corporationsToAccountantIds) }
            val loyaltyPointBalancesDeferred = async { getLoyaltyPoints(charactersWithLoyaltyPointScopes) }

            updateDatabaseFromEsi(charactersWithWalletScopes, corporationsToAccountantIds)

            _state.update { it.copy(loading = it.loading.copy(stage = LoadingStage.LoadingDatabase)) }
            val journalEntriesPerWallet = walletLocalRepository.loadJournalEntries()
            val transactions = walletLocalRepository.loadTransactions()

            val walletEntries = journalEntriesPerWallet.mapNotNull { (wallet, journalEntries) ->
                val characterId = when (wallet) {
                    is Wallet.Character -> wallet.characterId
                    is Wallet.Corporation ->
                        corporationsToAccountantIds.entries
                            .firstOrNull { it.key.id == wallet.corporationId }?.value ?: return@mapNotNull null
                }
                val journalEntryIds = journalEntries.map { it.id }
                val walletTransactions = transactions.filter { it.journalRefId in journalEntryIds }
                WalletEntries(wallet, characterId, journalEntries, walletTransactions)
            }

            _state.update {
                it.copy(
                    loading = it.loading.copy(
                        stage = LoadingStage.LoadingTypeDetails,
                        totalJournalItems = journalEntriesPerWallet.values.sumOf { it.size },
                    ),
                )
            }
            val details = loadTypeDetails(walletEntries)

            val items = walletEntries.map {
                async {
                    buildItems(it.wallet, it.journal, it.transactions, details)
                }
            }.awaitAll().flatten()

            val characters = (charactersWithWalletScopes + charactersWithLoyaltyPointScopes).distinct().map {
                Character(it.characterId, it.info?.name ?: it.characterId.toString())
            }
            val corporations = corporationsToAccountantIds.keys.toList()

            _state.update { it.copy(loading = it.loading.copy(stage = LoadingStage.LoadingDivisionNames)) }
            divisionNamesJob.join()
            _state.update { it.copy(loading = it.loading.copy(stage = LoadingStage.LoadingBalances)) }
            val walletBalances = walletBalancesDeferred.await()
            val loyaltyPointBalances = loyaltyPointBalancesDeferred.await()
            _state.update {
                it.copy(
                    loadedState = Result.Success(
                        LoadedState(
                            journal = items,
                            balances = walletBalances,
                            loyaltyPoints = loyaltyPointBalances,
                            characters = characters,
                            corporations = corporations,
                        ),
                    ),
                    loading = LoadingState(),
                )
            }
        }
    }

    private suspend fun getWalletBalances(
        charactersToLoad: List<LocalCharacter>,
        corporationsToAccountantIds: Map<Corporation, Int>,
    ): List<WalletBalance> {
        return coroutineScope {
            val characterWalletsDeferred = charactersToLoad.map { character ->
                async {
                    esiApi.getCharacterIdWallet(Originator.Wallets, character.characterId).map {
                        WalletBalance.Character(character.characterId, it)
                    }.success
                }
            }
            val corporationWalletsDeferred = corporationsToAccountantIds.map { (corporation, accountantId) ->
                async {
                    esiApi.getCorporationsCorporationIdWallet(Originator.Wallets, accountantId, corporation.id).map { wallets ->
                        wallets.map { wallet ->
                            WalletBalance.Corporation(corporation.id, wallet.divisionId, wallet.balance)
                        }
                    }.success
                }
            }
            val characterWallets = characterWalletsDeferred.awaitAll().filterNotNull()
            val corporationWallets = corporationWalletsDeferred.awaitAll().filterNotNull().flatten()
            characterWallets + corporationWallets
        }
    }

    private suspend fun getLoyaltyPoints(
        charactersToLoad: List<LocalCharacter>,
    ): List<CharacterLoyaltyPoints> {
        return coroutineScope {
            val loyaltyPointsByCharacterId = charactersToLoad.map { character ->
                async {
                    character.characterId to esiApi.getCharactersIdLoyaltyPoints(Originator.Wallets, character.characterId).success
                        ?.filter { it.loyaltyPoints > 0 }
                }
            }.awaitAll().associate {
                it.first to (it.second ?: emptyList())
            }
            val corporationNamesById = loyaltyPointsByCharacterId.values.flatten().map { it.corporationId }.distinct()
                .associateWith { corporationId -> esiApi.getCorporationsId(Originator.Wallets, corporationId.toInt()).success?.name }
            val characterLoyaltyPoints = loyaltyPointsByCharacterId.map { (characterId, loyaltyPoints) ->
                val characterSolarSystemId = characterLocationRepository.locations.value[characterId]?.solarSystemId
                CharacterLoyaltyPoints(
                    characterId = characterId,
                    balances = loyaltyPoints.map {
                        val store = stationsRepository.getLoyaltyPointStores(it.corporationId.toInt()).minByOrNull {
                            if (characterSolarSystemId != null) {
                                getSystemDistanceUseCase(characterSolarSystemId, it.systemId, settings.isUsingJumpBridgesForDistance) ?: Int.MAX_VALUE
                            } else {
                                Int.MAX_VALUE
                            }
                        }
                        val name = if (it.corporationId == 1000419L) "EverMarks" else corporationNamesById[it.corporationId] ?: it.corporationId.toString()
                        LoyaltyPoints(
                            corporationId = it.corporationId.toInt(),
                            name = name,
                            closestLoyaltyPointStore = store,
                            balance = it.loyaltyPoints,
                        )
                    },
                )
            }
            val totalLoyaltyPoints = characterLoyaltyPoints
                .flatMap { it.balances }
                .groupBy { it.corporationId }
                .mapValues { (corporationId, balances) ->
                    LoyaltyPoints(
                        corporationId = corporationId,
                        name = balances.first().name,
                        closestLoyaltyPointStore = null,
                        balance = balances.sumOf { it.balance },
                    )
                }.values.toList().let {
                    CharacterLoyaltyPoints(
                        characterId = null,
                        balances = it,
                    )
                }
            listOf(totalLoyaltyPoints) + characterLoyaltyPoints
        }
    }

    private suspend fun updateLoadingCharacter(characterId: Int, update: LoadingCharacter.() -> LoadingCharacter) {
        withContext(MainUIDispatcher) {
            _state.update {
                it.copy(
                    loading = it.loading.copy(
                        characters = it.loading.characters.map {
                            if (it.characterId == characterId) {
                                it.update()
                            } else {
                                it
                            }
                        },
                    ),
                )
            }
        }
    }

    private suspend fun updateLoadingCorporationDivision(corporationId: Int, divisionId: Int, update: LoadingCorporationDivision.() -> LoadingCorporationDivision) {
        withContext(MainUIDispatcher) {
            _state.update {
                it.copy(
                    loading = it.loading.copy(
                        corporationDivisions = it.loading.corporationDivisions.map {
                            if (it.corporationId == corporationId && it.divisionId == divisionId) {
                                it.update()
                            } else {
                                it
                            }
                        },
                    ),
                )
            }
        }
    }

    /**
     * Gets new journal entries and transactions from ESI and saves them to the database
     */
    private suspend fun updateDatabaseFromEsi(
        charactersToLoad: List<LocalCharacter>,
        corporationsToAccountantIds: Map<Corporation, Int>,
    ) {
        _state.update {
            it.copy(loading = it.loading.copy(stage = LoadingStage.LoadingJournal))
        }
        coroutineScope {
            charactersToLoad.forEach { character ->
                val wallet = Wallet.Character(character.characterId)
                launch {
                    val characterId = character.characterId
                    val deferredJournal = async {
                        fetchPagePaginated(
                            onProgressUpdate = { loadedItems ->
                                updateLoadingCharacter(characterId) {
                                    copy(loadedJournalItems = loadedItems)
                                }
                            },
                        ) {
                            esiApi.getCharactersIdWalletJournal(Originator.Wallets, characterId, it)
                        }
                    }
                    val deferredTransactions = async {
                        fetchOffsetIdPaginated(
                            onProgressUpdate = { loadedItems ->
                                updateLoadingCharacter(characterId) {
                                    copy(loadedTransactions = loadedItems)
                                }
                            },
                        ) {
                            esiApi.getCharactersIdWalletTransactions(Originator.Wallets, characterId, it)
                        }
                    }
                    val journalEntries = deferredJournal.await()
                    val transactions = deferredTransactions.await()
                    updateLoadingCharacter(characterId) {
                        copy(isJournalLoaded = true)
                    }

                    if (transactions is Result.Success && journalEntries is Result.Success) {
                        walletLocalRepository.save(wallet, journalEntries.data)
                        walletLocalRepository.save(transactions.data)
                    } else {
                        // TODO
                        if (journalEntries is Result.Failure) {
                            logger.error { "Failed to fetch journal entries for character $characterId: ${journalEntries.cause}" }
                        }
                        if (transactions is Result.Failure) {
                            logger.error { "Failed to fetch transactions for character $characterId: ${transactions.cause}" }
                        }
                    }
                }
            }

            _state.update {
                it.copy(
                    loading = it.loading.copy(
                        corporationDivisions = corporationsToAccountantIds.keys.flatMap { corporation ->
                            (1..7).map { divisionId ->
                                LoadingCorporationDivision(
                                    corporationId = corporation.id,
                                    divisionId = divisionId,
                                    name = "${corporation.name}，分账 $divisionId",
                                )
                            }
                        },
                    ),
                )
            }
            corporationsToAccountantIds.forEach { (corporation, accountantId) ->
                launch {
                    (1..7).forEach { divisionId ->
                        val wallet = Wallet.Corporation(corporation.id, divisionId)
                        launch {
                            val deferredJournal = async {
                                fetchPagePaginated(
                                    onProgressUpdate = { loadedItems ->
                                        updateLoadingCorporationDivision(corporation.id, divisionId) {
                                            copy(loadedJournalItems = loadedItems)
                                        }
                                    },
                                ) {
                                    esiApi.getCorporationsCorporationIdWalletsDivisionJournal(Originator.Wallets, accountantId, corporation.id, divisionId, it)
                                }
                            }
                            val deferredTransactions = async {
                                fetchOffsetIdPaginated(
                                    onProgressUpdate = { loadedItems ->
                                        updateLoadingCorporationDivision(corporation.id, divisionId) {
                                            copy(loadedTransactions = loadedItems)
                                        }
                                    },
                                ) {
                                    esiApi.getCorporationsCorporationIdWalletsDivisionTransactions(Originator.Wallets, accountantId, corporation.id, divisionId, it)
                                }
                            }
                            val journalEntries = deferredJournal.await()
                            val transactions = deferredTransactions.await()
                            updateLoadingCorporationDivision(corporation.id, divisionId) {
                                copy(isJournalLoaded = true)
                            }

                            if (transactions is Result.Success && journalEntries is Result.Success) {
                                walletLocalRepository.save(wallet, journalEntries.data)
                                walletLocalRepository.save(transactions.data)
                            } else {
                                // TODO
                                if (journalEntries is Result.Failure) {
                                    logger.error { "Failed to fetch journal entries for corporation $corporation division $divisionId: ${journalEntries.cause}" }
                                }
                                if (transactions is Result.Failure) {
                                    logger.error { "Failed to fetch transactions for corporation $corporation division $divisionId: ${transactions.cause}" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds a list of [WalletJournalItem]s from the given journal entries and transactions.
     */
    private fun buildItems(
        wallet: Wallet,
        journalEntries: List<WalletJournalEntry>,
        transactions: List<WalletTransaction>,
        typeDetails: TypeDetails,
    ): List<WalletJournalItem> {
        val entries = fixJournalEntries(journalEntries)
        val transactionsByJournalId = transactions.associateBy { it.journalRefId }
        val items = entries.map { entry ->
            val transaction = transactionsByJournalId[entry.id]?.let {
                WalletTransactionItem(
                    client = typeDetails[it.clientId],
                    date = it.date,
                    isBuy = it.isBuy,
                    isPersonal = it.isPersonal,
                    location = typeDetails[it.locationId],
                    quantity = it.quantity,
                    transactionId = it.transactionId,
                    type = typeDetails[it.typeId],
                    unitPrice = it.unitPrice,
                )
            }
            WalletJournalItem(
                wallet = wallet,
                amount = entry.amount ?: 0.0,
                balance = entry.balance,
                context = typeDetails[entry.contextId],
                date = entry.date,
                description = entry.description,
                id = entry.id,
                reason = entry.reason,
                reasonTypeDetails = getAdditionalTypeDetails(entry.refType, entry.reason, entry.description),
                refType = entry.refType,
                firstParty = typeDetails[entry.firstPartyId],
                secondParty = typeDetails[entry.secondPartyId],
                tax = entry.tax,
                taxReceiver = typeDetails[entry.taxReceiverId],
                transaction = transaction,
            )
        }
        return items
    }

    private fun fixJournalEntries(journalEntries: List<WalletJournalEntry>): List<WalletJournalEntry> {
        return journalEntries.map { entry ->
            if (entry.refType == "insurance" && entry.firstPartyId == 2L) {
                // Insurance has a bug where the first party ID is set to 2 instead of the corporation ID of the EVE Central Bank
                entry.copy(firstPartyId = 98099645)
            } else {
                entry
            }
        }
    }

    private fun getAdditionalTypeDetails(
        referenceType: String,
        reason: String?,
        description: String,
    ): List<TypeDetail> {
        if (referenceType == "project_payouts" && reason != null) {
            val map = reason.split(":")
                .mapNotNull {
                    val parts = it.split("=")
                    if (parts.size == 2) parts else null
                }
                .associate { it[0] to it[1] }
            val goalId = map["goal_id"]
            val goalName = map["goal_name"]
            if (goalId != null && goalName != null) {
                return listOf(TypeDetail.CorporationProject(goalId, goalName))
            }
        } else if (referenceType == "freelance_jobs_reward" && reason != null) {
            val map = reason.split(":")
                .mapNotNull {
                    val parts = it.split("=")
                    if (parts.size == 2) parts else null
                }
                .associate { it[0] to it[1] }
            val projectId = map["project_id"]
            val projectName = map["project_name"]
            if (projectId != null && projectName != null) {
                return listOf(TypeDetail.FreelanceProject(projectId, projectName))
            }
        } else if (referenceType == "bounty_prizes" && reason != null) {
            return reason.split(",")
                .mapNotNull {
                    val parts = it.split(": ")
                    if (parts.size == 2) {
                        (parts[0].toIntOrNull() ?: return@mapNotNull null) to (
                            parts[1].toLongOrNull()
                                ?: return@mapNotNull null
                            )
                    } else {
                        null
                    }
                }
                .mapNotNull { (typeId, count) ->
                    val type = typesRepository.getType(typeId) ?: return@mapNotNull null
                    TypeDetail.Type(type, count)
                }
        } else if (referenceType == "planetary_construction") {
            val planetName = description.substringAfter(" built on ")
            val planet = celestialsRepository.getCelestial(planetName)
            if (planet != null) {
                val system = solarSystemsRepository.getSystem(planet.solarSystemId)
                if (system != null) {
                    return listOf(TypeDetail.SolarSystem(system), TypeDetail.Celestial(planet))
                }
            }
        } else if (referenceType == "daily_goal_payouts" && reason != null) {
            when (reason.toIntOrNull()) {
                697658 -> "Scan 5 Signatures"
                697667 -> "Destroy 25 non-capsuleers"
                697670 -> "Mine 2000 units of Ore"
                697671 -> "Manufacture an Item"
                697674 -> "Damage other Capsuleers"
                697675 -> "Shield Boost other Capsuleers"
                697676 -> "Armor Repair other Capsuleers"
                697680 -> "Capture Contested FW Complex"
                697681 -> "Defend Contested FW Complex"
                712805 -> "Earn 50 LP for any corporation"
                712833 -> "Complete 9 Daily Bonus Goals - Alpha"
                1004953 -> "Complete 3 Jumps"
                else -> null
            }?.let { name ->
                return listOf(TypeDetail.DailyGoal(name))
            }
        }
        return emptyList()
    }

    /**
     * Loads details about all the types referenced in journal entries and transactions
     */
    private suspend fun loadTypeDetails(wallets: List<WalletEntries>): TypeDetails {
        // Prepare lists of IDs to fetch details about
        val uncategorizedIds = mutableSetOf<Long>()
        val structureIdsWithCharacterId = mutableSetOf<Pair<Long, Int>>()
        val stationIds = mutableSetOf<Long>()
        val characterIds = mutableSetOf<Long>()
        val corporationIds = mutableSetOf<Long>()
        val allianceIds = mutableSetOf<Long>()
        val factionIds = mutableSetOf<Long>()
        val systemIds = mutableSetOf<Long>()
        val typeIds = mutableSetOf<Long>()

        wallets.forEach { (_, characterId, journalEntries, transactions) ->
            journalEntries.forEach { entry ->
                if (entry.contextId != null) {
                    when (entry.contextIdType) {
                        ContextIdType.Structure -> structureIdsWithCharacterId += entry.contextId to characterId
                        ContextIdType.Station -> stationIds += entry.contextId
                        ContextIdType.MarketTransaction -> {} // We already have transactions, no need to fetch anything
                        ContextIdType.Character -> characterIds += entry.contextId
                        ContextIdType.Corporation -> if (entry.contextId != 1L) {
                            corporationIds += entry.contextId
                        }
                        ContextIdType.Alliance -> allianceIds += entry.contextId
                        ContextIdType.EveSystem -> uncategorizedIds += entry.contextId
                        ContextIdType.IndustryJob -> {} // Not supported
                        ContextIdType.Contract -> {} // Not supported
                        ContextIdType.Planet -> {} // Not supported // TODO
                        ContextIdType.System -> systemIds += entry.contextId
                        ContextIdType.Type -> typeIds += entry.contextId
                        null -> {}
                    }
                }
                uncategorizedIds += listOfNotNull(entry.firstPartyId, entry.secondPartyId)
                if (entry.taxReceiverId != null) corporationIds += entry.taxReceiverId
            }
            transactions.forEach { transaction ->
                uncategorizedIds += transaction.clientId
                if (IdRanges.isStation(transaction.locationId)) {
                    stationIds += transaction.locationId
                } else {
                    structureIdsWithCharacterId += transaction.locationId to characterId
                }
                typeIds += transaction.typeId
            }

            // Spawned items are not supported by ESI for name/category resolution
            val spawnedItemIds = uncategorizedIds.filter { IdRanges.isSpawnedItem(it) }.toSet()
            uncategorizedIds -= spawnedItemIds
            structureIdsWithCharacterId += spawnedItemIds.map { it to characterId }
        }

        _state.update {
            it.copy(
                loading = it.loading.copy(
                    typeDetails = LoadingTypeDetails(
                        structureIds = structureIdsWithCharacterId.size + stationIds.size,
                        characterIds = characterIds.size,
                        groupIds = corporationIds.size + allianceIds.size + factionIds.size,
                        systemIds = systemIds.size,
                        typeIds = typeIds.size,
                    ),
                ),
            )
        }

        // For those IDs where we don't know the category, fetch the categories from ESI
        namesRepository.resolveNames(Originator.Wallets, uncategorizedIds)
        uncategorizedIds.forEach { id ->
            when (namesRepository.getCategory(id)) {
                UniverseNamesCategory.Character -> characterIds += id
                UniverseNamesCategory.Constellation -> {}
                UniverseNamesCategory.Corporation -> corporationIds += id
                UniverseNamesCategory.InventoryType -> typeIds += id
                UniverseNamesCategory.Region -> {}
                UniverseNamesCategory.SolarSystem -> systemIds += id
                UniverseNamesCategory.Station -> stationIds += id
                UniverseNamesCategory.Faction -> factionIds += id
                UniverseNamesCategory.Alliance -> allianceIds += id
                null -> {}
            }
        }

        _state.update {
            it.copy(
                loading = it.loading.copy(
                    typeDetails = LoadingTypeDetails(
                        structureIds = structureIdsWithCharacterId.size + stationIds.size,
                        characterIds = characterIds.size,
                        groupIds = corporationIds.size + allianceIds.size + factionIds.size,
                        systemIds = systemIds.size,
                        typeIds = typeIds.size,
                    ),
                ),
            )
        }

        // Fetch details about all the IDs
        val details = mutableMapOf<Long, TypeDetail>()
        coroutineScope {
            val deferredStructures = structureIdsWithCharacterId.distinct().map { (structureId, characterId) ->
                async {
                    locationRepository.getStructure(Originator.Wallets, structureId, characterId, fetchOwner = true)
                }
            }
            val deferredStations = stationIds.distinct().map {
                async {
                    locationRepository.getStation(Originator.Wallets, it.toInt(), fetchOwner = true)
                }
            }
            deferredStructures.awaitAll().forEach { structure ->
                structure?.let { details[it.structureId] = TypeDetail.Structure(it) }
            }
            deferredStations.awaitAll().forEach { station ->
                station?.let { details[it.stationId.toLong()] = TypeDetail.Station(it) }
            }
            _state.update {
                it.copy(
                    loading = it.loading.copy(
                        typeDetails = it.loading.typeDetails.copy(
                            isStructuresLoaded = true,
                        ),
                    ),
                )
            }

            val deferredCharacters = characterIds.distinct().let {
                async {
                    characterDetailsRepository.getCharacterDetails(Originator.Wallets, it.map(Long::toInt)).values
                }
            }
            deferredCharacters.await().forEach { character ->
                character?.let { details[it.characterId.toLong()] = TypeDetail.Character(it) }
            }
            _state.update {
                it.copy(
                    loading = it.loading.copy(
                        typeDetails = it.loading.typeDetails.copy(
                            isCharactersLoaded = true,
                        ),
                    ),
                )
            }

            val deferredCorporations = corporationIds.distinct().map { corporationId ->
                async {
                    characterDetailsRepository.getCorporationDetails(Originator.Wallets, corporationId.toInt())
                }
            }
            val deferredAlliances = allianceIds.distinct().map {
                async {
                    characterDetailsRepository.getAllianceDetails(Originator.Wallets, it.toInt())
                }
            }

            deferredCorporations.awaitAll().forEach { corporation ->
                corporation?.let { details[corporation.corporationId.toLong()] = TypeDetail.Corporation(corporation) }
            }
            deferredAlliances.awaitAll().forEach { alliance ->
                alliance?.let { details[alliance.allianceId.toLong()] = TypeDetail.Alliance(alliance) }
            }
            _state.update {
                it.copy(
                    loading = it.loading.copy(
                        typeDetails = it.loading.typeDetails.copy(
                            isGroupsLoaded = true,
                        ),
                    ),
                )
            }

            factionIds.distinct().map { id ->
                details[id] = TypeDetail.Faction(id, FactionNames[id.toInt()])
            }
            systemIds.distinct().mapNotNull { id ->
                solarSystemsRepository.getSystem(id.toInt())?.let { details[id] = TypeDetail.SolarSystem(it) }
            }
            typeIds.distinct().mapNotNull { id ->
                typesRepository.getType(id.toInt())?.let { details[id] = TypeDetail.Type(it) }
            }
        }

        return TypeDetails(details)
    }
}
