package dev.nohus.rift.sso.authentication

import java.time.Instant

sealed class Authentication(
    open val accessToken: String,
    open val refreshToken: String,
    open val expiration: Instant,
    open val scopes: List<String>,
) {
    data class EveAuthentication(
        val characterId: Int,
        override val accessToken: String,
        override val refreshToken: String,
        override val expiration: Instant,
        override val scopes: List<String>,
    ) : Authentication(accessToken, refreshToken, expiration, scopes)
}
