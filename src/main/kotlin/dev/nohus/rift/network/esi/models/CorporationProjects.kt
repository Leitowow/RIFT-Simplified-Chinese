package dev.nohus.rift.network.esi.models

import dev.nohus.rift.network.IsoDateTimeSerializer
import dev.nohus.rift.network.esi.pagination.CursorPaginated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
enum class CorporationProjectsQueryState {
    @SerialName("All")
    All,

    @SerialName("Active")
    Active,
}

@Serializable
class CorporationsIdProjects(
    @SerialName("projects")
    override val items: List<CorporationProject>,
) : CursorPaginated<CorporationProject>()

@Serializable
data class CorporationProject(
    @SerialName("id")
    val id: String,

    @SerialName("last_modified")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastModified: Instant,

    @SerialName("name")
    val name: String,

    @SerialName("progress")
    val progress: CorporationProjectProgress,

    @SerialName("reward")
    val reward: CorporationProjectReward? = null,

    @SerialName("state")
    val state: OpportunityState,
)

@Serializable
data class CorporationsIdProjectsId(
    @SerialName("configuration")
    val configuration: CorporationProjectConfiguration,

    @SerialName("contribution")
    val contributionSettings: CorporationProjectContributionSettings? = null,

    @SerialName("creator")
    val creator: CorporationProjectCreator,

    @SerialName("details")
    val details: CorporationProjectDetails,

    @SerialName("id")
    val id: String,

    @SerialName("last_modified")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastModified: Instant,

    @SerialName("name")
    val name: String,

    @SerialName("progress")
    val progress: CorporationProjectProgress,

    @SerialName("reward")
    val reward: CorporationProjectReward? = null,

    @SerialName("state")
    val state: OpportunityState,
)

@Serializable
data class CorporationProjectProgress(
    @SerialName("current")
    val current: Long,

    @SerialName("desired")
    val desired: Long,
)

@Serializable
data class CorporationProjectReward(
    @SerialName("initial")
    val initial: Double,

    @SerialName("remaining")
    val remaining: Double,
)

@Serializable
data class CorporationsIdProjectsIdContribution(
    @SerialName("last_modified")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastModified: Instant? = null,

    @SerialName("contributed")
    val contributed: Long,
)

@Serializable
data class CorporationsIdProjectsIdContributors(
    @SerialName("contributors")
    override val items: List<Contributor>,
) : CursorPaginated<Contributor>()

@Serializable
data class Contributor(
    @SerialName("id")
    val id: Long,

    @SerialName("name")
    val name: String,

    @SerialName("contributed")
    val contributed: Long,
)

@Serializable
data class CorporationProjectConfiguration(
    @SerialName("capture_fw_complex")
    val captureFwComplex: CaptureFwComplex? = null,

    @SerialName("damage_ship")
    val damageShip: DamageShip? = null,

    @SerialName("defend_fw_complex")
    val defendFwComplex: DefendFwComplex? = null,

    @SerialName("deliver_item")
    val deliverItem: DeliverItem? = null,

    @SerialName("destroy_npc")
    val destroyNpc: DestroyNpc? = null,

    @SerialName("destroy_ship")
    val destroyShip: DestroyShip? = null,

    @SerialName("earn_loyalty_point")
    val earnLoyaltyPoint: EarnLoyaltyPoint? = null,

    @SerialName("ship_insurance")
    val shipInsurance: ShipInsurance? = null,

    @SerialName("lost_ship")
    val lostShip: LostShip? = null,

    @SerialName("manual")
    val manual: Unit? = null,

    @SerialName("manufacture_item")
    val manufactureItem: ManufactureItem? = null,

    @SerialName("mine_material")
    val mineMaterial: MineMaterial? = null,

    @SerialName("remote_boost_shield")
    val remoteBoostShield: RemoteBoostShield? = null,

    @SerialName("remote_repair_armor")
    val remoteRepairArmor: RemoteRepairArmor? = null,

    @SerialName("salvage_wreck")
    val salvageWreck: SalvageWreck? = null,

    @SerialName("scan_signature")
    val scanSignature: ScanSignature? = null,

    @SerialName("unknown")
    val unknown: Unknown? = null,
)

@Serializable
data class Unknown(
    @SerialName("type")
    val type: String,
)

@Serializable
data class ScanSignature(
    @SerialName("locations")
    val locations: List<Location>? = null,

    @SerialName("signatures")
    val signatures: List<SignatureTypeId>? = null,
)

@Serializable
data class SignatureTypeId(
    @SerialName("signature_type_id")
    val signatureTypeId: Long,
)

@Serializable
data class SalvageWreck(
    @SerialName("locations")
    val locations: List<Location>? = null,
)

@Serializable
data class RemoteRepairArmor(
    @SerialName("identities")
    val identities: List<Identity>? = null,

    @SerialName("locations")
    val locations: List<Location>? = null,

    @SerialName("ships")
    val ships: List<Item>? = null,
)

@Serializable
data class RemoteBoostShield(
    @SerialName("identities")
    val identities: List<Identity>? = null,

    @SerialName("locations")
    val locations: List<Location>? = null,

    @SerialName("ships")
    val ships: List<Item>? = null,
)

@Serializable
data class MineMaterial(
    @SerialName("locations")
    val locations: List<Location>? = null,

    @SerialName("materials")
    val materials: List<Item>? = null,
)

@Serializable
data class ManufactureItem(
    @SerialName("docking_locations")
    val dockingLocations: List<DockableLocation>? = null,

    @SerialName("items")
    val items: List<Item>? = null,

    @SerialName("owner")
    val owner: OwnerType,
)

@Serializable
enum class OwnerType {
    @SerialName("Any")
    Any,

    @SerialName("Corporation")
    Corporation,

    @SerialName("Character")
    Character,
}

@Serializable
data class LostShip(
    @SerialName("identities")
    val identities: List<Identity>? = null,

    @SerialName("locations")
    val locations: List<Location>? = null,

    @SerialName("ships")
    val ships: List<Item>? = null,
)

@Serializable
data class ShipInsurance(
    @SerialName("conflict_type")
    val conflictType: ConflictType,

    @SerialName("identities")
    val identities: List<Identity>? = null,

    @SerialName("locations")
    val locations: List<Location>? = null,

    @SerialName("reimburse_implants")
    val reimburseImplants: Boolean,

    @SerialName("ships")
    val ships: List<Item>? = null,
)

@Serializable
enum class ConflictType {
    @SerialName("Any")
    Any,

    @SerialName("Pvp")
    Pvp,

    @SerialName("Pve")
    Pve,
}

@Serializable
data class EarnLoyaltyPoint(
    @SerialName("corporations")
    val corporations: List<CorporationId>? = null,
)

@Serializable
data class CorporationId(
    @SerialName("corporation_id")
    val corporationId: Long,
)

@Serializable
data class DestroyShip(
    @SerialName("identities")
    val identities: List<Identity>? = null,

    @SerialName("locations")
    val locations: List<Location>? = null,

    @SerialName("ships")
    val ships: List<Item>? = null,
)

@Serializable
data class DestroyNpc(
    @SerialName("locations")
    val locations: List<Location>? = null,
)

@Serializable
data class DeliverItem(
    @SerialName("docking_locations")
    val dockingLocations: List<DockableLocation>? = null,

    @SerialName("items")
    val items: List<Item>? = null,

    @SerialName("office_id")
    val officeId: Long? = null,
)

@Serializable
data class DamageShip(
    @SerialName("identities")
    val identities: List<Identity>? = null,

    @SerialName("locations")
    val locations: List<Location>? = null,

    @SerialName("ships")
    val ships: List<Item>? = null,
)

@Serializable
data class DefendFwComplex(
    @SerialName("archetypes")
    val archetypes: List<Archetype>? = null,

    @SerialName("factions")
    val factions: List<Faction>? = null,

    @SerialName("locations")
    val locations: List<Location>? = null,
)

@Serializable
data class CaptureFwComplex(
    @SerialName("archetypes")
    val archetypes: List<Archetype>? = null,

    @SerialName("factions")
    val factions: List<Faction>? = null,

    @SerialName("locations")
    val locations: List<Location>? = null,
)

@Serializable
data class Item(
    @SerialName("type_id")
    val typeId: Long? = null,

    @SerialName("group_id")
    val groupId: Long? = null,
)

@Serializable
data class DockableLocation(
    @SerialName("structure_id")
    val structureId: Long? = null,

    @SerialName("station_id")
    val stationId: Long? = null,
)

@Serializable
data class Identity(
    @SerialName("character_id")
    val characterId: Long? = null,

    @SerialName("corporation_id")
    val corporationId: Long? = null,

    @SerialName("alliance_id")
    val allianceId: Long? = null,

    @SerialName("faction_id")
    val factionId: Long? = null,
)

@Serializable
data class Archetype(
    @SerialName("archetype_id")
    val archetypeId: Long,
)

@Serializable
data class Faction(
    @SerialName("faction_id")
    val factionId: Long,
)

@Serializable
data class Location(
    @SerialName("solar_system_id")
    val solarSystemId: Long? = null,

    @SerialName("constellation_id")
    val constellationId: Long? = null,

    @SerialName("region_id")
    val regionId: Long? = null,
)

@Serializable
data class CorporationProjectContributionSettings(
    @SerialName("participation_limit")
    val participationLimit: Long? = null,

    @SerialName("reward_per_contribution")
    val rewardPerContribution: Double? = null,

    @SerialName("submission_limit")
    val submissionLimit: Long? = null,

    @SerialName("submission_multiplier")
    val submissionMultiplier: Double? = null,
)

@Serializable
data class CorporationProjectCreator(
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    val name: String,
)

@Serializable
data class CorporationProjectDetails(
    @SerialName("career")
    val career: OpportunityCareer,

    @SerialName("created")
    @Serializable(with = IsoDateTimeSerializer::class)
    val created: Instant,

    @SerialName("description")
    val description: String,

    @SerialName("expires")
    @Serializable(with = IsoDateTimeSerializer::class)
    val expires: Instant? = null,

    @SerialName("finished")
    @Serializable(with = IsoDateTimeSerializer::class)
    val finished: Instant? = null,
)

@Serializable
enum class OpportunityCareer {
    @SerialName("Unspecified")
    Unspecified,

    @SerialName("Explorer")
    Explorer,

    @SerialName("Industrialist")
    Industrialist,

    @SerialName("Enforcer")
    Enforcer,

    @SerialName("Soldier of Fortune")
    SoldierOfFortune,
}
