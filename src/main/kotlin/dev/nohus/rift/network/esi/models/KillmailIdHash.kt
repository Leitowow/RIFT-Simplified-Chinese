package dev.nohus.rift.network.esi.models

import dev.nohus.rift.network.IsoDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class KillmailIdHash(
    @SerialName("attackers")
    val attackers: List<Attacker>,
    @SerialName("killmail_id")
    val killmailId: Long,
    @SerialName("killmail_time")
    @Serializable(with = IsoDateTimeSerializer::class)
    val killmailTime: Instant,
    @SerialName("moon_id")
    val moonId: Long? = null,
    @SerialName("solar_system_id")
    val solarSystemId: Long,
    @SerialName("victim")
    val victim: Victim,
    @SerialName("war_id")
    val warId: Long? = null,
)

@Serializable
data class Attacker(
    @SerialName("character_id")
    val characterId: Long? = null,
    @SerialName("corporation_id")
    val corporationId: Long? = null,
    @SerialName("alliance_id")
    val allianceId: Long? = null,
    @SerialName("damage_done")
    val damageDone: Long,
    @SerialName("faction_id")
    val factionId: Long? = null,
    @SerialName("final_blow")
    val isFinalBlow: Boolean,
    @SerialName("security_status")
    val securityStatus: Double,
    @SerialName("ship_type_id")
    val shipTypeId: Long? = null,
    @SerialName("weapon_type_id")
    val weaponTypeId: Long? = null,
)

@Serializable
data class Victim(
    @SerialName("character_id")
    val characterId: Long? = null,
    @SerialName("corporation_id")
    val corporationId: Long? = null,
    @SerialName("alliance_id")
    val allianceId: Long? = null,
    @SerialName("damage_taken")
    val damageTaken: Long,
    @SerialName("faction_id")
    val factionId: Long? = null,
    @SerialName("position")
    val position: Position? = null,
    @SerialName("ship_type_id")
    val shipTypeId: Long,
)

@Serializable
data class Position(
    @SerialName("x")
    val x: Double,
    @SerialName("y")
    val y: Double,
    @SerialName("z")
    val z: Double,
)
