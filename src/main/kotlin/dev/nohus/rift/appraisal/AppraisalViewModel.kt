package dev.nohus.rift.appraisal

import dev.nohus.rift.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Factory

@Factory
class AppraisalViewModel(
    private val janiceApiClient: JaniceApiClient,
) : ViewModel() {
    private val prettyJson = Json { prettyPrint = true }

    data class PriceSummary(
        val buy: Double,
        val split: Double,
        val sell: Double,
    )

    data class AppraisalItem(
        val name: String,
        val amount: Int,
        val buyTotal: Double,
        val splitTotal: Double,
        val sellTotal: Double,
    )

    data class AppraisalResult(
        val code: String?,
        val marketName: String,
        val pricing: String,
        val pricingVariant: String,
        val totalVolume: Double,
        val totalPackagedVolume: Double,
        val prices: PriceSummary,
        val items: List<AppraisalItem>,
        val failures: String,
    ) {
        val janiceUrl: String? get() = code?.let { "https://janice.e-351.com/a/$it" }
    }

    enum class Screen {
        Input,
        Result,
        ReprocessResult,
    }

    data class UiState(
        val screen: Screen = Screen.Input,
        val input: String = "",
        val pricePercentage: Int = 100,
        val isLoading: Boolean = false,
        val isReprocessLoading: Boolean = false,
        val warning: String? = null,
        val resultTitle: String = "",
        val resultBody: String = "",
        val appraisalResult: AppraisalResult? = null,
        val reprocessResultTitle: String = "",
        val reprocessResultBody: String = "",
        val reprocessAppraisalResult: AppraisalResult? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun onInputChanged(text: String) {
        _state.update { it.copy(input = text, warning = null) }
    }

    fun onPricePercentageChanged(value: Int) {
        val percentage = value.coerceIn(1, 200)
        _state.update { it.copy(pricePercentage = percentage, warning = null) }
    }

    fun onPricePercentageTextChanged(text: String) {
        val percentage = text
            .filter { it.isDigit() }
            .toIntOrNull()
            ?.coerceIn(1, 200)
            ?: return
        _state.update { it.copy(pricePercentage = percentage, warning = null) }
    }

    fun onBackClick() {
        _state.update {
            it.copy(
                screen = Screen.Input,
                warning = null,
                isReprocessLoading = false,
                appraisalResult = null,
                resultBody = "",
                resultTitle = "",
                reprocessAppraisalResult = null,
                reprocessResultBody = "",
                reprocessResultTitle = "",
            )
        }
    }

    fun onBackToPrimaryResultClick() {
        _state.update {
            if (it.screen == Screen.ReprocessResult) {
                it.copy(screen = Screen.Result, isReprocessLoading = false)
            } else {
                it
            }
        }
    }

    fun onEstimateClick() {
        val queryState = _state.value
        val input = queryState.input.trim()
        if (input.isEmpty()) {
            _state.update { it.copy(warning = "请输入要估价的物品内容") }
            return
        }

        val requestedPricePercentage = queryState.pricePercentage
        val pricePercentage = requestedPricePercentage / 100.0
        _state.update { it.copy(isLoading = true, warning = null) }
        viewModelScope.launch {
            val response = janiceApiClient.appraise(input, pricePercentage)
            response
                .onSuccess { result ->
                    val responseBody = prettifyJson(result.body)
                    if (result.statusCode in 200..299) {
                        val parsed = parseAppraisal(result.body)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                screen = Screen.Result,
                                resultTitle = "估价结果（${requestedPricePercentage}%）",
                                resultBody = responseBody,
                                appraisalResult = parsed,
                                isReprocessLoading = false,
                                reprocessAppraisalResult = null,
                                reprocessResultBody = "",
                                reprocessResultTitle = "",
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                screen = Screen.Result,
                                resultTitle = "估价失败（HTTP ${result.statusCode}）",
                                resultBody = responseBody,
                                appraisalResult = null,
                                isReprocessLoading = false,
                                reprocessAppraisalResult = null,
                                reprocessResultBody = "",
                                reprocessResultTitle = "",
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            screen = Screen.Result,
                            resultTitle = "估价请求失败",
                            resultBody = error.message ?: "未知错误",
                            appraisalResult = null,
                            isReprocessLoading = false,
                            reprocessAppraisalResult = null,
                            reprocessResultBody = "",
                            reprocessResultTitle = "",
                        )
                    }
                }
        }
    }

    fun onReprocessEstimateClick() {
        val queryState = _state.value
        if (queryState.isReprocessLoading) return
        val appraisalCode = queryState.appraisalResult?.code
        if (appraisalCode.isNullOrBlank()) return
        _state.update { it.copy(isReprocessLoading = true) }
        viewModelScope.launch {
            val response = janiceApiClient.reprocess(appraisalCode)
            response
                .onSuccess { result ->
                    val responseBody = prettifyJson(result.body)
                    val isRpcError = containsRpcError(result.body)
                    if (result.statusCode in 200..299 && !isRpcError) {
                        val parsed = parseAppraisal(result.body)
                        _state.update {
                            it.copy(
                                isReprocessLoading = false,
                                screen = Screen.ReprocessResult,
                                reprocessResultTitle = "化矿估价结果",
                                reprocessResultBody = responseBody,
                                reprocessAppraisalResult = parsed,
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isReprocessLoading = false,
                                screen = Screen.ReprocessResult,
                                reprocessResultTitle = if (isRpcError) "化矿估价失败（RPC）" else "化矿估价失败（HTTP ${result.statusCode}）",
                                reprocessResultBody = responseBody,
                                reprocessAppraisalResult = null,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isReprocessLoading = false,
                            screen = Screen.ReprocessResult,
                            reprocessResultTitle = "化矿估价请求失败",
                            reprocessResultBody = error.message ?: "未知错误",
                            reprocessAppraisalResult = null,
                        )
                    }
                }
        }
    }

    private fun prettifyJson(raw: String): String {
        return try {
            val element = Json.parseToJsonElement(raw)
            prettyJson.encodeToString(JsonElement.serializer(), element)
        } catch (_: Exception) {
            raw
        }
    }

    private fun parseAppraisal(raw: String): AppraisalResult? {
        return try {
            val parsed = Json.parseToJsonElement(raw).jsonObject
            val root = parsed.obj("result") ?: parsed
            val code = root.string("code")
            val marketName = localizeMarketName(root.obj("market")?.string("name") ?: root.obj("pricerMarket")?.string("name"))
            val pricing = root.string("pricing") ?: "-"
            val pricingVariant = root.string("pricingVariant") ?: "-"
            val totalVolume = root.number("totalVolume") ?: 0.0
            val totalPackagedVolume = root.number("totalPackagedVolume") ?: totalVolume
            val failures = root.string("failures").orEmpty()

            val effective = root.obj("effectivePrices")
            val prices = PriceSummary(
                buy = effective?.number("totalBuyPrice") ?: 0.0,
                split = effective?.number("totalSplitPrice") ?: 0.0,
                sell = effective?.number("totalSellPrice") ?: 0.0,
            )

            val items = root["items"]
                ?.jsonArray
                ?.mapNotNull { itemElement ->
                    val item = itemElement.jsonObject
                    val itemType = item.obj("itemType")
                    val effectivePrices = item.obj("effectivePrices")
                    val name = itemType?.string("name") ?: return@mapNotNull null
                    val amount = item.int("amount") ?: 0
                    val splitTotal = effectivePrices?.number("splitPriceTotal") ?: 0.0
                    val buyTotal = effectivePrices?.number("buyPriceTotal") ?: 0.0
                    val sellTotal = effectivePrices?.number("sellPriceTotal") ?: 0.0
                    AppraisalItem(
                        name = name,
                        amount = amount,
                        buyTotal = buyTotal,
                        splitTotal = splitTotal,
                        sellTotal = sellTotal,
                    )
                }
                ?: emptyList()

            AppraisalResult(
                code = code,
                marketName = marketName,
                pricing = pricing,
                pricingVariant = pricingVariant,
                totalVolume = totalVolume,
                totalPackagedVolume = totalPackagedVolume,
                prices = prices,
                items = items,
                failures = failures,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun containsRpcError(raw: String): Boolean {
        return try {
            Json.parseToJsonElement(raw).jsonObject["error"] != null
        } catch (_: Exception) {
            false
        }
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
    private fun JsonObject.number(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
    private fun JsonObject.obj(key: String): JsonObject? = this[key]?.jsonObject

    private fun localizeMarketName(marketName: String?): String {
        return when (marketName) {
            null -> "未知市场"
            "Jita 4-4" -> "吉他"
            else -> marketName
        }
    }
}
