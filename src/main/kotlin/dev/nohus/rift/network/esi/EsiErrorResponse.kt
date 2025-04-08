package dev.nohus.rift.network.esi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EsiErrorResponse(
    @SerialName("error")
    val error: String,
    @SerialName("sso_status")
    val ssoStatus: Int? = null,
)

data class EsiErrorException(
    val error: EsiErrorResponse,
    val code: Int,
) : Exception(error.error)
