package dev.nohus.rift.wallet

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.ViewModel
import dev.nohus.rift.network.Result
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.toColor
import dev.nohus.rift.utils.toHsb
import dev.nohus.rift.wallet.WalletRepository.Character
import dev.nohus.rift.wallet.WalletRepository.CharacterLoyaltyPoints
import dev.nohus.rift.wallet.WalletRepository.Corporation
import dev.nohus.rift.wallet.WalletRepository.LoadedState
import dev.nohus.rift.wallet.WalletRepository.LoadingState
import dev.nohus.rift.wallet.WalletRepository.WalletBalance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.absoluteValue

@Factory
class WalletViewModel(
    private val walletRepository: WalletRepository,
    private val typesRepository: TypesRepository,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val loadedData: Result<LoadedData>? = null,
        val filters: WalletFilters = WalletFilters(),
        val availableWalletFilters: AvailableWalletFilters = AvailableWalletFilters(),
        val loading: LoadingState = LoadingState(),
        val isProcessing: Boolean = false,
        val tab: WalletTab = WalletTab.Wallets,
        val insightsTab: InsightsTab = InsightsTab.IncomeByParty,
        val availableTimestamps: List<Duration> = emptyList(),

        val displayTimezone: ZoneId,
        val showCents: Boolean,
    )

    data class LoadedData(
        val filteredJournal: List<WalletJournalItem>,
        val balances: List<WalletBalance>,
        val loyaltyPointBalances: List<CharacterLoyaltyPoints>,
        val statistics: Statistics,
        val characters: List<Character>,
        val corporations: List<Corporation>,
    )

    data class Statistics(
        val journal: List<WalletJournalItem>,
        // Overview
        val income: Double,
        val expenses: Double,
        val balance: Double,
        val incomeGroupSegments: List<Segment>,
        val expensesGroupSegments: List<Segment>,
        val incomeSegments: Map<TransactionGroup, List<Segment>>,
        val expensesSegments: Map<TransactionGroup, List<Segment>>,
        // Insights
        val incomeByParty: List<PartyTransactions>,
        val expensesByParty: List<PartyTransactions>,
        val balanceByParty: List<PartyTransactions>,
        val destroyedRatsByParty: List<PartyKilledRats>,
        val dailyGoals: List<PartyDailyGoals>,
        val activity: List<PartyActivity>,
    )

    data class Segment(
        val group: TransactionGroup?,
        val name: String,
        val color: Color,
        val value: Double,
        val ratio: Double,
    )

    data class PartyTransactions(
        val party: TypeDetail,
        val transferDirection: TransferDirection?,
        val total: Double,
        val refTypes: List<Pair<String, Double>>,
    )

    data class PartyKilledRats(
        val party: TypeDetail,
        val totalBounty: Double,
        val totalRats: Long,
        val destroyedRats: List<TypeDetail.Type>,
        val countBySystem: List<Pair<TypeDetail, Long>>,
    )

    data class PartyDailyGoals(
        val party: TypeDetail,
        val totalReward: Double,
        val totalGoals: Int,
        val dailyGoals: List<Pair<LocalDate, List<TypeDetail.DailyGoal>>>,
    )

    data class PartyActivity(
        val party: TypeDetail,
        val totalBalance: Double,
        val transactions: List<Pair<LocalDate, List<WalletJournalItem>>>,
    )

    enum class WalletTab {
        Wallets,
        Overview,
        Transactions,
        Insights,
        LoyaltyPoints,
    }

    enum class InsightsTab {
        IncomeByParty,
        ExpenseByParty,
        BalanceByParty,
        DestroyedRatsByParty,
        DailyGoals,
        Activity,
    }

    private val _state = MutableStateFlow(
        UiState(
            displayTimezone = settings.displayTimeZone,
            showCents = settings.isShowIskCents,
        ),
    )
    val state = _state.asStateFlow()
    private val updateJournalsFlow = MutableSharedFlow<Unit>()

    init {
        viewModelScope.launch {
            walletRepository.state.map { it.loadedState }.distinctUntilChanged().collect { loadedState ->
                _state.update {
                    it.copy(
                        availableWalletFilters = getAvailableWalletFilters(loadedState),
                    )
                }
                updateJournalsFlow.emit(Unit)
            }
        }
        viewModelScope.launch {
            walletRepository.state.map { it.loading }.collect { loading ->
                _state.update {
                    it.copy(
                        loading = loading,
                    )
                }
            }
        }
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        displayTimezone = settings.displayTimeZone,
                        showCents = settings.isShowIskCents,
                    )
                }
            }
        }
        viewModelScope.launch {
            updateJournalsFlow.collectLatest {
                _state.update { it.copy(isProcessing = true) }
                updateJournals()
                _state.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun onVisibilityChange(visible: Boolean) {
        viewModelScope.launch {
            walletRepository.setNeedsRealtimeUpdates(visible)
        }
    }

    fun onFiltersUpdate(filters: WalletFilters) {
        _state.update { it.copy(filters = filters) }
        updateJournalsFlow.scopedEmit(Unit)
    }

    fun onInsightsTabSelected(tab: InsightsTab) {
        _state.update { it.copy(insightsTab = tab) }
    }

    fun onViewTransactions(partyTransactions: PartyTransactions) {
        _state.update {
            it.copy(
                filters = it.filters.copy(
                    direction = partyTransactions.transferDirection,
                    referenceTypes = emptyList(),
                    party = partyTransactions.party,
                    search = null,
                ),
                tab = WalletTab.Transactions,
            )
        }
        updateJournalsFlow.scopedEmit(Unit)
    }
    fun onViewTransactions(partyDailyGoals: PartyDailyGoals) {
        _state.update {
            it.copy(
                filters = it.filters.copy(
                    direction = null,
                    referenceTypes = listOf("daily_goal_payouts"),
                    party = partyDailyGoals.party,
                    search = null,
                ),
                tab = WalletTab.Transactions,
            )
        }
        updateJournalsFlow.scopedEmit(Unit)
    }

    fun onViewTransactions(partyActivity: PartyActivity) {
        _state.update {
            it.copy(
                filters = it.filters.copy(
                    direction = null,
                    referenceTypes = emptyList(),
                    party = partyActivity.party,
                    search = null,
                ),
                tab = WalletTab.Transactions,
            )
        }
        updateJournalsFlow.scopedEmit(Unit)
    }

    fun onReloadClick() {
        viewModelScope.launch {
            walletRepository.reload()
        }
    }

    fun onTabClick(tab: WalletTab) {
        _state.update { it.copy(tab = tab) }
    }

    private fun getAvailableWalletFilters(loadedState: Result<LoadedState>?): AvailableWalletFilters {
        val loaded = loadedState?.success
        return AvailableWalletFilters(
            characters = loaded?.characters ?: emptyList(),
            corporations = loaded?.corporations ?: emptyList(),
            referenceTypes = loaded?.journal?.map { it.refType }?.distinct()?.sorted() ?: emptyList(),
        )
    }

    private suspend fun updateJournals() {
        val loaded = walletRepository.state.value.loadedState
        val data = withContext(Dispatchers.Default) {
            var oldest = Instant.now()
            loaded?.map { loaded ->
                coroutineScope {
                    loaded.journal.minOfOrNull { it.date }?.also { if (oldest == null || it < oldest) oldest = it }
                    _state.update { it.copy(availableTimestamps = getAvailableTimespans(oldest)) }
                    if (_state.value.filters.timeSpan == Duration.ZERO) {
                        _state.update { it.copy(filters = it.filters.copy(timeSpan = it.availableTimestamps.lastOrNull() ?: Duration.ZERO)) }
                    }
                    val journals = filterJournalsByAge(filterJournalsByWallet(loaded.journal))
                        .sortedByDescending { it.date }
                    val filteredJournals = async { filterJournalsByTypeDirectionPartySearch(journals) }
                    val statistics = async { getStatistics(journals) }
                    LoadedData(
                        filteredJournal = filteredJournals.await(),
                        balances = loaded.balances,
                        loyaltyPointBalances = loaded.loyaltyPoints,
                        statistics = statistics.await(),
                        characters = loaded.characters,
                        corporations = loaded.corporations,
                    )
                }
            }
        }
        _state.update { it.copy(loadedData = data) }
    }

    private suspend fun getStatistics(journal: List<WalletJournalItem>): Statistics {
        val incomes = mutableListOf<WalletJournalItem>()
        val expenses = mutableListOf<WalletJournalItem>()
        val parties = mutableSetOf<TypeDetail>()
        val journalByParty = mutableMapOf<TypeDetail, MutableList<WalletJournalItem>>()
        var incomeSum = 0.0
        var expenseSum = 0.0
        var balance = 0.0

        journal.forEach { item ->
            if (item.amount > 0) {
                incomes.add(item)
                incomeSum += item.amount
            } else if (item.amount < 0) {
                expenses.add(item)
                expenseSum += item.amount
            }
            balance += item.amount
            item.firstParty?.let {
                parties += it
                journalByParty.getOrPut(it) { mutableListOf() }.add(item)
            }
            item.secondParty?.let {
                parties += it
                journalByParty.getOrPut(it) { mutableListOf() }.add(item)
            }
        }

        return coroutineScope {
            val incomeByParty = async { getPartyTransactions(journalByParty, parties, TransferDirection.Income) }
            val expensesByParty = async { getPartyTransactions(journalByParty, parties, TransferDirection.Expense) }
            val balanceByParty = async { getPartyTransactions(journalByParty, parties, null) }
            val destroyedRatsByParty = async { getDestroyedRatsByParty(incomes) }
            val dailyGoals = async { getDailyGoals(incomes) }
            val activity = async { getActivity(journalByParty, parties) }
            val incomeGroupSegments = async { getGroupSegments(incomes) }
            val expensesGroupSegments = async { getGroupSegments(expenses) }
            val incomeSegments = async { getSegments(incomes) }
            val expensesSegments = async { getSegments(expenses) }

            Statistics(
                journal = journal,
                income = incomeSum,
                expenses = expenseSum,
                balance = balance,
                incomeGroupSegments = incomeGroupSegments.await(),
                expensesGroupSegments = expensesGroupSegments.await(),
                incomeSegments = incomeSegments.await(),
                expensesSegments = expensesSegments.await(),
                incomeByParty = incomeByParty.await(),
                expensesByParty = expensesByParty.await(),
                balanceByParty = balanceByParty.await(),
                destroyedRatsByParty = destroyedRatsByParty.await(),
                dailyGoals = dailyGoals.await(),
                activity = activity.await(),
            )
        }
    }

    private fun getPartyTransactions(
        journalByParty: Map<TypeDetail, List<WalletJournalItem>>,
        parties: Set<TypeDetail>,
        transferDirection: TransferDirection?,
    ): List<PartyTransactions> {
        return parties
            .map { party ->
                val partyItems = journalByParty[party] ?: emptyList()
                val items = partyItems
                    .filter {
                        when (transferDirection) {
                            TransferDirection.Income -> it.amount > 0
                            TransferDirection.Expense -> it.amount < 0
                            else -> true
                        }
                    }
                    .filter {
                        when (it.wallet) {
                            is Wallet.Character -> {
                                if (it.wallet.characterId == (party as? TypeDetail.Character)?.character?.characterId) {
                                    // When collecting transactions involving a character, ignore transactions in that character's wallet, since they are redundant
                                    return@filter false
                                }
                            }
                            is Wallet.Corporation -> {
                                if (it.wallet.corporationId == (party as? TypeDetail.Corporation)?.corporation?.corporationId) {
                                    // When collecting transactions involving a corporation, ignore transactions in that corporation's wallet, since they are redundant
                                    return@filter false
                                }
                            }
                        }
                        return@filter true
                    }
                val total = items.sumOf { it.amount }
                val refTypes = items
                    .groupingBy { it.refType }
                    .fold(0.0) { acc, item -> acc + item.amount }
                    .map { it.key to it.value }
                    .sortedByDescending { it.second.absoluteValue }
                PartyTransactions(party, transferDirection, total, refTypes)
            }
            .filter { it.total.absoluteValue > 0.0 }
            .sortedByDescending { it.total.absoluteValue }
    }

    private fun getDestroyedRatsByParty(incomes: List<WalletJournalItem>): List<PartyKilledRats> {
        return incomes
            .filter { it.refType == "bounty_prizes" }
            .filter { it.secondParty != null }
            .groupBy { it.secondParty!! }
            .map { (character, items) ->
                val destroyedRats = items
                    .flatMap { it.reasonTypeDetails.filterIsInstance<TypeDetail.Type>() }
                    .groupingBy { it.type.id }
                    .reduce { _, acc, element -> acc.copy(count = (acc.count ?: 0) + (element.count ?: 0)) }
                    .values
                    .sortedByDescending { it.count }
                val countBySystem = items
                    .filter { it.context != null }
                    .groupingBy { it.context!! }
                    .fold(0L) { acc, element ->
                        acc + element.reasonTypeDetails.filterIsInstance<TypeDetail.Type>().sumOf { it.count ?: 0 }
                    }
                    .map { it.key to it.value }
                    .sortedByDescending { it.second }

                PartyKilledRats(
                    party = character,
                    totalBounty = items.sumOf { it.amount },
                    totalRats = destroyedRats.sumOf { it.count ?: 0 },
                    destroyedRats = destroyedRats,
                    countBySystem = countBySystem,
                )
            }
            .sortedByDescending { it.totalBounty }
    }

    private fun getDailyGoals(incomes: List<WalletJournalItem>): List<PartyDailyGoals> {
        return incomes
            .filter { it.refType == "daily_goal_payouts" }
            .filter { it.secondParty != null }
            .groupBy { it.secondParty!! }
            .map { (character, items) ->
                val dailyGoals = items
                    .mapNotNull { item ->
                        val date = item.date.toEveLocalDate()
                        val dailyGoal = item.reasonTypeDetails.filterIsInstance<TypeDetail.DailyGoal>().singleOrNull() ?: return@mapNotNull null
                        date to dailyGoal
                    }
                    .groupBy({ it.first }, { it.second })
                    .toList()
                    .sortedByDescending { it.first }
                PartyDailyGoals(
                    party = character,
                    totalReward = items.sumOf { it.amount },
                    totalGoals = items.size,
                    dailyGoals = dailyGoals,
                )
            }
            .sortedByDescending { it.dailyGoals.size }
    }

    private fun getActivity(
        journalByParty: Map<TypeDetail, List<WalletJournalItem>>,
        parties: Set<TypeDetail>,
    ): List<PartyActivity> {
        return parties
            .map { party ->
                val items = journalByParty[party] ?: emptyList()
                val total = items.sumOf { it.amount }
                val transactions = items
                    .groupBy { it.date.toEveLocalDate() }
                    .map { (date, items) ->
                        date to items
                    }
                    .sortedByDescending { it.first }
                PartyActivity(party, total, transactions)
            }
            .sortedByDescending { it.totalBalance.absoluteValue }
    }

    private fun getGroupSegments(items: List<WalletJournalItem>): List<Segment> {
        val total = items.sumOf { it.amount.absoluteValue }
        return items
            .groupingBy { TransactionGroup.byReferenceType(it.refType) }
            .fold(0.0) { acc, item -> acc + item.amount }
            .map { (key, value) ->
                val ratio = (value.absoluteValue / total)
                Segment(key, key.name, key.color, value, ratio)
            }
    }

    private fun getSegments(items: List<WalletJournalItem>): Map<TransactionGroup, List<Segment>> {
        return items
            .groupBy { TransactionGroup.byReferenceType(it.refType) }
            .entries
            .associate { (group, items) ->
                val total = items.sumOf { it.amount.absoluteValue }
                val (itemsWithTransactions, itemsWithoutTransactions) = items.partition { it.transaction != null }
                val segmentsWithTransactions = itemsWithTransactions
                    .groupingBy({
                        val transaction = it.transaction ?: return@groupingBy "No transaction"
                        val categoryId = (transaction.type as? TypeDetail.Type)?.type?.categoryId
                        val categoryName = categoryId?.let { id -> typesRepository.getCategoryName(id) } ?: return@groupingBy "No category"
                        if (transaction.isBuy) {
                            "Purchase: $categoryName"
                        } else {
                            "Sale: $categoryName"
                        }
                    })
                    .fold(0.0) { acc, item -> acc + item.amount }
                    .map { (key, value) ->
                        key to value
                    }
                val segmentsWithoutTransactions = itemsWithoutTransactions
                    .groupingBy { it.refType }
                    .fold(0.0) { acc, item -> acc + item.amount }
                    .map { (key, value) ->
                        getReferenceTypeName(key) to value
                    }
                group to (segmentsWithTransactions + segmentsWithoutTransactions).map { (name, value) ->
                    val ratio = (value.absoluteValue / total)
                    val brightnessModifier = (0.3 + 0.7 * ratio).toFloat()
                    val hsb = group.color.toHsb()
                    val color = hsb.copy(brightness = hsb.brightness * brightnessModifier).toColor()
                    Segment(null, name, color, value, ratio)
                }
            }
    }

    private fun filterJournalsByAge(items: List<WalletJournalItem>): List<WalletJournalItem> {
        val filters = _state.value.filters
        val minAge = Instant.now() - filters.timeSpan
        return items.filter {
            it.date >= minAge
        }
    }

    private fun filterJournalsByWallet(items: List<WalletJournalItem>): List<WalletJournalItem> {
        val filters = _state.value.filters
        return items
            .filter {
                if (filters.walletTypes.isEmpty()) return@filter true
                filters.walletTypes.any { walletType ->
                    when (walletType) {
                        WalletType.Character -> it.wallet is Wallet.Character
                        is WalletType.SpecificCharacter -> it.wallet is Wallet.Character && it.wallet.characterId == walletType.characterId
                        is WalletType.SpecificCorporation -> it.wallet is Wallet.Corporation && it.wallet.corporationId == walletType.corporationId
                        is WalletType.SpecificCorporationDivision -> it.wallet is Wallet.Corporation && it.wallet.corporationId == walletType.corporationId && it.wallet.divisionId == walletType.divisionId
                    }
                }
            }
    }

    private fun filterJournalsByTypeDirectionPartySearch(items: List<WalletJournalItem>): List<WalletJournalItem> {
        val filters = _state.value.filters
        return items
            .filter {
                if (filters.referenceTypes.isEmpty()) return@filter true
                filters.referenceTypes.any { type ->
                    it.refType == type
                }
            }
            .filter {
                when (filters.direction) {
                    TransferDirection.Income -> it.amount > 0
                    TransferDirection.Expense -> it.amount <= 0
                    null -> true
                }
            }
            .filter {
                filters.party == null || it.firstParty == filters.party || it.secondParty == filters.party
            }
            .filter {
                val search = filters.search?.lowercase() ?: return@filter true
                it.isMatching(search)
            }
    }

    private fun WalletJournalItem.isMatching(search: String): Boolean {
        return context.isMatching(search) ||
            search in description.lowercase() ||
            reason != null && search in reason.lowercase() ||
            reasonTypeDetails.any { it.isMatching(search) } ||
            search in refType.lowercase() ||
            search in getReferenceTypeName(refType).lowercase() ||
            firstParty.isMatching(search) ||
            secondParty.isMatching(search) ||
            taxReceiver.isMatching(search) ||
            transaction?.client.isMatching(search) ||
            transaction?.location.isMatching(search) ||
            transaction?.type.isMatching(search)
    }

    private fun getAvailableTimespans(journalHistoryFrom: Instant): List<Duration> {
        val maxTimespanDays = Duration.between(journalHistoryFrom, Instant.now()).toDays().toInt() + 1
        val maxTimespan = Duration.ofDays(maxTimespanDays.toLong())
        return sequence {
            yield(Duration.ofHours(2))
            yield(Duration.ofHours(12))
            yield(Duration.ofHours(24))
            yield(Duration.ofDays(2))
            yield(Duration.ofDays(7))
            yield(Duration.ofDays(14))
            repeat(100) {
                yield(Duration.ofDays(30L * (it + 1)))
            }
        }.takeWhile { it < maxTimespan }.toList() + maxTimespan
    }
}
