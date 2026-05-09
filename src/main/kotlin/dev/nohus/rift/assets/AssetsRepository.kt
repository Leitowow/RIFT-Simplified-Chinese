package dev.nohus.rift.assets

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.Asset
import dev.nohus.rift.network.esi.models.AssetLocationType
import dev.nohus.rift.network.esi.models.AssetName
import dev.nohus.rift.network.esi.models.UniverseStationsId
import dev.nohus.rift.network.esi.models.UniverseStructuresId
import dev.nohus.rift.network.esi.pagination.fetchPagePaginated
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Single
class AssetsRepository(
    private val localCharactersRepository: LocalCharactersRepository,
    private val typesRepository: TypesRepository,
    private val planetaryIndustryCommoditiesRepository: PlanetaryIndustryCommoditiesRepository,
    private val isNameableAssetUseCase: IsNameableAssetUseCase,
    private val esiApi: EsiApi,
) {

    data class State(
        val loadedState: Result<LoadedState>? = null,
        val isLoading: Boolean = true,
    )

    data class LoadedState(
        val assets: List<AssetWithLocation>,
        val balances: List<AssetBalance>,
        val owners: List<AssetOwner>,
        val divisionNames: Map<Int, Map<Int, String>>,
    )

    data class AssetBalance(
        val owner: AssetOwner,
        val count: Int,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val reloadRequest = MutableStateFlow(false)
    private val loadingMutex = Mutex()
    private var isRealtime = false

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                delay(1.minutes)
                if (isRealtime) {
                    reloadRequest.value = false
                    yield()
                    reloadRequest.value = true
                }
            }
        }
        launch {
            while (true) {
                delay(15.minutes)
                reloadRequest.value = true
            }
        }
        launch {
            localCharactersRepository.characters.debounce(500).collect {
                reloadRequest.value = true
            }
        }
        launch {
            reloadRequest.filter { it }.collect {
                reloadRequest.value = false
                load()
            }
        }
    }

    enum class LocationType {
        Container,
        AssetSafety,
        System,
        AbyssalSystem,
        Station,
        Structure,
        Other,
    }

    data class AssetWithOwner(
        val asset: Asset,
        val name: String?,
        val type: Type,
        val owner: AssetOwner,
    )

    data class AssetWithLocation(
        val asset: Asset,
        val name: String?,
        val type: Type,
        val owner: AssetOwner,
        val location: AssetLocation,
    )

    sealed class AssetOwner(open val character: LocalCharacter) {
        data class Character(override val character: LocalCharacter) : AssetOwner(character)
        data class Corporation(val corporationId: Int, val corporationName: String, override val character: LocalCharacter) : AssetOwner(character)
    }

    sealed class AssetLocation(open val locationId: Long) {
        data class Station(
            override val locationId: Long,
            val typeId: Int,
            val name: String,
            val systemId: Int,
        ) : AssetLocation(locationId)
        data class Structure(
            override val locationId: Long,
            val typeId: Int?,
            val name: String,
            val systemId: Int,
        ) : AssetLocation(locationId)
        data class System(
            override val locationId: Long,
            val systemId: Int,
        ) : AssetLocation(locationId)
        data class AssetSafety(
            override val locationId: Long,
        ) : AssetLocation(locationId)
        data class CustomsOffice(
            override val locationId: Long,
        ) : AssetLocation(locationId)
        data class Unknown(
            override val locationId: Long,
        ) : AssetLocation(locationId)
    }

    data class ResolvedAssetLocations(
        val stationsById: Map<Long, UniverseStationsId>,
        val structuresById: Map<Long, UniverseStructuresId>,
        val unresolveableIds: List<Long>,
    )

    fun reload() {
        if (!loadingMutex.isLocked) reloadRequest.value = true
    }

    fun setNeedsRealtimeUpdates(isRealtime: Boolean) {
        this.isRealtime = isRealtime
    }

    private suspend fun load() = withContext(Dispatchers.Default) {
        logger.debug { "Loading assets requested" }
        loadingMutex.withLock {
            logger.debug { "Loading assets" }
            val charactersWithAssetsScopes = localCharactersRepository.characters.value
                .filter { ScopeGroups.readAssets in it.scopes }
            val charactersWithCorpAssetsScopes = localCharactersRepository.characters.value
                .filter { ScopeGroups.readCorporationAssets in it.scopes }
            val characters = charactersWithAssetsScopes.map {
                AssetOwner.Character(it)
            }

            if (characters.isEmpty() && charactersWithCorpAssetsScopes.isEmpty()) return@withContext
            _state.update { it.copy(isLoading = true) }

            val corporations = charactersWithCorpAssetsScopes.mapNotNull { character ->
                val roles = character.info?.corporationRoles ?: return@mapNotNull null
                if ("Director" in roles) {
                    AssetOwner.Corporation(character.info.corporationId, character.info.corporationName, character)
                } else {
                    null
                }
            }.groupBy { it.corporationId }.map { (_, corporations) -> corporations.first() }
            val assetOwners = charactersWithAssetsScopes.map {
                AssetOwner.Character(it)
            } + corporations

            val divisionNames = async {
                corporations.mapNotNull { corporation ->
                    esiApi.getCorporationsCorporationIdDivisions(Originator.Assets, corporation.character.characterId, corporation.corporationId).map { divisions ->
                        val names = divisions.hangarDivisions?.mapNotNull {
                            (it.id?.toInt() ?: return@mapNotNull null) to (it.name ?: return@mapNotNull null)
                        }?.toMap() ?: emptyMap()
                        corporation.corporationId to names
                    }.success
                }.toMap()
            }

            val loadedState = loadAllAssetsWithLocations(characters, corporations).map {
                logger.debug { "Assets loaded: ${it.size}" }
                val assetBalances = getAssetBalances(it)
                LoadedState(it, assetBalances, assetOwners, divisionNames.await())
            }.onFailure {
                logger.error { "Could not load assets: ${it?.cause?.message}" }
            }
            _state.update { it.copy(loadedState = loadedState, isLoading = false) }
        }
    }

    private fun getAssetBalances(assets: List<AssetWithLocation>): List<AssetBalance> {
        return assets.groupBy { it.owner }.map { (owner, assets) ->
            AssetBalance(owner, assets.size)
        }
    }

    private suspend fun loadAllAssetsWithLocations(
        characters: List<AssetOwner.Character>,
        corporations: List<AssetOwner.Corporation>,
    ): Result<List<AssetWithLocation>> = withContext(Dispatchers.IO) {
        if (characters.isEmpty()) return@withContext Result.Success(emptyList())
        val allAssets = when (val result = loadAllAssets(characters, corporations)) {
            is Result.Success -> result.data
            is Result.Failure -> return@withContext result
        }
        val itemIds = allAssets.map { it.asset.itemId }.distinct()
        val resolvedAssetLocations = when (val result = resolveAssetLocations(allAssets, itemIds)) {
            is Result.Success -> result.data
            is Result.Failure -> return@withContext result
        }
        val allAssetsWithLocation = allAssets.map { assetWithCharacter ->
            val location = getAssetLocation(assetWithCharacter, itemIds, allAssets, resolvedAssetLocations.stationsById, resolvedAssetLocations.structuresById)
            AssetWithLocation(
                asset = assetWithCharacter.asset,
                name = assetWithCharacter.name,
                type = assetWithCharacter.type,
                owner = assetWithCharacter.owner,
                location = location,
            )
        }
        Result.Success(allAssetsWithLocation)
    }

    private suspend fun resolveAssetLocations(
        allAssets: List<AssetWithOwner>,
        itemIds: List<Long>,
    ): Result<ResolvedAssetLocations> = coroutineScope {
        val stationIds = mutableListOf<Long>()
        val structureIds = mutableListOf<Long>()
        allAssets
            .groupBy { getLocationType(it.asset.locationId, it.asset.locationType, itemIds) }
            .forEach { (locationType, items) ->
                val locationIds = items.map { it.asset.locationId }.distinct()
                when (locationType) {
                    LocationType.Station -> stationIds += locationIds
                    LocationType.Structure -> structureIds += locationIds
                    else -> {}
                }
            }
        val stationsByIdDeferred = stationIds.map { stationId ->
            async { stationId to esiApi.getUniverseStationsId(Originator.Assets, stationId.toInt()) }
        }
        val structuresByIdDeferred = structureIds.map { structureId ->
            async {
                val characterId = allAssets.first { it.asset.locationId == structureId }.owner.character.characterId
                structureId to esiApi.getUniverseStructuresId(Originator.Assets, structureId, characterId)
            }
        }
        val stationsById = stationsByIdDeferred.awaitAll().associate { (id, result) ->
            when (result) {
                is Result.Success -> id to result.data
                is Result.Failure -> return@coroutineScope result
            }
        }
        val (structuresById, unresolveableIds) = structuresByIdDeferred.awaitAll().map { (id, result) ->
            when (result) {
                is Result.Success -> id to result.data
                is Result.Failure -> run {
                    structureIds -= id
                    id to null
                }
            }
        }
            .partition { it.first in structureIds }
            .let { (structures, unresolveables) ->
                structures.associate { it.first to it.second!! } to unresolveables.map { it.first }
            }
        Result.Success(
            ResolvedAssetLocations(
                stationsById = stationsById,
                structuresById = structuresById,
                unresolveableIds = unresolveableIds,
            ),
        )
    }

    private suspend fun loadAllAssets(characters: List<AssetOwner.Character>, corporations: List<AssetOwner.Corporation>): Result<List<AssetWithOwner>> = coroutineScope {
        val characterAssetsDeferreds = characters.map { character ->
            async {
                fetchPagePaginated {
                    esiApi.getCharactersIdAssets(Originator.Assets, it, character.character.characterId)
                }.map { assets ->
                    typesRepository.resolveNamesFromEsi(Originator.Assets, assets.map { it.typeId })
                    val names = getAssetNames(assets, character.character)
                    assets.map { asset ->
                        AssetWithOwner(
                            asset = asset,
                            name = names[asset.itemId],
                            type = typesRepository.getTypeOrPlaceholder(asset.typeId),
                            owner = character,
                        )
                    }
                }
            }
        }
        val corporationAssetsDeferreds = corporations.map { corporation ->
            async {
                fetchPagePaginated {
                    esiApi.getCorporationsIdAssets(Originator.Assets, it, corporation.character.characterId, corporation.corporationId)
                }.map { assets ->
                    typesRepository.resolveNamesFromEsi(Originator.Assets, assets.map { it.typeId })
                    val names = getAssetNames(assets, corporation)
                    assets.map { asset ->
                        AssetWithOwner(
                            asset = asset,
                            name = names[asset.itemId],
                            type = typesRepository.getTypeOrPlaceholder(asset.typeId),
                            owner = corporation,
                        )
                    }
                }
            }
        }
        val assets = (characterAssetsDeferreds + corporationAssetsDeferreds).awaitAll().flatMap { result ->
            when (result) {
                is Result.Success -> result.data
                is Result.Failure -> return@coroutineScope result
            }
        }
        Result.Success(assets)
    }

    private suspend fun getAssetNames(
        assets: List<Asset>,
        character: LocalCharacter,
    ): Map<Long, String> {
        return getAssetNames(assets) { itemIds ->
            esiApi.getCharactersIdAssetsNames(Originator.Assets, character.characterId, itemIds)
        }
    }

    private suspend fun getAssetNames(
        assets: List<Asset>,
        corporation: AssetOwner.Corporation,
    ): Map<Long, String> {
        return getAssetNames(assets) { itemIds ->
            esiApi.getCorporationsIdAssetsNames(Originator.Assets, corporation.character.characterId, corporation.corporationId, itemIds)
        }
    }

    private suspend fun getAssetNames(
        assets: List<Asset>,
        request: suspend (itemIds: List<Long>) -> Result<List<AssetName>>,
    ): Map<Long, String> {
        return assets
            .filter { isNameableAssetUseCase(it) }
            .map { it.itemId }
            .distinct()
            .chunked(1000)
            .flatMap { itemIds ->
                when (val result = request(itemIds)) {
                    is Result.Success -> result.data
                    is Result.Failure -> {
                        logger.error { "Failed loading asset names ($itemIds): ${result.cause}" }
                        emptyList()
                    }
                }
            }
            .mapNotNull { it.itemId to (it.name.takeIf { it != "None" } ?: return@mapNotNull null) }
            .toMap()
    }

    private fun getAssetLocation(
        assetWithOwner: AssetWithOwner,
        itemIds: List<Long>,
        allAssets: List<AssetWithOwner>,
        stationsById: Map<Long, UniverseStationsId>,
        structuresById: Map<Long, UniverseStructuresId>,
    ): AssetLocation {
        val locationId = assetWithOwner.asset.locationId
        val locationType = getLocationType(locationId, assetWithOwner.asset.locationType, itemIds)
        val assetsByItemId = allAssets.associateBy { it.asset.itemId }
        return when (locationType) {
            LocationType.Container -> {
                val container = assetsByItemId[locationId]
                if (container != null) {
                    getAssetLocation(container, itemIds, allAssets, stationsById, structuresById)
                } else {
                    AssetLocation.Unknown(locationId)
                }
            }
            LocationType.AssetSafety -> AssetLocation.AssetSafety(locationId)
            LocationType.System -> AssetLocation.System(locationId, locationId.toInt())
            LocationType.AbyssalSystem -> AssetLocation.System(locationId, locationId.toInt())
            LocationType.Station -> {
                val station = stationsById[locationId]!!
                AssetLocation.Station(locationId, station.typeId, station.name, station.systemId)
            }
            LocationType.Structure -> {
                val structure = structuresById[locationId]
                if (structure != null) {
                    AssetLocation.Structure(locationId, structure.typeId, structure.name, structure.solarSystemId)
                } else {
                    val assetsInLocation = allAssets.filter { it.asset.locationId == locationId }.map { it.asset.typeId }
                    if (planetaryIndustryCommoditiesRepository.isPlanetaryIndustryItems(assetsInLocation)) {
                        AssetLocation.CustomsOffice(locationId)
                    } else {
                        AssetLocation.Unknown(locationId)
                    }
                }
            }
            LocationType.Other -> AssetLocation.Unknown(locationId)
        }
    }

    private fun getLocationType(locationId: Long, locationType: AssetLocationType, itemIds: List<Long>): LocationType {
        return when {
            locationId == 2004L -> LocationType.AssetSafety
            locationType == AssetLocationType.SolarSystem -> when (locationId) {
                in 30000000L..32000000L -> return LocationType.System
                in 32000000L..33000000L -> return LocationType.AbyssalSystem
                else -> LocationType.System
            }
            locationType == AssetLocationType.Station -> LocationType.Station
            locationType == AssetLocationType.Item -> when {
                locationId in itemIds -> return LocationType.Container
                else -> LocationType.Structure
            }
            else -> LocationType.Other
        }
    }
}
