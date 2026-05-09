package dev.nohus.rift.sso.authentication

import dev.nohus.rift.sso.SsoAuthority
import dev.nohus.rift.sso.authentication.Authentication.EveAuthentication
import dev.nohus.rift.sso.scopes.EsiScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Factory
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Factory
class SsoAuthenticator(
    private val ssoClient: SsoClient,
    private val eveSsoRepository: EveSsoRepository,
) {
    // One mutex per character to prevent concurrent refreshes for the same character
    private val refreshMutexes = mutableMapOf<Int, Mutex>()
    private val minValidityDuration = Duration.ofMinutes(1)

    /**
     * Starts the SSO flow, redirecting the user to the SSO login page.
     * Returns once the authentication flow has finished or failed
     */
    suspend fun authenticate(authority: SsoAuthority, scopes: List<String>) {
        val authentication = ssoClient.authenticate(authority, scopes.sorted())
        when (authority) {
            SsoAuthority.Eve -> eveSsoRepository.addAuthentication(authentication as EveAuthentication)
        }
        logger.info { "SSO authentication successful ($authority)" }
    }

    /**
     * Cancels an in progress SSO flow, if any
     */
    fun cancel() {
        ssoClient.cancel()
    }

    /**
     * Retrieves an access token, refreshing it first if needed, or null if there isn't one
     *
     * @param characterId Character ID
     * @param scope ESI scope the token has to be valid for, or null if no scopes are required. If the present
     * access token isn't valid for that scope, this method will throw.
     * @throws NoAuthenticationException When no valid token is available
     */
    suspend fun getValidEveAccessToken(characterId: Int, scope: EsiScope?): String {
        val authentication = eveSsoRepository.getAuthentication(characterId) ?: throw NoAuthenticationException(characterId, null)
        if (scope != null && scope.id !in authentication.scopes) {
            throw NoAuthenticationException(characterId, scope)
        }
        val expiresIn = Duration.between(Instant.now(), authentication.expiration)
        if (expiresIn > minValidityDuration) {
            return authentication.accessToken
        }

        // Token has expired, refresh it
        val mutex = synchronized(refreshMutexes) {
            refreshMutexes.getOrPut(characterId) { Mutex() }
        }
        return mutex.withLock {
            // Re-check after acquiring the lock in case another coroutine already refreshed
            val current = eveSsoRepository.getAuthentication(characterId) ?: throw NoAuthenticationException(characterId, null)
            if (scope != null && scope.id !in current.scopes) {
                throw NoAuthenticationException(characterId, scope)
            }
            if (current.expiration.isAfter(Instant.now())) {
                return@withLock current.accessToken
            }

            val newAuthentication = ssoClient.refreshToken(SsoAuthority.Eve, current) as EveAuthentication
            eveSsoRepository.addAuthentication(newAuthentication)
            logger.debug { "Eve SSO access token refreshed" }
            newAuthentication.accessToken
        }
    }
}
