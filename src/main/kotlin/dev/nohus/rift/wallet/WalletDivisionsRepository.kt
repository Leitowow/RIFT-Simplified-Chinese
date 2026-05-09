package dev.nohus.rift.wallet

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class WalletDivisionsRepository(
    private val esiApi: EsiApi,
    private val settings: Settings,
) {

    suspend fun load(directors: List<LocalCharactersRepository.LocalCharacter>) {
        val newCorporationIdToDivisions = directors.mapNotNull { director ->
            val corporationId = director.info?.corporationId ?: return@mapNotNull null
            val divisions = esiApi.getCorporationsCorporationIdDivisions(Originator.Wallets, director.characterId, corporationId).success?.walletDivisions
            val divisionNames = divisions?.mapNotNull {
                val id = it.id?.toInt() ?: return@mapNotNull null
                val name = it.name ?: return@mapNotNull null
                id to name
            }?.toMap() ?: return@mapNotNull null
            corporationId to divisionNames
        }.toMap()
        val currentCorporationIdToDivisions = settings.corpWalletDivisionNames

        val corporationIds = currentCorporationIdToDivisions.keys + newCorporationIdToDivisions.keys
        val updatedCorporationIdToDivisions = corporationIds.associateWith { corporationId ->
            val currentNames = currentCorporationIdToDivisions[corporationId] ?: emptyMap()
            val newNames = newCorporationIdToDivisions[corporationId] ?: emptyMap()
            (1..7).mapNotNull { divisionId ->
                divisionId to (newNames[divisionId] ?: currentNames[divisionId] ?: return@mapNotNull null)
            }.toMap()
        }

        settings.corpWalletDivisionNames = updatedCorporationIdToDivisions
    }

    fun getDivisionNameOrDefault(corporationId: Int, divisionId: Int): String {
        return getDivisionName(corporationId, divisionId) ?: "Division $divisionId"
    }

    fun getDivisionName(corporationId: Int, divisionId: Int): String? {
        val divisionNames = settings.corpWalletDivisionNames[corporationId] ?: emptyMap()
        return divisionNames[divisionId]
    }

    fun setDivisionName(corporationId: Int, divisionId: Int, name: String) {
        val corporations = settings.corpWalletDivisionNames.toMutableMap()
        val divisions = corporations[corporationId]?.toMutableMap() ?: mutableMapOf()
        if (name.isNotBlank()) {
            divisions[divisionId] = name
        } else {
            divisions -= divisionId
        }
        corporations[corporationId] = divisions
        settings.corpWalletDivisionNames = corporations
    }
}
