package dev.nohus.rift.appraisal

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.Originator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.IOException
import java.util.Base64

private val logger = KotlinLogging.logger {}

private const val JANICE_APPRAISAL_URL = "https://janice.e-351.com/api/rest/v2/appraisal"
private const val JANICE_RPC_URL = "https://janice.e-351.com/api/rpc/v1"
private const val JANICE_API_KEY_OBFUSCATED = "QCIBBUA6IhQqJw4rDSUubwg4KC85JDBQPiZuBQwgXwA="
private const val JANICE_API_KEY_MASK = "rift-janice"
private val JANICE_API_KEY: String by lazy { decodeObfuscatedApiKey() }

private fun decodeObfuscatedApiKey(): String {
    val decoded = Base64.getDecoder().decode(JANICE_API_KEY_OBFUSCATED)
    val unmasked = decoded.mapIndexed { index, byte ->
        val maskByte = JANICE_API_KEY_MASK[index % JANICE_API_KEY_MASK.length].code
        (byte.toInt() xor maskByte).toByte()
    }
    return unmasked.toByteArray().toString(Charsets.UTF_8)
}

data class JaniceAppraisalResponse(
    val statusCode: Int,
    val body: String,
)

@Single
class JaniceApiClient(
    @Named("api") private val okHttpClient: OkHttpClient,
) {

    suspend fun appraise(
        input: String,
        pricePercentage: Double,
        pricing: String? = null,
    ): Result<JaniceAppraisalResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val requestBody = input.toRequestBody("text/plain; charset=utf-8".toMediaType())
            val requestUrlBuilder = JANICE_APPRAISAL_URL
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("pricePercentage", pricePercentage.toString())
            if (pricing != null) {
                requestUrlBuilder.addQueryParameter("pricing", pricing)
            }
            val requestUrl = requestUrlBuilder.build()
            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .tag(Originator::class.java, Originator.Pings)
                .tag(Endpoint::class.java, Endpoint.Raw)
                .header("X-ApiKey", JANICE_API_KEY)
                .header("Accept", "application/json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val payload = response.body.string()
                Result.success(JaniceAppraisalResponse(response.code, payload))
            }
        } catch (e: IOException) {
            logger.error(e) { "Janice appraisal request failed" }
            Result.failure(e)
        }
    }

    suspend fun reprocess(code: String): Result<JaniceAppraisalResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val requestBody = """
                {
                  "id": 1,
                  "method": "Appraisal.reprocess",
                  "params": {
                    "code": "$code",
                    "oreEfficiency": 0.55,
                    "gasEfficiency": 0.55,
                    "scrapEfficiency": 0.55
                  }
                }
            """.trimIndent()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val requestUrl = JANICE_RPC_URL
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("m", "Appraisal.reprocess")
                .build()
            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .tag(Originator::class.java, Originator.Pings)
                .tag(Endpoint::class.java, Endpoint.Raw)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val payload = response.body.string()
                Result.success(JaniceAppraisalResponse(response.code, payload))
            }
        } catch (e: IOException) {
            logger.error(e) { "Janice reprocess request failed" }
            Result.failure(e)
        }
    }
}
