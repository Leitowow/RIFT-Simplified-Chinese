package dev.nohus.rift.repositories.character

import dev.nohus.rift.contacts.ContactsRepository
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.AlliancesIdAlliance
import dev.nohus.rift.network.esi.models.CharactersAffiliation
import dev.nohus.rift.network.esi.models.CorporationsIdCorporation
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.StandingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single
class CharacterDetailsRepository(
    private val esiApi: EsiApi,
    private val standingsRepository: StandingsRepository,
    private val contactsRepository: ContactsRepository,
    private val characterAffiliationRepository: CharacterAffiliationRepository,
) {

    data class CharacterDetails(
        val characterId: Int,
        val name: String,
        val corporationId: Int,
        val corporationName: String?,
        val corporationTicker: String?,
        val allianceId: Int?,
        val allianceName: String?,
        val allianceTicker: String?,
        val standing: Float,
        val standingLevel: Standing,
        val title: String?,
        val characterLabels: List<String>,
        val corporationLabels: List<String>,
        val allianceLabels: List<String>,
    )

    data class CorporationDetails(
        val corporationId: Int,
        val corporationName: String,
        val corporationTicker: String,
        val allianceId: Int?,
        val allianceName: String?,
        val allianceTicker: String?,
        val standing: Float,
        val standingLevel: Standing,
        val corporationLabels: List<String>,
        val allianceLabels: List<String>,
    )

    data class AllianceDetails(
        val allianceId: Int,
        val allianceName: String,
        val allianceTicker: String,
        val standing: Float,
        val standingLevel: Standing,
        val allianceLabels: List<String>,
    )

    // TODO: Check if batch version can be applied anywhere else
    suspend fun getCharacterDetails(originator: Originator, characterIds: List<Int>): Map<Int, CharacterDetails?> = coroutineScope {
        val distinct = characterIds.distinct()
        val affiliations = characterAffiliationRepository.getCharacterAffiliations(originator, distinct)
        distinct.map {
            async {
                it to getCharacterDetails(originator, it, affiliations[it])
            }
        }.awaitAll().toMap()
    }

    suspend fun getCharacterDetails(originator: Originator, characterId: Int, affiliation: CharactersAffiliation? = null): CharacterDetails? = coroutineScope {
        val characterDeferred = async { esiApi.getCharactersId(originator, characterId).success }
        val affiliationDeferred = async {
            affiliation ?: characterAffiliationRepository.getCharacterAffiliation(originator, characterId)
        }
        val character = characterDeferred.await() ?: return@coroutineScope null
        val affiliation = affiliationDeferred.await()
        val corporationId = affiliation?.corporationId ?: character.corporationId
        val allianceId = affiliation?.allianceId ?: character.allianceId
        val deferredCorporation = async { esiApi.getCorporationsId(originator, corporationId).success }
        val deferredAlliance = async { allianceId?.let { esiApi.getAlliancesId(originator, it).success } }
        val corporation = deferredCorporation.await()
        val alliance = deferredAlliance.await()
        val standing = standingsRepository.getStanding(allianceId, corporationId, characterId) ?: 0f
        val standingLevel = standingsRepository.getStandingLevel(allianceId, corporationId, characterId)
        val characterLabels = contactsRepository.getLabels(listOf(characterId)).map { it.name }.distinct()
        val corporationLabels = contactsRepository.getLabels(listOf(corporationId)).map { it.name }.distinct()
        val allianceLabels = allianceId
            ?.let { contactsRepository.getLabels(listOf(allianceId)).map { it.name }.distinct() }
            ?: emptyList()
        CharacterDetails(
            characterId = characterId,
            name = character.name,
            corporationId = corporationId,
            corporationName = corporation?.name,
            corporationTicker = corporation?.ticker,
            allianceId = allianceId,
            allianceName = alliance?.name,
            allianceTicker = alliance?.ticker,
            standing = standing,
            standingLevel = standingLevel,
            title = character.title,
            characterLabels = characterLabels,
            corporationLabels = corporationLabels,
            allianceLabels = allianceLabels,
        )
    }

    suspend fun getCorporationDetails(originator: Originator, corporationId: Int): CorporationDetails? = coroutineScope {
        val corporation = esiApi.getCorporationsId(originator, corporationId).success ?: return@coroutineScope null
        val alliance = corporation.allianceId?.let { esiApi.getAlliancesId(originator, it).success }
        val standing = standingsRepository.getStanding(corporation.allianceId, corporationId, null) ?: 0f
        val standingLevel = standingsRepository.getStandingLevel(corporation.allianceId, corporationId, null)
        val corporationLabels = contactsRepository.getLabels(listOf(corporationId)).map { it.name }.distinct()
        val allianceLabels = corporation.allianceId
            ?.let { contactsRepository.getLabels(listOf(corporation.allianceId)).map { it.name }.distinct() }
            ?: emptyList()
        CorporationDetails(
            corporationId = corporationId,
            corporationName = corporation.name,
            corporationTicker = corporation.ticker,
            allianceId = corporation.allianceId,
            allianceName = alliance?.name,
            allianceTicker = alliance?.ticker,
            standing = standing,
            standingLevel = standingLevel,
            corporationLabels = corporationLabels,
            allianceLabels = allianceLabels,
        )
    }

    suspend fun getAllianceDetails(originator: Originator, allianceId: Int): AllianceDetails? = coroutineScope {
        val alliance = esiApi.getAlliancesId(originator, allianceId).success ?: return@coroutineScope null
        val standing = standingsRepository.getStanding(allianceId, null, null) ?: 0f
        val standingLevel = standingsRepository.getStandingLevel(allianceId, null, null)
        val allianceLabels = contactsRepository.getLabels(listOf(allianceId)).map { it.name }.distinct()
        AllianceDetails(
            allianceId = allianceId,
            allianceName = alliance.name,
            allianceTicker = alliance.ticker,
            standing = standing,
            standingLevel = standingLevel,
            allianceLabels = allianceLabels,
        )
    }

    suspend fun getCorporationName(originator: Originator, corporationId: Int): Result<CorporationsIdCorporation> {
        return esiApi.getCorporationsId(originator, corporationId)
    }

    suspend fun getAllianceName(originator: Originator, allianceId: Int): Result<AlliancesIdAlliance> {
        return esiApi.getAlliancesId(originator, allianceId)
    }
}
