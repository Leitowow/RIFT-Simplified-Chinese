package dev.nohus.rift.location

import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import org.koin.core.annotation.Single

@Single
class LocationRepository(
    private val esiApi: EsiApi,
    private val characterDetailsRepository: CharacterDetailsRepository,
) {

    data class Station(
        val stationId: Int,
        val name: String,
        val owner: CharacterDetailsRepository.CorporationDetails?,
        val typeId: Int,
        val solarSystemId: Int,
    )

    data class Structure(
        val structureId: Long,
        val name: String,
        val owner: CharacterDetailsRepository.CorporationDetails?,
        val typeId: Int?,
        val solarSystemId: Int,
    )

    suspend fun getStation(originator: Originator, stationId: Int?, fetchOwner: Boolean = false): Station? {
        if (stationId == null) return null
        return esiApi.getUniverseStationsId(originator, stationId).map {
            val owner = it.ownerId?.takeIf { fetchOwner }?.let { ownerId -> characterDetailsRepository.getCorporationDetails(originator, ownerId) }
            Station(stationId, it.name, owner, it.typeId, it.systemId)
        }.success
    }

    suspend fun getStructure(originator: Originator, structureId: Long?, characterId: Int, fetchOwner: Boolean = false): Structure? {
        if (structureId == null) return null
        return esiApi.getUniverseStructuresId(originator, structureId, characterId).map {
            val owner = it.ownerId.takeIf { fetchOwner }?.let { ownerId -> characterDetailsRepository.getCorporationDetails(originator, ownerId) }
            Structure(structureId, it.name, owner, it.typeId, it.solarSystemId)
        }.success
    }
}
