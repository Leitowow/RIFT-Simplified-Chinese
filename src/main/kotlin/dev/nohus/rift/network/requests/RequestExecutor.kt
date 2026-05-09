package dev.nohus.rift.network.requests

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.esi.EsiErrorException
import dev.nohus.rift.network.esi.EsiErrorResponse
import dev.nohus.rift.sso.authentication.EveSsoRepository
import dev.nohus.rift.sso.authentication.SsoException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Headers
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private val logger = KotlinLogging.logger {}

/**
 * A deserialized response body and response headers
 */
data class Reply<T>(
    val body: T,
    val headers: Headers,
)

interface RequestExecutor {
    suspend fun <R : Any> execute(
        request: suspend () -> R,
    ): Result<R>

    suspend fun <R : Any> executeWithHeaders(
        request: suspend () -> Response<R>,
    ): Result<Reply<R>>

    fun setHttpErrorHandler(handler: (e: HttpException) -> Failure?)
}

open class RequestExecutorImpl(
    private val json: Json,
) : RequestExecutor {

    private var httpErrorHandler: ((e: HttpException) -> Failure?) = ::handleHttpError

    override suspend fun <R : Any> execute(
        request: suspend () -> R,
    ): Result<R> {
        return try {
            Success(withContext(Dispatchers.IO) { request() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override suspend fun <R : Any> executeWithHeaders(
        request: suspend () -> Response<R>,
    ): Result<Reply<R>> {
        return try {
            val response = withContext(Dispatchers.IO) { request() }
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Success(Reply(body, response.headers()))
                } else {
                    handleError(SerializationException("Response body was null"))
                }
            } else {
                handleError(HttpException(response))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override fun setHttpErrorHandler(handler: (e: HttpException) -> Failure?) {
        httpErrorHandler = handler
    }

    private fun handleError(e: Exception): Failure {
        when (e) {
            is SsoException -> logger.error { "Could not execute request due to SSO failure: $e" }
            is HttpException -> httpErrorHandler(e)?.let { return it }
            is IOException -> logger.error { "Could not execute request: $e" }
            is SerializationException -> logger.error(e) { "Unexpected API response" }
            else -> logger.error(e) { "Unknown API Error" }
        }
        return Failure(e)
    }

    /**
     * With ESI in mind, can be overridden for other APIs
     */
    private fun handleHttpError(e: HttpException): Failure? {
        if (e.code() == 401) {
            logger.error { "Unauthorized: ${e.message}" }
        } else {
            val body = e.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                try {
                    val errorResponse: EsiErrorResponse = json.decodeFromString(body)
                    if ("Forbidden" == errorResponse.error) {
                        logger.debug { "Forbidden API response" }
                    } else if ("unroutable" == errorResponse.error) {
                        logger.error { "ESI request could not be routed to Tranquility, the game is probably down" }
                    } else if ("Timeout contacting tranquility" == errorResponse.error) {
                        logger.error { "ESI request timed out contacting Tranquility, the game is probably down" }
                    } else if (e.code() == 404) {
                        // Valid response
                    } else {
                        val text = buildString {
                            append(errorResponse.error)
                            if (errorResponse.ssoStatus != null) {
                                append(" (SSO status: ${errorResponse.ssoStatus})")
                            }
                        }
                        logger.error { "ESI error response (${e.code()}): $text" }
                    }
                    return Failure(EsiErrorException(errorResponse, e.code()))
                } catch (_: SerializationException) {
                    logger.error { "API HTTP error: ${e.code()} (with unknown body) \"$body\"" }
                }
            } else {
                logger.error { "API HTTP error: ${e.code()}" }
            }
        }
        return null
    }
}
