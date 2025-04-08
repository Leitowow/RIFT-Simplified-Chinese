package dev.nohus.rift.contacts

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.UniverseStructuresId
import dev.nohus.rift.repositories.NamesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.StationsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails
import dev.nohus.rift.standings.StandingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single
class SearchRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val namesRepository: NamesRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val stationsRepository: StationsRepository,
    private val typesRepository: TypesRepository,
    private val standingsRepository: StandingsRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
) {

    enum class SearchCategory(val displayName: String, val queryName: String) {
        Agents("Agents", "agent"),
        Alliance("Alliances", "alliance"),
        Characters("Characters", "character"),
        Constellation("Constellations", "constellation"),
        Corporations("Corporations", "corporation"),
        Factions("Factions", "faction"),
        InventoryTypes("Items", "inventory_type"),
        Regions("Regions", "region"),
        SolarSystems("Solar Systems", "solar_system"),
        Stations("Stations", "station"),
        Structures("Structures", "structure"),
    }

    data class SearchResult(
        val id: Long,
        val typeId: Int,
        val systemId: Int? = null,
        val name: String,
        val description: String? = null,
        val standing: Float? = null,
        val characterDetails: CharacterDetails? = null,
    )

    suspend fun search(
        characterId: Int,
        categories: List<SearchCategory>,
        search: String,
    ): Result<Map<SearchCategory, List<SearchResult>>> {
        return esiApi.getCharactersIdSearch(
            characterId = characterId,
            categories = categories.map { it.queryName },
            strict = false,
            search = search,
        ).map { response ->
            coroutineScope {
                val ids = response.agent + response.alliance + response.character + response.constellation +
                    response.corporation + response.faction + response.inventoryType + response.region +
                    response.solarSystem + response.station + response.station
                namesRepository.resolveNames(ids.map { it.toInt() })
                buildMap {
                    put(SearchCategory.Agents, response.agent.mapResults())
                    put(SearchCategory.Alliance, response.alliance.mapAlliancesResults())
                    if (response.character.isNotEmpty()) {
                        val characters = response.character.map { characterId ->
                            async { characterDetailsRepository.getCharacterDetails(characterId.toInt()) }
                        }.awaitAll().filterNotNull().associateBy { it.characterId }
                        put(SearchCategory.Characters, response.character.mapCharactersResults(characters))
                    }
                    put(SearchCategory.Constellation, response.constellation.mapResults())
                    put(SearchCategory.Corporations, response.corporation.mapCorporationsResults())
                    put(SearchCategory.Factions, response.faction.mapResults())
                    put(SearchCategory.InventoryTypes, response.inventoryType.mapResults())
                    put(SearchCategory.Regions, response.region.mapResults())
                    put(SearchCategory.SolarSystems, response.solarSystem.mapSolarSystemsResults())
                    put(SearchCategory.Stations, response.station.mapStationsResults())
                    if (response.structure.isNotEmpty()) {
                        val structures = response.structure.map { structureId ->
                            async { structureId to esiApi.getUniverseStructuresId(structureId, characterId) }
                        }.awaitAll().mapNotNull { it.first to (it.second.success ?: return@mapNotNull null) }.toMap()
                        put(SearchCategory.Structures, response.structure.mapStructuresResults(structures))
                    }
                }
            }
        }
    }

    private fun List<Long>.mapResults(): List<SearchResult> {
        return map { id ->
            SearchResult(
                id = id,
                typeId = id.toInt(),
                name = namesRepository.getName(id.toInt()) ?: "$id",
            )
        }.sortedBy { it.name }
    }

    private fun List<Long>.mapCharactersResults(characters: Map<Int, CharacterDetails>): List<SearchResult> {
        return map { id ->
            val details = characters[id.toInt()]
            SearchResult(
                id = id,
                typeId = id.toInt(),
                name = namesRepository.getName(id.toInt()) ?: "$id",
                standing = details?.standing,
                characterDetails = details,
            )
        }.sortedBy { it.name }
    }

    private fun List<Long>.mapCorporationsResults(): List<SearchResult> {
        return map { id ->
            SearchResult(
                id = id,
                typeId = id.toInt(),
                name = namesRepository.getName(id.toInt()) ?: "$id",
                standing = standingsRepository.getStanding(
                    allianceId = null,
                    corporationId = id.toInt(),
                    characterId = null,
                ),
            )
        }
    }

    private fun List<Long>.mapAlliancesResults(): List<SearchResult> {
        return map { id ->
            SearchResult(
                id = id,
                typeId = id.toInt(),
                name = namesRepository.getName(id.toInt()) ?: "$id",
                standing = standingsRepository.getStanding(
                    allianceId = id.toInt(),
                    corporationId = null,
                    characterId = null,
                ),
            )
        }
    }

    private fun List<Long>.mapSolarSystemsResults(): List<SearchResult> {
        return map { id ->
            val name = solarSystemsRepository.getSystem(id.toInt())?.name
            val sunTypeId = if (name != null) {
                solarSystemsRepository.getSystemSunTypeId(name)
            } else {
                0
            }
            SearchResult(
                id = id,
                typeId = sunTypeId,
                name = namesRepository.getName(id.toInt()) ?: name ?: "$id",
            )
        }.sortedBy { it.name }
    }

    private fun List<Long>.mapStationsResults(): List<SearchResult> {
        return map { id ->
            val station = stationsRepository.getStation(id.toInt())
            SearchResult(
                id = id,
                typeId = station?.typeId ?: 0,
                systemId = station?.systemId,
                name = namesRepository.getName(id.toInt()) ?: station?.name ?: "$id",
                description = station?.typeId?.let { typesRepository.getTypeName(it) },
            )
        }.sortedBy { it.name }
    }

    private fun List<Long>.mapStructuresResults(structures: Map<Long, UniverseStructuresId>): List<SearchResult> {
        return map { id ->
            val structure = structures[id]
            SearchResult(
                id = id,
                typeId = structure?.typeId ?: 0,
                systemId = structure?.solarSystemId,
                name = structure?.name ?: "$id",
                description = structure?.typeId?.let { typesRepository.getTypeName(it) },
            )
        }.sortedBy { it.name }
    }
}
