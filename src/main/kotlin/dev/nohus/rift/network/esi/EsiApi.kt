package dev.nohus.rift.network.esi

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.models.AlliancesIdAlliance
import dev.nohus.rift.network.esi.models.Asset
import dev.nohus.rift.network.esi.models.AssetLocation
import dev.nohus.rift.network.esi.models.AssetName
import dev.nohus.rift.network.esi.models.CharacterIdLocation
import dev.nohus.rift.network.esi.models.CharacterIdOnline
import dev.nohus.rift.network.esi.models.CharacterIdShip
import dev.nohus.rift.network.esi.models.CharactersAffiliation
import dev.nohus.rift.network.esi.models.CharactersIdCharacter
import dev.nohus.rift.network.esi.models.CharactersIdClones
import dev.nohus.rift.network.esi.models.CharactersIdFleet
import dev.nohus.rift.network.esi.models.CharactersIdPlanet
import dev.nohus.rift.network.esi.models.CharactersIdPlanetsId
import dev.nohus.rift.network.esi.models.CharactersIdRoles
import dev.nohus.rift.network.esi.models.CharactersIdSearch
import dev.nohus.rift.network.esi.models.Contact
import dev.nohus.rift.network.esi.models.ContactsLabel
import dev.nohus.rift.network.esi.models.CorporationDivisions
import dev.nohus.rift.network.esi.models.CorporationProjectsQueryState
import dev.nohus.rift.network.esi.models.CorporationWalletBalance
import dev.nohus.rift.network.esi.models.CorporationsIdCorporation
import dev.nohus.rift.network.esi.models.CorporationsIdProjects
import dev.nohus.rift.network.esi.models.CorporationsIdProjectsId
import dev.nohus.rift.network.esi.models.CorporationsIdProjectsIdContribution
import dev.nohus.rift.network.esi.models.CorporationsIdProjectsIdContributors
import dev.nohus.rift.network.esi.models.FactionWarfareSystem
import dev.nohus.rift.network.esi.models.FleetMember
import dev.nohus.rift.network.esi.models.FleetsId
import dev.nohus.rift.network.esi.models.FreelanceJobs
import dev.nohus.rift.network.esi.models.FreelanceJobsId
import dev.nohus.rift.network.esi.models.GetCharactersFreelanceJobsParticipation
import dev.nohus.rift.network.esi.models.GetCorporationsFreelanceJobsParticipants
import dev.nohus.rift.network.esi.models.Incursion
import dev.nohus.rift.network.esi.models.IndustrySystem
import dev.nohus.rift.network.esi.models.KillmailIdHash
import dev.nohus.rift.network.esi.models.LoyaltyPoints
import dev.nohus.rift.network.esi.models.MarketsPrice
import dev.nohus.rift.network.esi.models.NewMailRequest
import dev.nohus.rift.network.esi.models.SovereigntySystem
import dev.nohus.rift.network.esi.models.Status
import dev.nohus.rift.network.esi.models.UniverseIdsResponse
import dev.nohus.rift.network.esi.models.UniverseName
import dev.nohus.rift.network.esi.models.UniverseStationsId
import dev.nohus.rift.network.esi.models.UniverseStructuresId
import dev.nohus.rift.network.esi.models.UniverseSystemJumps
import dev.nohus.rift.network.esi.models.UniverseSystemKills
import dev.nohus.rift.network.esi.models.WalletJournalEntry
import dev.nohus.rift.network.esi.models.WalletTransaction
import dev.nohus.rift.network.requests.Character
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.requests.Reply
import dev.nohus.rift.network.requests.RequestExecutor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.UUID

@Single
class EsiApi(
    @Named("network") json: Json,
    @Named("esi") client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://esi.evetech.net/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(EsiService::class.java)

    private val Int.authorization get() = Character(this)

    suspend fun getStatus(originator: Originator, characterId: Int): Result<Status> {
        return execute { service.getStatus(originator, UUID.randomUUID().toString(), characterId.authorization) }
    }

    suspend fun postUniverseIds(originator: Originator, names: List<String>): Result<UniverseIdsResponse> {
        return execute { service.postUniverseIds(originator, names) }
    }

    suspend fun postUniverseNames(originator: Originator, ids: List<Long>): Result<List<UniverseName>> {
        return execute { service.postUniverseNames(originator, ids) }
    }

    suspend fun getCharactersId(originator: Originator, characterId: Int): Result<CharactersIdCharacter> {
        return execute { service.getCharactersId(originator, characterId) }
    }

    suspend fun getCharactersAffiliation(originator: Originator, characterIds: List<Int>): Result<List<CharactersAffiliation>> {
        return execute { service.getCharactersAffiliation(originator, characterIds) }
    }

    suspend fun getCorporationsId(originator: Originator, corporationId: Int): Result<CorporationsIdCorporation> {
        return execute { service.getCorporationsId(originator, corporationId) }
    }

    suspend fun getAlliancesId(originator: Originator, allianceId: Int): Result<AlliancesIdAlliance> {
        return execute { service.getAlliancesId(originator, allianceId) }
    }

    suspend fun getAlliancesIdContacts(originator: Originator, characterId: Int, allianceId: Int): Result<List<Contact>> {
        return execute { service.getAlliancesIdContacts(originator, allianceId, characterId.authorization) }
    }

    suspend fun getCorporationsIdContacts(originator: Originator, characterId: Int, corporationId: Int): Result<List<Contact>> {
        return execute { service.getCorporationsIdContacts(originator, corporationId, characterId.authorization) }
    }

    suspend fun getCharactersIdContacts(originator: Originator, characterId: Int): Result<List<Contact>> {
        return execute { service.getCharactersIdContacts(originator, characterId, characterId.authorization) }
    }

    suspend fun getAlliancesIdContactsLabels(originator: Originator, characterId: Int, allianceId: Int): Result<List<ContactsLabel>> {
        return execute { service.getAlliancesIdContactsLabels(originator, allianceId, characterId.authorization) }
    }

    suspend fun getCorporationsIdContactsLabels(originator: Originator, characterId: Int, corporationId: Int): Result<List<ContactsLabel>> {
        return execute { service.getCorporationsIdContactsLabels(originator, corporationId, characterId.authorization) }
    }

    suspend fun getCharactersIdContactsLabels(originator: Originator, characterId: Int): Result<List<ContactsLabel>> {
        return execute { service.getCharactersIdContactsLabels(originator, characterId, characterId.authorization) }
    }

    suspend fun deleteCharactersIdContacts(
        originator: Originator,
        characterId: Int,
        contactIds: List<Int>,
    ): Result<Unit> {
        return execute {
            service.deleteCharactersIdContacts(
                originator = originator,
                characterId = characterId,
                contactIds = contactIds,
                characterId.authorization,
            )
        }
    }

    suspend fun postCharactersIdContacts(
        originator: Originator,
        characterId: Int,
        labelIds: List<Long>?,
        standing: Float,
        watched: Boolean?,
        contactIds: List<Int>,
    ): Result<List<Int>> {
        return execute {
            service.postCharactersIdContacts(
                originator = originator,
                characterId = characterId,
                labelIds = labelIds,
                standing = standing,
                watched = watched,
                characterId.authorization,
                contactIds = contactIds,
            )
        }
    }

    suspend fun putCharactersIdContacts(
        originator: Originator,
        characterId: Int,
        labelIds: List<Long>?,
        standing: Float,
        watched: Boolean?,
        contactIds: List<Int>,
    ): Result<Unit> {
        return execute {
            service.putCharactersIdContacts(originator, characterId, labelIds, standing, watched, characterId.authorization, contactIds)
        }
    }

    suspend fun getCharacterIdOnline(originator: Originator, characterId: Int): Result<CharacterIdOnline> {
        return execute { service.getCharacterIdOnline(originator, characterId, characterId.authorization) }
    }

    suspend fun getCharacterIdShip(originator: Originator, characterId: Int): Result<CharacterIdShip> {
        return execute { service.getCharacterIdShip(originator, characterId, characterId.authorization) }
    }

    suspend fun getCharacterIdLocation(originator: Originator, characterId: Int): Result<CharacterIdLocation> {
        return execute { service.getCharacterIdLocation(originator, characterId, characterId.authorization) }
    }

    suspend fun getCharacterIdWallet(originator: Originator, characterId: Int): Result<Double> {
        return execute {
            service.getCharactersIdWallet(originator, characterId, characterId.authorization)
        }
    }

    suspend fun getCharactersIdWalletJournal(
        originator: Originator,
        characterId: Int,
        page: Int? = null,
    ): Result<Reply<List<WalletJournalEntry>>> {
        return executeWithHeaders {
            service.getCharactersIdWalletJournal(originator, characterId, page, characterId.authorization)
        }
    }

    suspend fun getCharactersIdWalletTransactions(
        originator: Originator,
        characterId: Int,
        fromId: Long? = null,
    ): Result<List<WalletTransaction>> {
        return execute {
            service.getCharactersIdWalletTransactions(originator, characterId, fromId, characterId.authorization)
        }
    }

    suspend fun getCorporationsCorporationIdWallet(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
    ): Result<List<CorporationWalletBalance>> {
        return execute {
            service.getCorporationsCorporationIdWallets(originator, corporationId, characterId.authorization)
        }
    }

    suspend fun getCorporationsCorporationIdWalletsDivisionJournal(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        division: Int,
        page: Int? = null,
    ): Result<Reply<List<WalletJournalEntry>>> {
        return executeWithHeaders {
            service.getCorporationsCorporationIdWalletsDivisionJournal(originator, corporationId, division, page, characterId.authorization)
        }
    }

    suspend fun getCorporationsCorporationIdWalletsDivisionTransactions(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        division: Int,
        fromId: Long? = null,
    ): Result<List<WalletTransaction>> {
        return execute {
            service.getCorporationsCorporationIdWalletsDivisionTransactions(originator, corporationId, division, fromId, characterId.authorization)
        }
    }

    suspend fun getCorporationsCorporationIdDivisions(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
    ): Result<CorporationDivisions> {
        return execute { service.getCorporationsCorporationIdDivisions(originator, corporationId, characterId.authorization) }
    }

    suspend fun getCharactersIdSearch(originator: Originator, characterId: Int, categories: List<String>, strict: Boolean, search: String): Result<CharactersIdSearch> {
        return execute {
            service.getCharactersIdSearch(originator, characterId, categories, strict, search, characterId.authorization)
        }
    }

    suspend fun getCharactersIdClones(originator: Originator, characterId: Int): Result<CharactersIdClones> {
        return execute {
            service.getCharactersIdClones(originator, characterId, characterId.authorization)
        }
    }

    suspend fun getCharactersIdImplants(originator: Originator, characterId: Int): Result<List<Int>> {
        return execute {
            service.getCharactersIdImplants(originator, characterId, characterId.authorization)
        }
    }

    suspend fun getCharactersIdLoyaltyPoints(originator: Originator, characterId: Int): Result<List<LoyaltyPoints>> {
        return execute {
            service.getCharactersIdLoyaltyPoints(originator, characterId, characterId.authorization)
        }
    }

    suspend fun getUniverseStationsId(originator: Originator, stationId: Int): Result<UniverseStationsId> {
        return execute { service.getUniverseStationsId(originator, stationId) }
    }

    suspend fun getUniverseStructuresId(originator: Originator, structureId: Long, characterId: Int): Result<UniverseStructuresId> {
        return execute {
            service.getUniverseStructuresId(originator, structureId, characterId.authorization)
        }
    }

    suspend fun getUniverseSystemJumps(originator: Originator): Result<List<UniverseSystemJumps>> {
        return execute { service.getUniverseSystemJumps(originator) }
    }

    suspend fun getUniverseSystemKills(originator: Originator): Result<List<UniverseSystemKills>> {
        return execute { service.getUniverseSystemKills(originator) }
    }

    suspend fun getIncursions(originator: Originator): Result<List<Incursion>> {
        return execute { service.getIncursions(originator) }
    }

    suspend fun getFactionWarfareSystems(originator: Originator): Result<List<FactionWarfareSystem>> {
        return execute { service.getFactionWarfareSystems(originator) }
    }

    suspend fun getSovereigntyMap(originator: Originator): Result<List<SovereigntySystem>> {
        return execute { service.getSovereigntyMap(originator) }
    }

    suspend fun postUiAutopilotWaypoint(
        originator: Originator,
        destinationId: Long,
        clearOtherWaypoints: Boolean,
        characterId: Int,
    ): Result<Unit> {
        return execute {
            service.postUiAutopilotWaypoint(
                originator = originator,
                addToBeginning = false,
                clearOtherWaypoints = clearOtherWaypoints,
                destinationId = destinationId,
                characterId.authorization,
            )
        }
    }

    suspend fun getCharactersIdAssets(originator: Originator, page: Int, characterId: Int): Result<Reply<List<Asset>>> {
        return executeWithHeaders {
            service.getCharactersIdAssets(originator, characterId, page, characterId.authorization)
        }
    }

    suspend fun getCharactersIdAssetsNames(originator: Originator, characterId: Int, assets: List<Long>): Result<List<AssetName>> {
        return execute {
            service.getCharactersIdAssetsNames(originator, characterId, assets, characterId.authorization)
        }
    }

    suspend fun getCharactersIdAssetsLocations(originator: Originator, characterId: Int, itemIds: List<Long>): Result<List<AssetLocation>> {
        return execute {
            service.getCharactersIdAssetsLocations(originator, characterId, itemIds, characterId.authorization)
        }
    }

    suspend fun getCorporationsIdAssets(originator: Originator, page: Int, characterId: Int, corporationId: Int): Result<Reply<List<Asset>>> {
        return executeWithHeaders {
            service.getCorporationsIdAssets(originator, corporationId, page, characterId.authorization)
        }
    }

    suspend fun getCorporationsIdAssetsNames(originator: Originator, characterId: Int, corporationId: Int, assets: List<Long>): Result<List<AssetName>> {
        return execute {
            service.getCorporationsIdAssetsNames(originator, corporationId, assets, characterId.authorization)
        }
    }

    suspend fun getCorporationsIdAssetsLocations(originator: Originator, characterId: Int, corporationId: Int, itemIds: List<Long>): Result<List<AssetLocation>> {
        return execute {
            service.getCorporationsIdAssetsLocations(originator, corporationId, itemIds, characterId.authorization)
        }
    }

    suspend fun getMarketsPrices(originator: Originator): Result<List<MarketsPrice>> {
        return execute { service.getMarketsPrices(originator) }
    }

    suspend fun getCharactersIdFleet(originator: Originator, characterId: Int): Result<CharactersIdFleet> {
        return execute {
            service.getCharactersIdFleet(originator, characterId, characterId.authorization)
        }
    }

    suspend fun getFleetsId(originator: Originator, characterId: Int, fleetId: Long): Result<FleetsId> {
        return execute {
            service.getFleetsId(originator, fleetId, characterId.authorization)
        }
    }

    suspend fun getFleetsIdMembers(originator: Originator, characterId: Int, fleetId: Long): Result<List<FleetMember>> {
        return execute {
            service.getFleetsIdMembers(originator, fleetId, characterId.authorization)
        }
    }

    suspend fun getCharactersIdPlanets(originator: Originator, characterId: Int): Result<List<CharactersIdPlanet>> {
        return execute {
            service.getCharactersIdPlanets(originator, characterId, characterId.authorization)
        }
    }

    suspend fun getCharactersIdPlanetsId(originator: Originator, characterId: Int, planetId: Int): Result<CharactersIdPlanetsId> {
        return execute {
            service.getCharactersIdPlanetsId(originator, characterId, planetId, characterId.authorization)
        }
    }

    suspend fun getIndustrySystems(originator: Originator): Result<List<IndustrySystem>> {
        return execute { service.getIndustrySystems(originator) }
    }

    suspend fun getCorporationsIdProjects(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        before: String?,
        after: String?,
        limit: Int? = 100,
        state: CorporationProjectsQueryState?,
    ): Result<CorporationsIdProjects> {
        return execute {
            service.getCorporationsIdProjects(originator, corporationId, before, after, limit, state, characterId.authorization)
        }
    }

    suspend fun getCorporationsIdProjectsId(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        projectId: String,
        cacheBuster: String,
    ): Result<CorporationsIdProjectsId> {
        return execute {
            service.getCorporationsIdProjectsId(originator, corporationId, projectId, cacheBuster, characterId.authorization)
        }
    }

    suspend fun getCorporationsIdProjectsIdContribution(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        projectId: String,
        cacheBuster: String,
    ): Result<CorporationsIdProjectsIdContribution> {
        return execute {
            service.getCorporationsIdProjectsIdContribution(originator, corporationId, projectId, characterId, cacheBuster, characterId.authorization)
        }
    }

    suspend fun getCorporationsIdProjectsIdContributors(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        projectId: String,
        before: String?,
        after: String?,
        limit: Int? = 100,
        cacheBuster: String?,
    ): Result<CorporationsIdProjectsIdContributors> {
        return execute {
            service.getCorporationsIdProjectsIdContributors(originator, corporationId, projectId, before, after, limit, cacheBuster, characterId.authorization)
        }
    }

    suspend fun getFreelanceJobs(
        originator: Originator,
        characterId: Int,
        before: String?,
        after: String?,
        limit: Int? = 100,
        corporationId: Int? = null,
    ): Result<FreelanceJobs> {
        return execute {
            service.getFreelanceJobs(originator, before, after, limit, corporationId, characterId.authorization)
        }
    }

    suspend fun getFreelanceJobsId(
        originator: Originator,
        characterId: Int,
        jobId: String,
        cacheBuster: String,
    ): Result<FreelanceJobsId> {
        return execute {
            service.getFreelanceJobsId(originator, jobId, cacheBuster, characterId.authorization)
        }
    }

    suspend fun getCharactersIdFreelanceJobs(
        originator: Originator,
        characterId: Int,
    ): Result<FreelanceJobs> {
        return execute {
            service.getCharactersIdFreelanceJobs(originator, characterId, characterId.authorization)
        }
    }

    suspend fun getCharactersIdFreelanceJobsIdParticipation(
        originator: Originator,
        characterId: Int,
        jobId: String,
        cacheBuster: String,
    ): Result<GetCharactersFreelanceJobsParticipation> {
        return execute {
            service.getCharactersIdFreelanceJobsIdParticipation(originator, characterId, jobId, cacheBuster, characterId.authorization)
        }
    }

    suspend fun getCorporationsIdFreelanceJobs(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        before: String?,
        after: String?,
        limit: Int? = 100,
    ): Result<FreelanceJobs> {
        return execute {
            service.getCorporationsIdFreelanceJobs(originator, corporationId, before, after, limit, characterId.authorization)
        }
    }

    suspend fun getCorporationsIdFreelanceJobsIdParticipants(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        jobId: String,
        before: String?,
        after: String?,
        limit: Int? = 100,
        cacheBuster: String,
    ): Result<GetCorporationsFreelanceJobsParticipants> {
        return execute {
            service.getCorporationsIdFreelanceJobsIdParticipants(originator, corporationId, jobId, before, after, limit, cacheBuster, characterId.authorization)
        }
    }

    suspend fun getCharactersIdRoles(
        originator: Originator,
        characterId: Int,
    ): Result<CharactersIdRoles> {
        return execute {
            service.getCharactersIdRoles(originator, characterId, characterId.authorization)
        }
    }

    suspend fun postUiOpenWindowInformation(
        originator: Originator,
        characterId: Int,
        id: Long,
    ): Result<Unit> {
        return execute {
            service.postUiOpenWindowInformation(originator, id, characterId.authorization)
        }
    }

    suspend fun postUiOpenWindowMarketDetails(
        originator: Originator,
        characterId: Int,
        typeId: Long,
    ): Result<Unit> {
        return execute {
            service.postUiOpenWindowMarketDetails(originator, typeId, characterId.authorization)
        }
    }

    suspend fun postUiOpenWindowNewMail(
        originator: Originator,
        characterId: Int,
        request: NewMailRequest,
    ): Result<Unit> {
        return execute {
            service.postUiOpenWindowNewMail(originator, request, characterId.authorization)
        }
    }

    suspend fun getKillmailIdHash(
        originator: Originator,
        killmailId: String,
        killmailHash: String,
    ): Result<KillmailIdHash> {
        return execute {
            service.getKillmailIdHash(originator, killmailId, killmailHash)
        }
    }
}
