package dev.nohus.rift.network.esi.models

import dev.nohus.rift.network.IsoDateTimeSerializer
import dev.nohus.rift.network.esi.pagination.OffsetId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Status(
    @SerialName("players")
    val playersOnline: Int,
    @SerialName("server_version")
    val serverVersion: String,
    @SerialName("start_time")
    @Serializable(with = IsoDateTimeSerializer::class)
    val startTime: Instant,
    @SerialName("vip")
    val isVip: Boolean? = null,
)

@Serializable
data class UniverseIdsResponse(
    @SerialName("characters")
    val characters: List<UniverseIdsCharacter>? = null,
)

@Serializable
data class UniverseName(
    @SerialName("category")
    val category: UniverseNamesCategory,
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
)

@Serializable
enum class UniverseNamesCategory {
    @SerialName("character")
    Character,

    @SerialName("constellation")
    Constellation,

    @SerialName("corporation")
    Corporation,

    @SerialName("inventory_type")
    InventoryType,

    @SerialName("region")
    Region,

    @SerialName("solar_system")
    SolarSystem,

    @SerialName("station")
    Station,

    @SerialName("faction")
    Faction,

    @SerialName("alliance")
    Alliance,
}

@Serializable
data class UniverseIdsCharacter(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
)

@Serializable
data class CharactersIdCharacter(
    @SerialName("alliance_id")
    val allianceId: Int? = null,
    @SerialName("corporation_id")
    val corporationId: Int,
    @SerialName("name")
    val name: String,
    @SerialName("title")
    val title: String? = null,
    @SerialName("birthday")
    @Serializable(with = IsoDateTimeSerializer::class)
    val birthday: Instant,
)

@Serializable
data class CharactersAffiliation(
    @SerialName("character_id")
    val characterId: Int,
    @SerialName("corporation_id")
    val corporationId: Int,
    @SerialName("alliance_id")
    val allianceId: Int? = null,
    @SerialName("faction_id")
    val factionId: Int? = null,
)

@Serializable
data class CorporationsIdCorporation(
    @SerialName("name")
    val name: String,
    @SerialName("ticker")
    val ticker: String,
    @SerialName("alliance_id")
    val allianceId: Int? = null,
)

@Serializable
data class AlliancesIdAlliance(
    @SerialName("name")
    val name: String,
    @SerialName("ticker")
    val ticker: String,
)

@Serializable
data class Contact(
    @SerialName("contact_id")
    val contactId: Int,
    @SerialName("contact_type")
    val contactType: ContactType,
    @SerialName("is_blocked")
    val isBlocked: Boolean? = null,
    @SerialName("is_watched")
    val isWatched: Boolean? = null,
    @SerialName("label_ids")
    val labelIds: List<Long>? = emptyList(),
    @SerialName("standing")
    val standing: Float,
)

@Serializable
enum class ContactType {
    @SerialName("character")
    Character,

    @SerialName("corporation")
    Corporation,

    @SerialName("alliance")
    Alliance,

    @SerialName("faction")
    Faction,
}

@Serializable
data class ContactsLabel(
    @SerialName("label_id")
    val labelId: Long,
    @SerialName("label_name")
    val labelName: String,
)

@Serializable
data class CharactersIdClones(
    @SerialName("home_location")
    val homeLocation: HomeLocation? = null,
    @SerialName("jump_clones")
    val jumpClones: List<JumpClone>,
)

@Serializable
data class JumpClone(
    @SerialName("implants")
    val implants: List<Int>,
    @SerialName("jump_clone_id")
    val id: Int,
    @SerialName("location_id")
    val locationId: Long,
    @SerialName("location_type")
    val locationType: LocationType,
)

@Serializable
data class HomeLocation(
    @SerialName("location_id")
    val locationId: Long,
    @SerialName("location_type")
    val locationType: LocationType,
)

@Serializable
enum class LocationType {
    @SerialName("station")
    Station,

    @SerialName("structure")
    Structure,
}

@Serializable
data class CharacterIdOnline(
    @SerialName("last_login")
    val lastLogin: String? = null,
    @SerialName("last_logout")
    val lastLogout: String? = null,
    @SerialName("logins")
    val logins: Int? = null,
    @SerialName("online")
    val isOnline: Boolean,
)

@Serializable
data class CharacterIdShip(
    @SerialName("ship_item_id")
    val shipItemId: Long,
    @SerialName("ship_name")
    val shipName: String,
    @SerialName("ship_type_id")
    val shipTypeId: Int,
)

@Serializable
data class CharacterIdLocation(
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("station_id")
    val stationId: Int? = null,
    @SerialName("structure_id")
    val structureId: Long? = null,
)

@Serializable
data class UniverseStationsId(
    @SerialName("name")
    val name: String,
    @SerialName("owner")
    val ownerId: Int? = null,
    @SerialName("system_id")
    val systemId: Int,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
data class UniverseStructuresId(
    @SerialName("name")
    val name: String,
    @SerialName("owner_id")
    val ownerId: Int,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("type_id")
    val typeId: Int? = null,
)

@Serializable
data class UniverseSystemJumps(
    @SerialName("ship_jumps")
    val shipJumps: Int,
    @SerialName("system_id")
    val systemId: Int,
)

@Serializable
data class UniverseSystemKills(
    @SerialName("npc_kills")
    val npcKills: Int,
    @SerialName("pod_kills")
    val podKills: Int,
    @SerialName("ship_kills")
    val shipKills: Int,
    @SerialName("system_id")
    val systemId: Int,
)

@Serializable
data class Incursion(
    @SerialName("infested_solar_systems")
    val infestedSolarSystems: List<Int>,
    @SerialName("state")
    val state: IncursionState,
    @SerialName("type")
    val type: String,
)

@Serializable
enum class IncursionState {
    @SerialName("withdrawing")
    Withdrawing,

    @SerialName("mobilizing")
    Mobilizing,

    @SerialName("established")
    Established,
}

@Serializable
data class FactionWarfareSystem(
    @SerialName("contested")
    val contested: Contested,
    @SerialName("occupier_faction_id")
    val occupierFactionId: Int,
    @SerialName("owner_faction_id")
    val ownerFactionId: Int,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("victory_points")
    val victoryPoints: Int,
    @SerialName("victory_points_threshold")
    val victoryPointsThreshold: Int,
)

@Serializable
enum class Contested {
    @SerialName("captured")
    Captured,

    @SerialName("contested")
    Contested,

    @SerialName("uncontested")
    Uncontested,

    @SerialName("vulnerable")
    Vulnerable,
}

@Serializable
data class SovereigntySystem(
    @SerialName("alliance_id")
    val allianceId: Int? = null,
    @SerialName("corporation_id")
    val corporationId: Int? = null,
    @SerialName("faction_id")
    val factionId: Int? = null,
    @SerialName("system_id")
    val systemId: Int,
)

@Serializable
data class CharactersIdSearch(
    @SerialName("agent")
    val agent: List<Long> = emptyList(),
    @SerialName("alliance")
    val alliance: List<Long> = emptyList(),
    @SerialName("character")
    val character: List<Long> = emptyList(),
    @SerialName("constellation")
    val constellation: List<Long> = emptyList(),
    @SerialName("corporation")
    val corporation: List<Long> = emptyList(),
    @SerialName("faction")
    val faction: List<Long> = emptyList(),
    @SerialName("inventory_type")
    val inventoryType: List<Long> = emptyList(),
    @SerialName("region")
    val region: List<Long> = emptyList(),
    @SerialName("solar_system")
    val solarSystem: List<Long> = emptyList(),
    @SerialName("station")
    val station: List<Long> = emptyList(),
    @SerialName("structure")
    val structure: List<Long> = emptyList(),
)

@Serializable
data class Asset(
    @SerialName("is_blueprint_copy")
    val isBlueprintCopy: Boolean? = null,
    @SerialName("is_singleton")
    val isSingleton: Boolean,
    @SerialName("item_id")
    val itemId: Long,
    @SerialName("location_flag")
    val locationFlag: String,
    @SerialName("location_id")
    val locationId: Long,
    @SerialName("location_type")
    val locationType: AssetLocationType,
    @SerialName("quantity")
    val quantity: Int,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
enum class AssetLocationType {
    @SerialName("station")
    Station,

    @SerialName("solar_system")
    SolarSystem,

    @SerialName("item")
    Item,

    @SerialName("other")
    Other,
}

@Serializable
data class AssetName(
    @SerialName("item_id")
    val itemId: Long,
    @SerialName("name")
    val name: String,
)

@Serializable
data class AssetLocation(
    @SerialName("item_id")
    val itemId: Long,
    @SerialName("position")
    val position: AssetPosition,
)

@Serializable
data class AssetPosition(
    @SerialName("x")
    val x: Double,
    @SerialName("y")
    val y: Double,
    @SerialName("z")
    val z: Double,
)

@Serializable
data class MarketsPrice(
    @SerialName("adjusted_price")
    val adjustedPrice: Double? = null,
    @SerialName("average_price")
    val averagePrice: Double? = null,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
data class CharactersIdFleet(
    @SerialName("fleet_id")
    val id: Long,
    @SerialName("role")
    val role: FleetRole,
    @SerialName("squad_id")
    val squadId: Long,
    @SerialName("wing_id")
    val wingId: Long,
)

@Serializable
data class FleetsId(
    @SerialName("is_registered")
    val isRegistered: Boolean,
    @SerialName("motd")
    val motd: String,
)

@Serializable
data class FleetMember(
    @SerialName("character_id")
    val characterId: Int,
    @SerialName("join_time")
    val joinTime: String,
    @SerialName("role")
    val role: FleetRole,
    @SerialName("ship_type_id")
    val shipTypeId: Int,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("squad_id")
    val squadId: Long,
    @SerialName("station_id")
    val stationId: Long? = null,
    @SerialName("takes_fleet_warp")
    val takesFleetWarp: Boolean,
    @SerialName("wing_id")
    val wingId: Long,
)

@Serializable
enum class FleetRole {
    @SerialName("fleet_commander")
    FleetCommander,

    @SerialName("squad_commander")
    SquadCommander,

    @SerialName("squad_member")
    SquadMember,

    @SerialName("wing_commander")
    WingCommander,
}

@Serializable
data class CharactersIdPlanet(
    @SerialName("last_update")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastUpdate: Instant,
    @SerialName("num_pins")
    val numPins: Int,
    @SerialName("owner_id")
    val ownerId: Int,
    @SerialName("planet_id")
    val planetId: Int,
    @SerialName("planet_type")
    val planetType: PlanetType,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("upgrade_level")
    val upgradeLevel: Int,
)

@Serializable
enum class PlanetType {
    @SerialName("temperate")
    Temperate,

    @SerialName("barren")
    Barren,

    @SerialName("oceanic")
    Oceanic,

    @SerialName("ice")
    Ice,

    @SerialName("gas")
    Gas,

    @SerialName("lava")
    Lava,

    @SerialName("storm")
    Storm,

    @SerialName("plasma")
    Plasma,
}

@Serializable
data class CharactersIdPlanetsId(
    val links: List<PlanetaryLink>,
    val pins: List<PlanetaryPin>,
    val routes: List<PlanetaryRoute>,
)

@Serializable
data class PlanetaryLink(
    @SerialName("destination_pin_id")
    val destinationPinId: Long,
    @SerialName("link_level")
    val linkLevel: Int,
    @SerialName("source_pin_id")
    val sourcePinId: Long,
)

@Serializable
data class PlanetaryPin(
    @SerialName("contents")
    val contents: List<PlanetaryItem>? = null,
    @SerialName("expiry_time")
    @Serializable(with = IsoDateTimeSerializer::class)
    val expiryTime: Instant? = null,
    @SerialName("extractor_details")
    val extractor: PlanetaryExtractor? = null,
    @SerialName("factory_details")
    val factory: PlanetaryFactory? = null,
    @SerialName("install_time")
    @Serializable(with = IsoDateTimeSerializer::class)
    val installTime: Instant? = null,
    @SerialName("last_cycle_start")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastCycleStart: Instant? = null,
    @SerialName("latitude")
    val latitude: Float,
    @SerialName("longitude")
    val longitude: Float,
    @SerialName("pin_id")
    val pinId: Long,
    @SerialName("schematic_id")
    val schematicId: Int? = null,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
data class PlanetaryItem(
    @SerialName("amount")
    val amount: Long,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
data class PlanetaryExtractor(
    @SerialName("cycle_time")
    val cycleTime: Int? = null,
    @SerialName("head_radius")
    val headRadius: Float? = null,
    @SerialName("heads")
    val heads: List<PlanetaryExtractorHead>,
    @SerialName("product_type_id")
    val productTypeId: Int? = null,
    @SerialName("qty_per_cycle")
    val quantityPerCycle: Int? = null,
)

@Serializable
data class PlanetaryExtractorHead(
    @SerialName("head_id")
    val headId: Int,
    @SerialName("latitude")
    val latitude: Float,
    @SerialName("longitude")
    val longitude: Float,
)

@Serializable
data class PlanetaryFactory(
    @SerialName("schematic_id")
    val schematicId: Int,
)

@Serializable
data class PlanetaryRoute(
    @SerialName("content_type_id")
    val typeId: Int,
    @SerialName("destination_pin_id")
    val destinationPinId: Long,
    @SerialName("quantity")
    val quantity: Float,
    @SerialName("route_id")
    val routeId: Long,
    @SerialName("source_pin_id")
    val sourcePinId: Long,
    @SerialName("waypoints")
    val waypoints: List<Long>? = null,
)

@Serializable
data class IndustrySystem(
    @SerialName("cost_indices")
    val indices: List<IndustrySystemIndex>,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
)

@Serializable
data class IndustrySystemIndex(
    @SerialName("activity")
    val activity: IndustryActivity,
    @SerialName("cost_index")
    val costIndex: Float,
)

@Serializable
enum class IndustryActivity {
    @SerialName("copying")
    Copying,

    @SerialName("duplicating")
    Duplicating,

    @SerialName("invention")
    Invention,

    @SerialName("manufacturing")
    Manufacturing,

    @SerialName("none")
    None,

    @SerialName("reaction")
    Reaction,

    @SerialName("researching_material_efficiency")
    ResearchingMaterialEfficiency,

    @SerialName("researching_technology")
    ResearchingTechnology,

    @SerialName("researching_time_efficiency")
    ResearchingTimeEfficiency,

    @SerialName("reverse_engineering")
    ReverseEngineering,
}

@Serializable
data class CharactersIdRoles(
    @SerialName("roles")
    val roles: List<String>,
)

@Serializable
data class NewMailRequest(
    @SerialName("body")
    val body: String,
    @SerialName("recipients")
    val recipients: List<Long>,
    @SerialName("subject")
    val subject: String,
    @SerialName("to_corp_or_alliance_id")
    val corporationOrAllianceId: Int? = null,
    @SerialName("to_mailing_list_id")
    val mailingListId: Long? = null,
)

@Serializable
data class WalletJournalEntry(
    @SerialName("amount")
    val amount: Double? = null,
    @SerialName("balance")
    val balance: Double? = null,
    @SerialName("context_id")
    val contextId: Long? = null,
    @SerialName("context_id_type")
    val contextIdType: ContextIdType? = null,
    @SerialName("date")
    @Serializable(with = IsoDateTimeSerializer::class)
    val date: Instant,
    @SerialName("description")
    val description: String,
    @SerialName("id")
    val id: Long,
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("ref_type")
    val refType: String,
    @SerialName("first_party_id")
    val firstPartyId: Long? = null,
    @SerialName("second_party_id")
    val secondPartyId: Long? = null,
    @SerialName("tax")
    val tax: Double? = null,
    @SerialName("tax_receiver_id")
    val taxReceiverId: Long? = null,
)

@Serializable
enum class ContextIdType {
    @SerialName("structure_id")
    Structure,

    @SerialName("station_id")
    Station,

    @SerialName("market_transaction_id")
    MarketTransaction,

    @SerialName("character_id")
    Character,

    @SerialName("corporation_id")
    Corporation,

    @SerialName("alliance_id")
    Alliance,

    @SerialName("eve_system")
    EveSystem,

    @SerialName("industry_job_id")
    IndustryJob,

    @SerialName("contract_id")
    Contract,

    @SerialName("planet_id")
    Planet,

    @SerialName("system_id")
    System,

    @SerialName("type_id")
    Type,
}

@Serializable
data class WalletTransaction(
    @SerialName("client_id")
    val clientId: Long,
    @SerialName("date")
    @Serializable(with = IsoDateTimeSerializer::class)
    val date: Instant,
    @SerialName("is_buy")
    val isBuy: Boolean,
    @SerialName("is_personal")
    val isPersonal: Boolean? = null,
    @SerialName("journal_ref_id")
    val journalRefId: Long,
    @SerialName("location_id")
    val locationId: Long,
    @SerialName("quantity")
    val quantity: Long,
    @SerialName("transaction_id")
    val transactionId: Long,
    @SerialName("type_id")
    val typeId: Long,
    @SerialName("unit_price")
    val unitPrice: Double,
) : OffsetId {
    override val offsetId: Long get() = transactionId
}

@Serializable
data class CorporationWalletBalance(
    @SerialName("balance")
    val balance: Double,
    @SerialName("division")
    val divisionId: Int,
)

@Serializable
data class CorporationDivisions(
    @SerialName("hangar")
    val hangarDivisions: List<Division>? = null,
    @SerialName("wallet")
    val walletDivisions: List<Division>? = null,
)

@Serializable
data class Division(
    @SerialName("division")
    val id: Long? = null,
    @SerialName("name")
    val name: String? = null,
)

@Serializable
data class LoyaltyPoints(
    @SerialName("corporation_id")
    val corporationId: Long,
    @SerialName("loyalty_points")
    val loyaltyPoints: Long,
)
