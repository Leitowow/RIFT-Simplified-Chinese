package dev.nohus.rift.industry.cookbook

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.Originator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.IOException
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}
private const val EVE_COOKBOOK_API_URL = "https://evecookbook.com/api/buildCost"

@Single
class EveCookbookApi(
    @Named("api") private val okHttpClient: OkHttpClient,
    @Named("network") private val json: Json,
) {

    data class BuildCostRequest(
        val blueprintTypeIds: List<Int>,
        val quantity: Int = 1,
        val priceMode: String = "buy",
        val additionalCosts: Double = 0.0,
        val baseMe: Int = 0,
        val componentsMe: Int = 0,
        val system: String = "Jita",
        val facilityTax: Double = 0.0,
        val industryStructureType: String = "Station",
        val industryRig: String = "None",
        val reactionStructureType: String = "Athanor",
        val reactionRig: String = "None",
        val includeReactionJobs: Boolean = false,
        val blueprintVersion: String = "tq",
    )

    data class BuildCostResponse(
        val statusCode: Int,
        val body: String,
        val errorFlag: Int? = null,
        val message: String? = null,
    )

    suspend fun getBuildCost(request: BuildCostRequest): Result<List<BuildCostResponse>> {
        if (request.blueprintTypeIds.isEmpty()) return Result.Failure(IllegalArgumentException("Missing blueprintTypeIds"))
        val chunks = request.blueprintTypeIds.distinct().chunked(20)
        val responses = mutableListOf<BuildCostResponse>()
        for (chunk in chunks) {
            val chunkRequest = request.copy(blueprintTypeIds = chunk)
            when (val result = executeWithRetry(chunkRequest)) {
                is Result.Success -> responses += result.data
                is Result.Failure -> return result
            }
        }
        return Result.Success(responses)
    }

    private suspend fun executeWithRetry(request: BuildCostRequest): Result<BuildCostResponse> = withContext(Dispatchers.IO) {
        val retryDelaysMs = listOf(0L, 500L, 1_000L, 2_000L)
        for ((attempt, delayMs) in retryDelaysMs.withIndex()) {
            if (delayMs > 0) delay(delayMs)
            val response = executeOnce(request)
            when (response) {
                is Result.Success -> {
                    val code = response.data.statusCode
                    if (code == 429 && attempt < retryDelaysMs.lastIndex) {
                        logger.warn { "EVE Cookbook API rate-limited (429), retrying attempt ${attempt + 1}" }
                        continue
                    }
                    return@withContext response
                }

                is Result.Failure -> {
                    if (attempt < retryDelaysMs.lastIndex) continue
                    return@withContext response
                }
            }
        }
        Result.Failure(IOException("Unknown request failure"))
    }

    private fun executeOnce(request: BuildCostRequest): Result<BuildCostResponse> {
        return try {
            val urlBuilder = EVE_COOKBOOK_API_URL.toHttpUrl().newBuilder()
            request.blueprintTypeIds.forEach { blueprintTypeId ->
                urlBuilder.addQueryParameter("blueprintTypeId", blueprintTypeId.toString())
            }
            urlBuilder
                .addQueryParameter("quantity", request.quantity.toString())
                .addQueryParameter("priceMode", request.priceMode)
                .addQueryParameter("additionalCosts", request.additionalCosts.toApiDecimal())
                .addQueryParameter("baseMe", request.baseMe.toString())
                .addQueryParameter("componentsMe", request.componentsMe.toString())
                .addQueryParameter("system", request.system)
                .addQueryParameter("facilityTax", request.facilityTax.toApiDecimal())
                .addQueryParameter("industryStructureType", request.industryStructureType)
                .addQueryParameter("industryRig", request.industryRig)
                .addQueryParameter("reactionStructureType", request.reactionStructureType)
                .addQueryParameter("reactionRig", request.reactionRig)
                .addQueryParameter("reactionFlag", if (request.includeReactionJobs) "Yes" else "")
                .addQueryParameter("blueprintVersion", request.blueprintVersion)

            val requestBuilder = Request.Builder()
                .url(urlBuilder.build())
                .tag(Originator::class.java, Originator.PlanetaryIndustry)
                .tag(Endpoint::class.java, Endpoint.Raw)
                .get()
                .build()

            okHttpClient.newCall(requestBuilder).execute().use { response ->
                val payload = response.body.string()
                val parsed = parseResponseSummary(payload)
                Result.Success(
                    BuildCostResponse(
                        statusCode = response.code,
                        body = payload,
                        errorFlag = parsed.first,
                        message = parsed.second,
                    ),
                )
            }
        } catch (e: IOException) {
            logger.error(e) { "EVE Cookbook request failed" }
            Result.Failure(e)
        }
    }

    private fun parseResponseSummary(payload: String): Pair<Int?, String?> {
        return try {
            val obj = json.parseToJsonElement(payload).jsonObject
            val error = obj["error"]?.toString()?.trim('"')?.toIntOrNull()
            val message = (obj["message"] as? JsonPrimitive)
                ?.takeUnless { it.isString.not() }
                ?.content
            error to message
        } catch (_: Exception) {
            null to null
        }
    }

    private fun Double.toApiDecimal(): String {
        return BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
    }
}
