package dev.nohus.rift.network.esi

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
import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.requests.RateLimit
import dev.nohus.rift.network.requests.RateLimitGroup
import dev.nohus.rift.network.requests.Scope
import dev.nohus.rift.sso.scopes.EsiScope
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

interface EsiService {

    @GET("/status")
    @EndpointTag(Endpoint.GetStatus::class)
    @RateLimit(RateLimitGroup.Status::class)
    suspend fun getStatus(
        @Tag originator: Originator,
        @Query("cb") cacheBuster: String,
        @Tag character: Character,
    ): Status

    @POST("/universe/ids")
    @EndpointTag(Endpoint.PostUniverseIds::class)
    @RateLimit(RateLimitGroup.StaticData::class)
    suspend fun postUniverseIds(
        @Tag originator: Originator,
        @Body names: List<String>,
    ): UniverseIdsResponse

    @POST("/universe/names")
    @EndpointTag(Endpoint.PostUniverseNames::class)
    @RateLimit(RateLimitGroup.StaticData::class)
    suspend fun postUniverseNames(
        @Tag originator: Originator,
        @Body ids: List<Long>,
    ): List<UniverseName>

    @GET("/characters/{id}")
    @EndpointTag(Endpoint.GetCharactersId::class)
    @RateLimit(RateLimitGroup.Character::class)
    suspend fun getCharactersId(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
    ): CharactersIdCharacter

    @POST("/characters/affiliation")
    @EndpointTag(Endpoint.GetCharactersAffiliation::class)
    @RateLimit(RateLimitGroup.Character::class)
    suspend fun getCharactersAffiliation(
        @Tag originator: Originator,
        @Body characterIds: List<Int>,
    ): List<CharactersAffiliation>

    @GET("/corporations/{id}")
    @EndpointTag(Endpoint.GetCorporationsId::class)
    @RateLimit(RateLimitGroup.Corporation::class)
    suspend fun getCorporationsId(
        @Tag originator: Originator,
        @Path("id") corporationId: Int,
    ): CorporationsIdCorporation

    @GET("/alliances/{id}")
    @EndpointTag(Endpoint.GetAlliancesId::class)
    @RateLimit(RateLimitGroup.Alliance::class)
    suspend fun getAlliancesId(
        @Tag originator: Originator,
        @Path("id") allianceId: Int,
    ): AlliancesIdAlliance

    @GET("/alliances/{id}/contacts")
    @EndpointTag(Endpoint.GetAlliancesIdContacts::class)
    @RateLimit(RateLimitGroup.AllianceSocial::class)
    @Scope(EsiScope.Alliances.ReadContacts::class)
    suspend fun getAlliancesIdContacts(
        @Tag originator: Originator,
        @Path("id") allianceId: Int,
        @Tag character: Character,
    ): List<Contact>

    @GET("/corporations/{id}/contacts")
    @EndpointTag(Endpoint.GetCorporationsIdContacts::class)
    @RateLimit(RateLimitGroup.CorpSocial::class)
    @Scope(EsiScope.Corporations.ReadContacts::class)
    suspend fun getCorporationsIdContacts(
        @Tag originator: Originator,
        @Path("id") corporationId: Int,
        @Tag character: Character,
    ): List<Contact>

    @GET("/characters/{id}/contacts")
    @EndpointTag(Endpoint.GetCharactersIdContacts::class)
    @RateLimit(RateLimitGroup.CharSocial::class)
    @Scope(EsiScope.Characters.ReadContacts::class)
    suspend fun getCharactersIdContacts(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): List<Contact>

    @GET("/alliances/{id}/contacts/labels")
    @EndpointTag(Endpoint.GetAlliancesIdContactsLabels::class)
    @RateLimit(RateLimitGroup.AllianceSocial::class)
    @Scope(EsiScope.Alliances.ReadContacts::class)
    suspend fun getAlliancesIdContactsLabels(
        @Tag originator: Originator,
        @Path("id") allianceId: Int,
        @Tag character: Character,
    ): List<ContactsLabel>

    @GET("/corporations/{id}/contacts/labels")
    @EndpointTag(Endpoint.GetCorporationsIdContactsLabels::class)
    @RateLimit(RateLimitGroup.CorpSocial::class)
    @Scope(EsiScope.Corporations.ReadContacts::class)
    suspend fun getCorporationsIdContactsLabels(
        @Tag originator: Originator,
        @Path("id") corporationId: Int,
        @Tag character: Character,
    ): List<ContactsLabel>

    @GET("/characters/{id}/contacts/labels")
    @EndpointTag(Endpoint.GetCharactersIdContactsLabels::class)
    @RateLimit(RateLimitGroup.CharSocial::class)
    @Scope(EsiScope.Characters.ReadContacts::class)
    suspend fun getCharactersIdContactsLabels(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): List<ContactsLabel>

    @DELETE("/characters/{id}/contacts")
    @EndpointTag(Endpoint.DeleteCharactersIdContacts::class)
    @RateLimit(RateLimitGroup.CharSocial::class)
    @Scope(EsiScope.Characters.WriteContacts::class)
    suspend fun deleteCharactersIdContacts(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Query("contact_ids") contactIds: List<Int>,
        @Tag character: Character,
    )

    @POST("/characters/{id}/contacts")
    @EndpointTag(Endpoint.PostCharactersIdContacts::class)
    @RateLimit(RateLimitGroup.CharSocial::class)
    @Scope(EsiScope.Characters.WriteContacts::class)
    suspend fun postCharactersIdContacts(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Query("label_ids") labelIds: List<Long>?,
        @Query("standing") standing: Float,
        @Query("watched") watched: Boolean?,
        @Tag character: Character,
        @Body contactIds: List<Int>,
    ): List<Int>

    @PUT("/characters/{id}/contacts")
    @EndpointTag(Endpoint.PutCharactersIdContacts::class)
    @RateLimit(RateLimitGroup.CharSocial::class)
    @Scope(EsiScope.Characters.WriteContacts::class)
    suspend fun putCharactersIdContacts(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Query("label_ids") labelIds: List<Long>?,
        @Query("standing") standing: Float,
        @Query("watched") watched: Boolean?,
        @Tag character: Character,
        @Body contactIds: List<Int>,
    ): Response<Unit>

    @GET("/characters/{id}/online")
    @EndpointTag(Endpoint.GetCharacterIdOnline::class)
    @RateLimit(RateLimitGroup.CharLocation::class)
    @Scope(EsiScope.Locations.ReadOnline::class)
    suspend fun getCharacterIdOnline(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): CharacterIdOnline

    @GET("/characters/{id}/ship")
    @EndpointTag(Endpoint.GetCharacterIdShip::class)
    @RateLimit(RateLimitGroup.CharLocation::class)
    @Scope(EsiScope.Locations.ReadShipType::class)
    suspend fun getCharacterIdShip(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): CharacterIdShip

    @GET("/characters/{id}/location")
    @EndpointTag(Endpoint.GetCharacterIdLocation::class)
    @RateLimit(RateLimitGroup.CharLocation::class)
    @Scope(EsiScope.Locations.ReadLocation::class)
    suspend fun getCharacterIdLocation(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): CharacterIdLocation

    @GET("/characters/{id}/wallet")
    @EndpointTag(Endpoint.GetCharacterIdWallet::class)
    @RateLimit(RateLimitGroup.CharWallet::class)
    @Scope(EsiScope.Wallet.ReadCharacterWallet::class)
    suspend fun getCharactersIdWallet(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): Double

    @GET("/characters/{id}/wallet/journal")
    @EndpointTag(Endpoint.GetCharactersIdWalletJournal::class)
    @RateLimit(RateLimitGroup.CharWallet::class)
    @Scope(EsiScope.Wallet.ReadCharacterWallet::class)
    suspend fun getCharactersIdWalletJournal(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Query("page") page: Int?,
        @Tag character: Character,
    ): Response<List<WalletJournalEntry>>

    @GET("/characters/{id}/wallet/transactions")
    @EndpointTag(Endpoint.GetCharactersIdWalletTransactions::class)
    @RateLimit(RateLimitGroup.CharWallet::class)
    @Scope(EsiScope.Wallet.ReadCharacterWallet::class)
    suspend fun getCharactersIdWalletTransactions(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Query("from_id") fromId: Long?,
        @Tag character: Character,
    ): List<WalletTransaction>

    @GET("/corporations/{corporation_id}/wallets")
    @EndpointTag(Endpoint.GetCorporationsCorporationIdWallet::class)
    @RateLimit(RateLimitGroup.CorpWallet::class)
    @Scope(EsiScope.Wallet.ReadCorporationWallets::class)
    suspend fun getCorporationsCorporationIdWallets(
        @Tag originator: Originator,
        @Path("corporation_id") corporationId: Int,
        @Tag character: Character,
    ): List<CorporationWalletBalance>

    @GET("/corporations/{corporation_id}/wallets/{division}/journal")
    @EndpointTag(Endpoint.GetCorporationsCorporationIdWalletsDivisionJournal::class)
    @RateLimit(RateLimitGroup.CorpWallet::class)
    @Scope(EsiScope.Wallet.ReadCorporationWallets::class)
    suspend fun getCorporationsCorporationIdWalletsDivisionJournal(
        @Tag originator: Originator,
        @Path("corporation_id") corporationId: Int,
        @Path("division") division: Int,
        @Query("page") page: Int?,
        @Tag character: Character,
    ): Response<List<WalletJournalEntry>>

    @GET("/corporations/{corporation_id}/wallets/{division}/transactions")
    @EndpointTag(Endpoint.GetCorporationsCorporationIdWalletsDivisionTransactions::class)
    @RateLimit(RateLimitGroup.CorpWallet::class)
    @Scope(EsiScope.Wallet.ReadCorporationWallets::class)
    suspend fun getCorporationsCorporationIdWalletsDivisionTransactions(
        @Tag originator: Originator,
        @Path("corporation_id") corporationId: Int,
        @Path("division") division: Int,
        @Query("from_id") fromId: Long?,
        @Tag character: Character,
    ): List<WalletTransaction>

    @GET("/corporations/{corporation_id}/divisions")
    @EndpointTag(Endpoint.GetCorporationsCorporationIdDivisions::class)
    @RateLimit(RateLimitGroup.CorpWallet::class)
    @Scope(EsiScope.Corporations.ReadDivisions::class)
    suspend fun getCorporationsCorporationIdDivisions(
        @Tag originator: Originator,
        @Path("corporation_id") corporationId: Int,
        @Tag character: Character,
    ): CorporationDivisions

    @GET("/characters/{id}/search")
    @EndpointTag(Endpoint.GetCharactersIdSearch::class)
    @RateLimit(RateLimitGroup.CharDetail::class)
    @Scope(EsiScope.Search.SearchStructures::class)
    suspend fun getCharactersIdSearch(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Query("categories") categories: List<String>,
        @Query("strict") strict: Boolean,
        @Query("search") search: String,
        @Tag character: Character,
    ): CharactersIdSearch

    @GET("/characters/{id}/clones")
    @EndpointTag(Endpoint.GetCharactersIdClones::class)
    @RateLimit(RateLimitGroup.CharLocation::class)
    @Scope(EsiScope.Clones.ReadClones::class)
    suspend fun getCharactersIdClones(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): CharactersIdClones

    @GET("/characters/{id}/implants")
    @EndpointTag(Endpoint.GetCharactersIdImplants::class)
    @RateLimit(RateLimitGroup.CharDetail::class)
    @Scope(EsiScope.Clones.ReadImplants::class)
    suspend fun getCharactersIdImplants(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): List<Int>

    @GET("/characters/{id}/loyalty/points")
    @EndpointTag(Endpoint.GetCharactersIdLoyaltyPoints::class)
    @RateLimit(RateLimitGroup.CharWallet::class)
    @Scope(EsiScope.Characters.ReadLoyalty::class)
    suspend fun getCharactersIdLoyaltyPoints(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): List<LoyaltyPoints>

    @GET("/universe/stations/{id}")
    @EndpointTag(Endpoint.GetUniverseStationsId::class)
    @RateLimit(RateLimitGroup.StaticData::class)
    suspend fun getUniverseStationsId(
        @Tag originator: Originator,
        @Path("id") stationId: Int,
    ): UniverseStationsId

    @GET("/universe/structures/{id}")
    @EndpointTag(Endpoint.GetUniverseStructuresId::class)
    @RateLimit(RateLimitGroup.StaticData::class)
    @Scope(EsiScope.Universe.ReadStructures::class)
    suspend fun getUniverseStructuresId(
        @Tag originator: Originator,
        @Path("id") structureId: Long,
        @Tag character: Character,
    ): UniverseStructuresId

    @GET("/universe/system_jumps")
    @EndpointTag(Endpoint.GetUniverseSystemJumps::class)
    @RateLimit(RateLimitGroup.StaticData::class)
    suspend fun getUniverseSystemJumps(
        @Tag originator: Originator,
    ): List<UniverseSystemJumps>

    @GET("/universe/system_kills")
    @EndpointTag(Endpoint.GetUniverseSystemKills::class)
    @RateLimit(RateLimitGroup.StaticData::class)
    suspend fun getUniverseSystemKills(
        @Tag originator: Originator,
    ): List<UniverseSystemKills>

    @GET("/incursions")
    @EndpointTag(Endpoint.GetIncursions::class)
    @RateLimit(RateLimitGroup.Incursion::class)
    suspend fun getIncursions(
        @Tag originator: Originator,
    ): List<Incursion>

    @GET("/fw/systems")
    @EndpointTag(Endpoint.GetFactionWarfareSystems::class)
    @RateLimit(RateLimitGroup.FactionalWarfare::class)
    suspend fun getFactionWarfareSystems(
        @Tag originator: Originator,
    ): List<FactionWarfareSystem>

    @GET("/sovereignty/map")
    @EndpointTag(Endpoint.GetSovereigntyMap::class)
    @RateLimit(RateLimitGroup.Sovereignty::class)
    suspend fun getSovereigntyMap(
        @Tag originator: Originator,
    ): List<SovereigntySystem>

    @POST("/ui/autopilot/waypoint")
    @EndpointTag(Endpoint.PostUiAutopilotWaypoint::class)
    @RateLimit(RateLimitGroup.Ui::class)
    @Scope(EsiScope.Ui.WriteWaypoint::class)
    suspend fun postUiAutopilotWaypoint(
        @Tag originator: Originator,
        @Query("add_to_beginning") addToBeginning: Boolean,
        @Query("clear_other_waypoints") clearOtherWaypoints: Boolean,
        @Query("destination_id") destinationId: Long,
        @Tag character: Character,
    ): Response<Unit>

    @GET("/characters/{id}/assets")
    @EndpointTag(Endpoint.GetCharactersIdAssets::class)
    @RateLimit(RateLimitGroup.CharAsset::class)
    @Scope(EsiScope.Assets.ReadAssets::class)
    suspend fun getCharactersIdAssets(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Query("page") page: Int,
        @Tag character: Character,
    ): Response<List<Asset>>

    @POST("/characters/{id}/assets/names")
    @EndpointTag(Endpoint.GetCharactersIdAssetsNames::class)
    @RateLimit(RateLimitGroup.CharAsset::class)
    @Scope(EsiScope.Assets.ReadAssets::class)
    suspend fun getCharactersIdAssetsNames(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Body assets: List<Long>,
        @Tag character: Character,
    ): List<AssetName>

    @POST("/characters/{id}/assets/locations")
    @EndpointTag(Endpoint.GetCharactersIdAssetsLocations::class)
    @RateLimit(RateLimitGroup.CharAsset::class)
    @Scope(EsiScope.Assets.ReadAssets::class)
    suspend fun getCharactersIdAssetsLocations(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Body itemIds: List<Long>,
        @Tag character: Character,
    ): List<AssetLocation>

    @GET("/corporations/{id}/assets")
    @EndpointTag(Endpoint.GetCorporationsIdAssets::class)
    @RateLimit(RateLimitGroup.CorpAsset::class)
    @Scope(EsiScope.Assets.ReadAssets::class)
    suspend fun getCorporationsIdAssets(
        @Tag originator: Originator,
        @Path("id") corporationId: Int,
        @Query("page") page: Int,
        @Tag character: Character,
    ): Response<List<Asset>>

    @POST("/corporations/{id}/assets/names")
    @EndpointTag(Endpoint.GetCorporationsIdAssetsNames::class)
    @RateLimit(RateLimitGroup.CorpAsset::class)
    @Scope(EsiScope.Assets.ReadAssets::class)
    suspend fun getCorporationsIdAssetsNames(
        @Tag originator: Originator,
        @Path("id") corporationId: Int,
        @Body assets: List<Long>,
        @Tag character: Character,
    ): List<AssetName>

    @POST("/corporations/{id}/assets/locations")
    @EndpointTag(Endpoint.GetCorporationsIdAssetsLocations::class)
    @RateLimit(RateLimitGroup.CorpAsset::class)
    @Scope(EsiScope.Assets.ReadAssets::class)
    suspend fun getCorporationsIdAssetsLocations(
        @Tag originator: Originator,
        @Path("id") corporationId: Int,
        @Body itemIds: List<Long>,
        @Tag character: Character,
    ): List<AssetLocation>

    @GET("/markets/prices")
    @EndpointTag(Endpoint.GetMarketsPrices::class)
    @RateLimit(RateLimitGroup.Market::class)
    suspend fun getMarketsPrices(
        @Tag originator: Originator,
    ): List<MarketsPrice>

    @GET("/characters/{id}/fleet")
    @EndpointTag(Endpoint.GetCharactersIdFleet::class)
    @RateLimit(RateLimitGroup.Fleet::class)
    @Scope(EsiScope.Fleets.ReadFleet::class)
    suspend fun getCharactersIdFleet(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): CharactersIdFleet

    @GET("/fleets/{id}")
    @EndpointTag(Endpoint.GetFleetsId::class)
    @RateLimit(RateLimitGroup.Fleet::class)
    @Scope(EsiScope.Fleets.ReadFleet::class)
    suspend fun getFleetsId(
        @Tag originator: Originator,
        @Path("id") fleetId: Long,
        @Tag character: Character,
    ): FleetsId

    @GET("/fleets/{id}/members")
    @EndpointTag(Endpoint.GetFleetsIdMembers::class)
    @RateLimit(RateLimitGroup.Fleet::class)
    @Scope(EsiScope.Fleets.ReadFleet::class)
    suspend fun getFleetsIdMembers(
        @Tag originator: Originator,
        @Path("id") fleetId: Long,
        @Tag character: Character,
    ): List<FleetMember>

    @GET("/characters/{id}/planets")
    @EndpointTag(Endpoint.GetCharactersIdPlanets::class)
    @RateLimit(RateLimitGroup.CharIndustry::class)
    @Scope(EsiScope.Planets.ManagePlanets::class)
    suspend fun getCharactersIdPlanets(
        @Tag originator: Originator,
        @Path("id") characterId: Int,
        @Tag character: Character,
    ): List<CharactersIdPlanet>

    @GET("/characters/{character_id}/planets/{planet_id}")
    @EndpointTag(Endpoint.GetCharactersIdPlanetsId::class)
    @RateLimit(RateLimitGroup.CharIndustry::class)
    @Scope(EsiScope.Planets.ManagePlanets::class)
    suspend fun getCharactersIdPlanetsId(
        @Tag originator: Originator,
        @Path("character_id") characterId: Int,
        @Path("planet_id") planetId: Int,
        @Tag character: Character,
    ): CharactersIdPlanetsId

    @GET("/industry/systems")
    @EndpointTag(Endpoint.GetIndustrySystems::class)
    @RateLimit(RateLimitGroup.Industry::class)
    suspend fun getIndustrySystems(
        @Tag originator: Originator,
    ): List<IndustrySystem>

    @GET("/corporations/{id}/projects")
    @EndpointTag(Endpoint.GetCorporationsIdProjects::class)
    @RateLimit(RateLimitGroup.CorpProject::class)
    @Scope(EsiScope.Corporations.ReadProjects::class)
    suspend fun getCorporationsIdProjects(
        @Tag originator: Originator,
        @Path("id") corporationId: Int,
        @Query("before") before: String?,
        @Query("after") after: String?,
        @Query("limit") limit: Int?,
        @Query("state") state: CorporationProjectsQueryState?,
        @Tag character: Character,
    ): CorporationsIdProjects

    @GET("/corporations/{corporation_id}/projects/{project_id}")
    @EndpointTag(Endpoint.GetCorporationsIdProjectsId::class)
    @RateLimit(RateLimitGroup.CorpProject::class)
    @Scope(EsiScope.Corporations.ReadProjects::class)
    suspend fun getCorporationsIdProjectsId(
        @Tag originator: Originator,
        @Path("corporation_id") corporationId: Int,
        @Path("project_id") projectId: String,
        @Query("cb") cacheBuster: String,
        @Tag character: Character,
    ): CorporationsIdProjectsId

    @GET("/corporations/{corporation_id}/projects/{project_id}/contribution/{character_id}")
    @EndpointTag(Endpoint.GetCorporationsIdProjectsIdContribution::class)
    @RateLimit(RateLimitGroup.CorpProject::class)
    @Scope(EsiScope.Corporations.ReadProjects::class)
    suspend fun getCorporationsIdProjectsIdContribution(
        @Tag originator: Originator,
        @Path("corporation_id") corporationId: Int,
        @Path("project_id") projectId: String,
        @Path("character_id") characterId: Int,
        @Query("cb") cacheBuster: String,
        @Tag character: Character,
    ): CorporationsIdProjectsIdContribution

    @GET("/corporations/{corporation_id}/projects/{project_id}/contributors")
    @EndpointTag(Endpoint.GetCorporationsIdProjectsIdContributors::class)
    @RateLimit(RateLimitGroup.CorpProject::class)
    @Scope(EsiScope.Corporations.ReadProjects::class)
    suspend fun getCorporationsIdProjectsIdContributors(
        @Tag originator: Originator,
        @Path("corporation_id") corporationId: Int,
        @Path("project_id") projectId: String,
        @Query("before") before: String?,
        @Query("after") after: String?,
        @Query("limit") limit: Int?,
        @Query("cb") cacheBuster: String?,
        @Tag character: Character,
    ): CorporationsIdProjectsIdContributors

    @GET("/freelance-jobs")
    @EndpointTag(Endpoint.GetFreelanceJobs::class)
    @RateLimit(RateLimitGroup.FreelanceJob::class)
    suspend fun getFreelanceJobs(
        @Tag originator: Originator,
        @Query("before") before: String?,
        @Query("after") after: String?,
        @Query("limit") limit: Int?,
        @Query("corporation_id") corporationId: Int?,
        @Tag character: Character,
    ): FreelanceJobs

    @GET("/freelance-jobs/{job_id}")
    @EndpointTag(Endpoint.GetFreelanceJobsId::class)
    @RateLimit(RateLimitGroup.FreelanceJob::class)
    suspend fun getFreelanceJobsId(
        @Tag originator: Originator,
        @Path("job_id") jobId: String,
        @Query("cb") cacheBuster: String,
        @Tag character: Character,
    ): FreelanceJobsId

    @GET("/characters/{character_id}/freelance-jobs")
    @EndpointTag(Endpoint.GetCharactersIdFreelanceJobs::class)
    @RateLimit(RateLimitGroup.CharFreelanceJob::class)
    @Scope(EsiScope.Characters.ReadFreelanceJobs::class)
    suspend fun getCharactersIdFreelanceJobs(
        @Tag originator: Originator,
        @Path("character_id") characterId: Int,
        @Tag character: Character,
    ): FreelanceJobs

    @GET("/characters/{character_id}/freelance-jobs/{job_id}/participation")
    @EndpointTag(Endpoint.GetCharactersIdFreelanceJobsIdParticipation::class)
    @RateLimit(RateLimitGroup.CharFreelanceJob::class)
    @Scope(EsiScope.Characters.ReadFreelanceJobs::class)
    suspend fun getCharactersIdFreelanceJobsIdParticipation(
        @Tag originator: Originator,
        @Path("character_id") characterId: Int,
        @Path("job_id") jobId: String,
        @Query("cb") cacheBuster: String?,
        @Tag character: Character,
    ): GetCharactersFreelanceJobsParticipation

    @GET("/corporations/{corporation_id}/freelance-jobs")
    @EndpointTag(Endpoint.GetCorporationsIdFreelanceJobs::class)
    @RateLimit(RateLimitGroup.CorpFreelanceJob::class)
    @Scope(EsiScope.Corporations.ReadFreelanceJobs::class)
    suspend fun getCorporationsIdFreelanceJobs(
        @Tag originator: Originator,
        @Path("corporation_id") corporationId: Int,
        @Query("before") before: String?,
        @Query("after") after: String?,
        @Query("limit") limit: Int?,
        @Tag character: Character,
    ): FreelanceJobs

    @GET("/corporations/{corporation_id}/freelance-jobs/{job_id}/participants")
    @EndpointTag(Endpoint.GetCorporationsIdFreelanceJobsIdParticipants::class)
    @RateLimit(RateLimitGroup.CorpFreelanceJob::class)
    @Scope(EsiScope.Corporations.ReadFreelanceJobs::class)
    suspend fun getCorporationsIdFreelanceJobsIdParticipants(
        @Tag originator: Originator,
        @Path("corporation_id") corporationId: Int,
        @Path("job_id") jobId: String,
        @Query("before") before: String?,
        @Query("after") after: String?,
        @Query("limit") limit: Int?,
        @Query("cb") cacheBuster: String?,
        @Tag character: Character,
    ): GetCorporationsFreelanceJobsParticipants

    @GET("/characters/{character_id}/roles")
    @EndpointTag(Endpoint.GetCharactersIdRoles::class)
    @RateLimit(RateLimitGroup.CharDetail::class)
    @Scope(EsiScope.Characters.ReadCorporationRoles::class)
    suspend fun getCharactersIdRoles(
        @Tag originator: Originator,
        @Path("character_id") characterId: Int,
        @Tag character: Character,
    ): CharactersIdRoles

    @POST("/ui/openwindow/information")
    @EndpointTag(Endpoint.PostUiOpenWindowInformation::class)
    @RateLimit(RateLimitGroup.Ui::class)
    @Scope(EsiScope.Ui.OpenWindow::class)
    suspend fun postUiOpenWindowInformation(
        @Tag originator: Originator,
        @Query("target_id") id: Long,
        @Tag character: Character,
    )

    @POST("/ui/openwindow/marketdetails")
    @EndpointTag(Endpoint.PostUiOpenWindowMarketDetails::class)
    @RateLimit(RateLimitGroup.Ui::class)
    @Scope(EsiScope.Ui.OpenWindow::class)
    suspend fun postUiOpenWindowMarketDetails(
        @Tag originator: Originator,
        @Query("type_id") typeId: Long,
        @Tag character: Character,
    )

    @POST("/ui/openwindow/newmail")
    @EndpointTag(Endpoint.PostUiOpenWindowNewMail::class)
    @RateLimit(RateLimitGroup.Ui::class)
    @Scope(EsiScope.Ui.OpenWindow::class)
    suspend fun postUiOpenWindowNewMail(
        @Tag originator: Originator,
        @Body request: NewMailRequest,
        @Tag character: Character,
    )

    @GET("/killmails/{killmail_id}/{killmail_hash}")
    @EndpointTag(Endpoint.GetKillmail::class)
    @RateLimit(RateLimitGroup.Killmail::class)
    suspend fun getKillmailIdHash(
        @Tag originator: Originator,
        @Path("killmail_id") killmailId: String,
        @Path("killmail_hash") killmailHash: String,
    ): KillmailIdHash
}
