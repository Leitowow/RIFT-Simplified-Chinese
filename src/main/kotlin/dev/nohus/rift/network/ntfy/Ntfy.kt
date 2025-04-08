package dev.nohus.rift.network.ntfy

import kotlinx.serialization.Serializable

@Serializable
data class Ntfy(
    val topic: String,
    val title: String,
    val message: String,
    val icon: String?,
)
