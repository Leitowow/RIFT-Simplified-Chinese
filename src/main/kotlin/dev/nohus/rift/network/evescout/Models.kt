package dev.nohus.rift.network.evescout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Observation(
    @SerialName("id")
    val id: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("observation_type")
    val observationType: ObservationType,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("system_id")
    val systemId: Int,
)

enum class ObservationType {
    @SerialName("electric_a")
    ElectricA,

    @SerialName("electric_b")
    ElectricB,

    @SerialName("exotic_a")
    ExoticA,

    @SerialName("exotic_b")
    ExoticB,

    @SerialName("gamma_a")
    GammaA,

    @SerialName("gamma_b")
    GammaB,

    @SerialName("plasma_a")
    PlasmaA,

    @SerialName("plasma_b")
    PlasmaB,

    @SerialName("toms_shuttle")
    TomsShuttle,
}

@Serializable
data class Signature(
    @SerialName("wh_exits_outward")
    val whExitsOutward: Boolean? = null,
    @SerialName("wh_type")
    val whType: String? = null,
    @SerialName("max_ship_size")
    val maxShipSize: MaxShipSize? = null,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("out_system_id")
    val outSystemId: Int,
    @SerialName("out_system_name")
    val outSystemName: String,
    @SerialName("out_signature")
    val outSignature: String,
    @SerialName("in_system_id")
    val inSystemId: Int? = null,
    @SerialName("in_system_name")
    val inSystemName: String? = null,
    @SerialName("in_region_name")
    val inRegionName: String? = null,
    @SerialName("in_signature")
    val inSignature: String? = null,
)

enum class MaxShipSize {
    @SerialName("small")
    Small,

    @SerialName("medium")
    Medium,

    @SerialName("large")
    Large,

    @SerialName("xlarge")
    XLarge,

    @SerialName("capital")
    Capital,

    @SerialName("unknown")
    Unknown,
}
