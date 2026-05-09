package dev.nohus.rift.network.interceptors

import dev.nohus.rift.network.esi.EsiErrorResponse
import dev.nohus.rift.network.requests.Character
import dev.nohus.rift.network.requests.Scope
import dev.nohus.rift.sso.authentication.EveSsoRepository
import dev.nohus.rift.sso.authentication.NoAuthenticationException
import dev.nohus.rift.sso.authentication.SsoAuthenticator
import dev.nohus.rift.sso.authentication.SsoException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Invocation

private val logger = KotlinLogging.logger {}

@Single
class EsiAuthorizationInterceptor(
    private val ssoAuthenticator: SsoAuthenticator,
    private val eveSsoRepository: EveSsoRepository,
    @Named("network") private val json: Json,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()

        val character = request.tag(Character::class.java)
        val invocation = request.tag(Invocation::class.java)
        val scopeAnnotation = invocation?.method()?.getAnnotation(Scope::class.java)
        val scope = scopeAnnotation?.value?.objectInstance

        val newRequest = if (character != null) {
            val accessToken = try {
                ssoAuthenticator.getValidEveAccessToken(character.id, scope)
            } catch (e: NoAuthenticationException) {
                return@runBlocking createSyntheticFailure(request, e)
            } catch (e: SsoException) {
                return@runBlocking createSyntheticFailure(request, e)
            }
            request.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            request
        }

        val response = chain.proceed(newRequest)
        if (character != null && !response.isSuccessful) {
            val body = response.peekBody(Long.MAX_VALUE).string()
            try {
                val errorResponse: EsiErrorResponse = json.decodeFromString(body)
                if ("Token is not valid" in errorResponse.error) {
                    logger.error { "Character ${character.id} has an invalid token, removing" }
                    eveSsoRepository.removeAuthentication(character.id)
                } else if ("Character has been deleted" in errorResponse.error) {
                    logger.error { "Character ${character.id} has been deleted from the game, removing" }
                    eveSsoRepository.removeAuthentication(character.id)
                }
            } catch (_: SerializationException) {
                // Authentication errors come as EsiErrorResponse, so this error is not relevant
            }
        }
        response
    }

    private fun createSyntheticFailure(request: Request, exception: NoAuthenticationException): Response {
        val message = if (exception.scope != null) {
            "Could not execute request because the character ${exception.characterId} does not allow scope \"${exception.scope.id}\""
        } else {
            "Could not execute request because the character ${exception.characterId} is not authenticated"
        }
        return createSyntheticFailure(request, message)
    }

    private fun createSyntheticFailure(request: Request, exception: SsoException): Response {
        val message = exception.errorResponse?.errorDescription ?: exception.message ?: "Unknown error"
        return createSyntheticFailure(request, message)
    }

    private fun createSyntheticFailure(request: Request, text: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message(text)
            .body(text.toResponseBody(null))
            .build()
    }
}
