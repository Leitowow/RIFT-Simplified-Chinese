package dev.nohus.rift.industry.cookbook

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}
private const val EVE_REF_BLUEPRINT_URL = "https://ref-data.everef.net/blueprints"

@Single
class BlueprintProductsRepository(
    @Named("api") private val okHttpClient: OkHttpClient,
    @Named("network") private val json: Json,
) {

    data class ManufacturedProduct(
        val typeId: Int,
        val quantity: Int,
    )

    private val productsByBlueprintId = ConcurrentHashMap<Int, ManufacturedProduct?>()

    suspend fun getManufacturedProduct(blueprintTypeId: Int): Result<ManufacturedProduct?> {
        productsByBlueprintId[blueprintTypeId]?.let { return Result.Success(it) }
        return withContext(Dispatchers.IO) {
            try {
            val request = Request.Builder()
                .url(
                    EVE_REF_BLUEPRINT_URL
                        .toHttpUrl()
                        .newBuilder()
                        .addPathSegment(blueprintTypeId.toString())
                        .build(),
                )
                .tag(Originator::class.java, Originator.PlanetaryIndustry)
                .tag(Endpoint::class.java, Endpoint.Raw)
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.Failure(IOException("Blueprint lookup failed: HTTP ${response.code}"))
                } else {
                    val payload = response.body.string()
                    val product = parseManufacturedProduct(payload)
                    productsByBlueprintId[blueprintTypeId] = product
                    Result.Success(product)
                }
            }
            } catch (e: IOException) {
                logger.warn(e) { "Could not load product for blueprint $blueprintTypeId" }
                Result.Failure(e)
            }
        }
    }

    private fun parseManufacturedProduct(payload: String): ManufacturedProduct? {
        return runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            val products = root["activities"]
                ?.jsonObject
                ?.get("manufacturing")
                ?.jsonObject
                ?.get("products")
                ?.jsonObject
                ?: return null
            val firstProduct = products.values.firstOrNull()?.jsonObject ?: return null
            ManufacturedProduct(
                typeId = firstProduct["type_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return null,
                quantity = firstProduct["quantity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
            )
        }.getOrNull()
    }
}
