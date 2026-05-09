package dev.nohus.rift.network.esi.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OpportunityState {
    @SerialName("Unspecified")
    Unspecified,

    @SerialName("Active")
    Active,

    @SerialName("Closed")
    Closed,

    @SerialName("Completed")
    Completed,

    @SerialName("Expired")
    Expired,

    @SerialName("Deleted")
    Deleted,
}
