package dev.nohus.rift.network.zkillboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecentActivity(
    @SerialName("characterID")
    val characterIds: List<Int>,
)
