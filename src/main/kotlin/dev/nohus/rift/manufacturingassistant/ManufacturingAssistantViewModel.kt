package dev.nohus.rift.manufacturingassistant

import dev.nohus.rift.ViewModel
import dev.nohus.rift.appraisal.JaniceApiClient
import dev.nohus.rift.industry.cookbook.BlueprintProductsRepository
import dev.nohus.rift.industry.cookbook.BlueprintNameResolver
import dev.nohus.rift.industry.cookbook.EveCookbookApi
import dev.nohus.rift.network.Result
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.settings.persistence.ManufacturingAssistant as ManufacturingAssistantSettings
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Factory
import kotlin.math.pow

@Factory
class ManufacturingAssistantViewModel(
    private val blueprintNameResolver: BlueprintNameResolver,
    private val eveCookbookApi: EveCookbookApi,
    private val blueprintProductsRepository: BlueprintProductsRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
    private val janiceApiClient: JaniceApiClient,
    private val settings: Settings,
) : ViewModel() {

    enum class PriceMode(val apiValue: String, val label: String) {
        Buy("buy", "买单价"),
        Sell("sell", "卖单价"),
    }

    enum class IndustryStructureType(val apiValue: String, val label: String) {
        Station("Station", "空间站"),
        Raitaru("Raitaru", "莱塔卢"),
        Azbel("Azbel", "阿兹贝尔"),
        Sotiyo("Sotiyo", "索迪约"),
    }

    enum class IndustryRigType(val apiValue: String, val label: String) {
        None("None", "无"),
        T1("T1", "T1 工业改装件"),
        T2("T2", "T2 工业改装件"),
    }

    enum class ReactionStructureType(val apiValue: String, val label: String) {
        Athanor("Athanor", "阿塔诺"),
        Tatara("Tatara", "塔塔拉"),
    }

    enum class ReactionRigType(val apiValue: String, val label: String) {
        None("None", "无"),
        T1("T1", "T1 反应改装件"),
        T2("T2", "T2 反应改装件"),
    }

    data class SuggestionItem(
        val label: String,
        val match: BlueprintNameResolver.Match,
    )

    data class SummaryField(
        val label: String,
        val value: String,
    )

    private data class StructuredBuildCostResult(
        val summaryFields: List<SummaryField>,
        val preferredTitle: String? = null,
        val warning: String? = null,
        val totalCost: Double? = null,
        val producedQuantity: Int? = null,
        val blueprintTypeId: Int? = null,
    )

    private data class AppraisalPrices(
        val marketName: String?,
        val buyTotal: Double,
        val sellTotal: Double,
    )

    data class UiState(
        val input: String = "",
        val quantity: String = "1",
        val additionalCosts: String = "0",
        val baseMe: String = "10",
        val componentsMe: String = "10",
        val system: String = "C-J6MT",
        val systemSuggestions: List<String> = emptyList(),
        val facilityTax: String = "1",
        val priceMode: PriceMode = PriceMode.Buy,
        val includeReactionJobs: Boolean = true,
        val industryStructureType: IndustryStructureType = IndustryStructureType.Sotiyo,
        val industryRigType: IndustryRigType = IndustryRigType.T2,
        val reactionStructureType: ReactionStructureType = ReactionStructureType.Tatara,
        val reactionRigType: ReactionRigType = ReactionRigType.T2,
        val isParametersExpanded: Boolean = true,
        val suggestions: List<SuggestionItem> = emptyList(),
        val isLoading: Boolean = false,
        val warning: String? = null,
        val selectedBlueprint: BlueprintNameResolver.Match? = null,
        val resultTitle: String = "",
        val summaryFields: List<SummaryField> = emptyList(),
    )

    private val _state = MutableStateFlow(getInitialState())
    val state = _state.asStateFlow()

    fun onInputChanged(input: String) {
        val matches = blueprintNameResolver.search(input)
        val preferChinese = isChineseInput(input)
        val suggestions = matches.map { match ->
            SuggestionItem(
                label = getSuggestionLabel(match, preferChinese),
                match = match,
            )
        }
        _state.update {
            it.copy(
                input = input,
                suggestions = suggestions,
                warning = null,
                selectedBlueprint = suggestions.firstOrNull { suggestion ->
                    suggestion.label.equals(input, ignoreCase = true) ||
                        suggestion.match.enName.equals(input, ignoreCase = true) ||
                        suggestion.match.zhName?.equals(input, ignoreCase = true) == true
                }?.match ?: it.selectedBlueprint,
            )
        }
    }

    fun onSuggestionConfirmed(displayName: String) {
        val selected = _state.value.suggestions
            .firstOrNull { suggestion -> suggestion.label.equals(displayName, ignoreCase = true) }
            ?.match
            ?: blueprintNameResolver.search(displayName, maxResults = 20)
                .firstOrNull {
                    it.enName.equals(displayName, ignoreCase = true) ||
                        it.zhName?.equals(displayName, ignoreCase = true) == true
                }
            ?: blueprintNameResolver.search(displayName, maxResults = 1).firstOrNull()
        _state.update {
            it.copy(
                input = selected?.let { match ->
                    getSuggestionLabel(match, isChineseInput(displayName))
                } ?: displayName,
                selectedBlueprint = selected,
                warning = null,
            )
        }
    }

    fun onQuantityChanged(quantity: String) {
        val sanitized = quantity.filter { char -> char.isDigit() }.take(5)
        _state.update { it.copy(quantity = sanitized.ifBlank { "" }, warning = null) }
        saveParameters()
    }

    fun onAdditionalCostsChanged(value: String) {
        val sanitized = sanitizeDecimalInput(value = value, maxIntegerDigits = 18)
        _state.update { it.copy(additionalCosts = sanitized.ifBlank { "" }, warning = null) }
        saveParameters()
    }

    fun onBaseMeChanged(value: String) {
        val sanitized = value.filter { char -> char.isDigit() }.take(2)
        _state.update { it.copy(baseMe = sanitized.ifBlank { "" }, warning = null) }
        saveParameters()
    }

    fun onComponentsMeChanged(value: String) {
        val sanitized = value.filter { char -> char.isDigit() }.take(2)
        _state.update { it.copy(componentsMe = sanitized.ifBlank { "" }, warning = null) }
        saveParameters()
    }

    fun onSystemChanged(value: String) {
        _state.update {
            it.copy(
                system = value,
                systemSuggestions = getSystemSuggestions(value),
                warning = null,
            )
        }
        saveParameters()
    }

    fun onSystemSuggestionConfirmed(value: String) {
        _state.update {
            it.copy(
                system = value,
                systemSuggestions = emptyList(),
                warning = null,
            )
        }
        saveParameters()
    }

    fun onFacilityTaxChanged(value: String) {
        val sanitized = sanitizeDecimalInput(value = value, maxIntegerDigits = 3)
        _state.update { it.copy(facilityTax = sanitized.ifBlank { "" }, warning = null) }
        saveParameters()
    }

    fun onPriceModeChanged(value: PriceMode) {
        _state.update { it.copy(priceMode = value, warning = null) }
        saveParameters()
    }

    fun onIncludeReactionJobsChanged(value: Boolean) {
        _state.update { it.copy(includeReactionJobs = value, warning = null) }
        saveParameters()
    }

    fun onIndustryStructureTypeChanged(value: IndustryStructureType) {
        _state.update { it.copy(industryStructureType = value, warning = null) }
        saveParameters()
    }

    fun onIndustryRigTypeChanged(value: IndustryRigType) {
        _state.update { it.copy(industryRigType = value, warning = null) }
        saveParameters()
    }

    fun onReactionStructureTypeChanged(value: ReactionStructureType) {
        _state.update { it.copy(reactionStructureType = value, warning = null) }
        saveParameters()
    }

    fun onReactionRigTypeChanged(value: ReactionRigType) {
        _state.update { it.copy(reactionRigType = value, warning = null) }
        saveParameters()
    }

    fun onParametersExpandedChange(isExpanded: Boolean) {
        _state.update { it.copy(isParametersExpanded = isExpanded) }
    }

    fun onQueryClick() {
        val state = _state.value
        val quantity = state.quantity.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            _state.update { it.copy(warning = "“数量”必须为大于 0 的整数") }
            return
        }
        val baseMe = state.baseMe.toIntOrNull()
        if (baseMe == null || baseMe !in 0..10) {
            _state.update { it.copy(warning = "“主蓝图材料效率”必须为 0-10 的整数") }
            return
        }
        val componentsMe = state.componentsMe.toIntOrNull()
        if (componentsMe == null || componentsMe !in 0..10) {
            _state.update { it.copy(warning = "“组件材料效率”必须为 0-10 的整数") }
            return
        }
        if (!isValidDecimalInRange(input = state.facilityTax, min = 0.0, max = 100.0, maxScale = 2)) {
            _state.update { it.copy(warning = "“建筑税率”必须为 0-100，且最多两位小数") }
            return
        }
        val facilityTax = state.facilityTax.toDoubleOrNull() ?: run {
            _state.update { it.copy(warning = "“建筑税率”格式不正确") }
            return
        }
        if (!isValidDecimalInRange(input = state.additionalCosts, min = 0.0, max = null, maxScale = 2)) {
            _state.update { it.copy(warning = "“附加成本”必须为大于等于 0 的数值，且最多两位小数") }
            return
        }
        val additionalCosts = state.additionalCosts.toDoubleOrNull() ?: run {
            _state.update { it.copy(warning = "“附加成本”格式不正确") }
            return
        }
        _state.update { it.copy(isParametersExpanded = false) }
        val system = state.system.trim().ifBlank { "C-J6MT" }
        val input = state.input.trim()
        if (input.isBlank()) {
            _state.update { it.copy(warning = "请输入蓝图名称或蓝图 Type ID") }
            return
        }

        val typeIdInput = input.toIntOrNull()
        val typeId = typeIdInput ?: state.selectedBlueprint?.typeId
        if (typeId == null) {
            val resolution = resolveBlueprintInputCandidates(input)
            when (resolution) {
                is BlueprintNameResolver.ResolveResult.NotFound -> {
                    _state.update { it.copy(warning = "未找到蓝图，请检查拼写或先从候选中选择") }
                    return
                }

                is BlueprintNameResolver.ResolveResult.Ambiguous -> {
                    val preferChinese = isChineseInput(input)
                    _state.update {
                        it.copy(
                            warning = "匹配到多个蓝图，请从候选中选择更具体的项目",
                            suggestions = resolution.matches.map { match ->
                                SuggestionItem(
                                    label = getSuggestionLabel(match, preferChinese),
                                    match = match,
                                )
                            },
                        )
                    }
                    return
                }

                is BlueprintNameResolver.ResolveResult.Resolved -> {
                    queryBuildCost(
                        typeId = resolution.match.typeId,
                        displayName = resolution.match.enName,
                        selectedBlueprint = resolution.match,
                        quantity = quantity,
                        additionalCosts = additionalCosts,
                        baseMe = baseMe,
                        componentsMe = componentsMe,
                        system = system,
                        facilityTax = facilityTax,
                        priceMode = state.priceMode,
                        includeReactionJobs = state.includeReactionJobs,
                        industryStructureType = state.industryStructureType,
                        industryRigType = state.industryRigType,
                        reactionStructureType = state.reactionStructureType,
                        reactionRigType = state.reactionRigType,
                    )
                    return
                }
            }
        } else {
            val selectedBlueprint = state.selectedBlueprint ?: blueprintNameResolver.getByTypeId(typeId)
            val displayName = selectedBlueprint?.enName ?: "Type ID $typeId"
            queryBuildCost(
                typeId = typeId,
                displayName = displayName,
                selectedBlueprint = selectedBlueprint,
                quantity = quantity,
                additionalCosts = additionalCosts,
                baseMe = baseMe,
                componentsMe = componentsMe,
                system = system,
                facilityTax = facilityTax,
                priceMode = state.priceMode,
                includeReactionJobs = state.includeReactionJobs,
                industryStructureType = state.industryStructureType,
                industryRigType = state.industryRigType,
                reactionStructureType = state.reactionStructureType,
                reactionRigType = state.reactionRigType,
            )
        }
    }

    private fun resolveBlueprintInputCandidates(input: String): BlueprintNameResolver.ResolveResult {
        val candidates = extractBlueprintInputCandidates(input)
        candidates.forEach { candidate ->
            val result = resolveBlueprintInputWithFallback(candidate)
            if (result !is BlueprintNameResolver.ResolveResult.NotFound) return result
        }
        return BlueprintNameResolver.ResolveResult.NotFound
    }

    private fun extractBlueprintInputCandidates(input: String): List<String> {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return emptyList()
        val candidates = linkedSetOf(trimmed)
        val match = Regex("""^\s*(.+?)\s*[（(]\s*(.+?)\s*[）)]\s*$""").matchEntire(trimmed)
        if (match != null) {
            val first = match.groupValues[1].trim()
            val second = match.groupValues[2].trim()
            if (first.isNotBlank()) candidates += first
            if (second.isNotBlank()) candidates += second
        }
        val beforeParen = trimmed.substringBefore("（").substringBefore("(").trim()
        if (beforeParen.isNotBlank()) candidates += beforeParen
        return candidates.toList()
    }

    private fun resolveBlueprintInputWithFallback(input: String): BlueprintNameResolver.ResolveResult {
        val primary = blueprintNameResolver.resolve(input)
        if (primary !is BlueprintNameResolver.ResolveResult.NotFound) return primary
        if ("blueprint" in input.lowercase()) return primary
        val fallbackInput = "$input Blueprint"
        return blueprintNameResolver.resolve(fallbackInput)
    }

    private fun queryBuildCost(
        typeId: Int,
        displayName: String,
        selectedBlueprint: BlueprintNameResolver.Match?,
        quantity: Int,
        additionalCosts: Double,
        baseMe: Int,
        componentsMe: Int,
        system: String,
        facilityTax: Double,
        priceMode: PriceMode,
        includeReactionJobs: Boolean,
        industryStructureType: IndustryStructureType,
        industryRigType: IndustryRigType,
        reactionStructureType: ReactionStructureType,
        reactionRigType: ReactionRigType,
    ) {
        _state.update {
            it.copy(
                isLoading = true,
                warning = null,
                resultTitle = "",
                summaryFields = emptyList(),
            )
        }
        viewModelScope.launch {
            val request = EveCookbookApi.BuildCostRequest(
                blueprintTypeIds = listOf(typeId),
                quantity = quantity,
                additionalCosts = additionalCosts,
                baseMe = baseMe,
                componentsMe = componentsMe,
                system = system,
                facilityTax = facilityTax,
                priceMode = priceMode.apiValue,
                includeReactionJobs = includeReactionJobs,
                blueprintVersion = "tq",
                industryStructureType = industryStructureType.apiValue,
                industryRig = industryRigType.apiValue,
                reactionStructureType = reactionStructureType.apiValue,
                reactionRig = reactionRigType.apiValue,
            )
            when (val result = eveCookbookApi.getBuildCost(request)) {
                is Result.Success -> {
                    val response = result.data.firstOrNull()
                    val structured = response?.body?.let(::parseStructuredResult)
                    val isSuccess = response?.statusCode == 200
                    val title = if (response?.statusCode == 200) {
                        "查询成功：$displayName x$quantity"
                    } else {
                        "查询失败：$displayName（HTTP ${response?.statusCode ?: "?"}）"
                    }
                    val baseWarning = structured?.warning
                        ?: response?.message
                            ?.takeUnless { msg -> msg.isBlank() }
                            ?.takeIf { !isSuccess }
                    val (summaryFields, marketWarning) = if (isSuccess && structured != null) {
                        enrichSummaryWithMarketData(
                            requestBlueprintTypeId = typeId,
                            structured = structured,
                        )
                    } else {
                        structured?.summaryFields.orEmpty() to null
                    }
                    _state.update {
                        val effectiveBlueprint = selectedBlueprint ?: blueprintNameResolver.getByTypeId(typeId)
                        val titleName = effectiveBlueprint?.let { formatBlueprintName(it) } ?: displayName
                        val normalizedFields = withLocalizedBlueprintName(
                            summaryFields = summaryFields,
                            selectedBlueprint = effectiveBlueprint,
                        )
                        it.copy(
                            isLoading = false,
                            selectedBlueprint = effectiveBlueprint ?: BlueprintNameResolver.Match(typeId = typeId, enName = displayName),
                            resultTitle = if (response?.statusCode == 200) {
                                "查询成功：$titleName x$quantity"
                            } else {
                                structured?.preferredTitle ?: title
                            },
                            summaryFields = normalizedFields,
                            warning = mergeWarnings(baseWarning, marketWarning),
                        )
                    }
                }

                is Result.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            resultTitle = "请求失败",
                            summaryFields = emptyList(),
                            warning = result.cause?.message ?: "网络请求失败",
                        )
                    }
                }
            }
        }
    }

    private fun parseStructuredResult(rawBody: String): StructuredBuildCostResult {
        return try {
            val root = Json.parseToJsonElement(rawBody)
            val rootObj = root.jsonObject
            val error = rootObj["error"]?.jsonPrimitive?.intOrNull
            val status = rootObj["status"]?.jsonPrimitive?.intOrNull
            val messageElement = rootObj["message"]

            val fields = buildList {
                when (messageElement) {
                    is JsonObject -> {
                        val blueprintName = messageElement["blueprintName"]?.jsonPrimitive?.contentOrNull
                        val blueprintTypeId = messageElement["blueprintTypeId"]?.jsonPrimitive?.intOrNull
                        if (!blueprintName.isNullOrBlank()) add(SummaryField("蓝图名称", blueprintName))

                        addCurrencyField(this, "材料成本", messageElement, "materialCost")
                        addCurrencyField(this, "作业成本", messageElement, "jobCost")
                        addCurrencyField(this, "附加成本", messageElement, "additionalCost")
                        addCurrencyField(this, "总成本", messageElement, "totalCost")
                        addCurrencyField(this, "单件建造成本", messageElement, "buildCostPerUnit")
                        addCurrencyField(this, "余料价值", messageElement, "excessMaterialsValue")

                        val producedQuantity = messageElement["producedQuantity"]?.jsonPrimitive?.intOrNull
                        if (producedQuantity != null) add(SummaryField("产出数量", producedQuantity.toString()))
                    }

                    is JsonPrimitive -> {
                        val text = messageElement.contentOrNull
                        if (!text.isNullOrBlank()) {
                            add(SummaryField("消息", text))
                        }
                    }

                    else -> {
                        // Keep empty when the API sends unexpected payload.
                    }
                }
            }
            val preferredTitle = (messageElement as? JsonObject)
                ?.get("blueprintName")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?.let { "查询结果：$it" }
            val warning = (messageElement as? JsonPrimitive)?.contentOrNull
            val messageObject = messageElement as? JsonObject
            StructuredBuildCostResult(
                summaryFields = fields.distinctBy { it.label to it.value },
                preferredTitle = preferredTitle,
                warning = warning,
                totalCost = messageObject?.get("totalCost")?.jsonPrimitive?.doubleOrNull,
                producedQuantity = messageObject?.get("producedQuantity")?.jsonPrimitive?.intOrNull,
                blueprintTypeId = messageObject?.get("blueprintTypeId")?.jsonPrimitive?.intOrNull,
            )
        } catch (_: Exception) {
            StructuredBuildCostResult(summaryFields = emptyList())
        }
    }

    private fun addCurrencyField(
        fields: MutableList<SummaryField>,
        label: String,
        source: JsonObject,
        key: String,
    ) {
        val value = source[key]?.jsonPrimitive?.doubleOrNull ?: return
        fields += SummaryField(label, "%,.2f ISK".format(value))
    }

    private val JsonPrimitive.contentOrNull: String?
        get() = runCatching { content }.getOrNull()

    private fun isChineseInput(text: String): Boolean {
        return text.any { char ->
            Character.UnicodeScript.of(char.code) == Character.UnicodeScript.HAN
        }
    }

    private fun getSuggestionLabel(match: BlueprintNameResolver.Match, preferChinese: Boolean): String {
        return if (preferChinese) {
            match.zhName?.let { "$it（${match.enName}）" } ?: match.enName
        } else {
            formatBlueprintName(match)
        }
    }

    private fun formatBlueprintName(match: BlueprintNameResolver.Match): String {
        return match.zhName?.let { "${match.enName}（$it）" } ?: match.enName
    }

    private fun withLocalizedBlueprintName(
        summaryFields: List<SummaryField>,
        selectedBlueprint: BlueprintNameResolver.Match?,
    ): List<SummaryField> {
        val match = selectedBlueprint ?: return summaryFields
        return summaryFields.map { field ->
            if (field.label == "蓝图名称") {
                field.copy(value = formatBlueprintName(match))
            } else {
                field
            }
        }
    }

    private fun getSystemSuggestions(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        if (solarSystemsRepository.getSystem(input) != null) return emptyList()
        return solarSystemsRepository.getSystems()
            .asSequence()
            .map { it.name }
            .filter { it.startsWith(input, ignoreCase = true) }
            .take(5)
            .toList()
    }

    private fun sanitizeDecimalInput(
        value: String,
        maxIntegerDigits: Int,
    ): String {
        val filtered = value.filter { it.isDigit() || it == '.' }
        if (filtered.isBlank()) return ""
        val dotIndex = filtered.indexOf('.')
        val rawInteger = if (dotIndex >= 0) filtered.substring(0, dotIndex) else filtered
        val normalizedInteger = rawInteger
            .filter { it.isDigit() }
            .take(maxIntegerDigits)
            .trimStart('0')
            .ifBlank { "0" }
        if (dotIndex < 0) return normalizedInteger
        val fraction = filtered
            .substring(dotIndex + 1)
            .filter { it.isDigit() }
            .take(2)
        val hasTrailingDot = filtered.endsWith('.') && fraction.isEmpty()
        return when {
            hasTrailingDot -> "$normalizedInteger."
            fraction.isNotEmpty() -> "$normalizedInteger.$fraction"
            else -> normalizedInteger
        }
    }

    private fun isValidDecimalInRange(
        input: String,
        min: Double,
        max: Double?,
        maxScale: Int,
        mustBeGreaterThanMin: Boolean = false,
    ): Boolean {
        if (!Regex("^\\d+(\\.\\d{1,$maxScale})?$").matches(input)) return false
        val value = input.toDoubleOrNull() ?: return false
        if (mustBeGreaterThanMin) {
            if (value <= min) return false
        } else {
            if (value < min) return false
        }
        if (max != null && value > max) return false
        val scaleFactor = 10.0.pow(maxScale)
        val scaled = value * scaleFactor
        return kotlin.math.abs(scaled - scaled.toLong()) < 1e-6
    }

    private suspend fun enrichSummaryWithMarketData(
        requestBlueprintTypeId: Int,
        structured: StructuredBuildCostResult,
    ): Pair<List<SummaryField>, String?> {
        val totalCost = structured.totalCost ?: return structured.summaryFields to null
        val blueprintTypeId = structured.blueprintTypeId ?: requestBlueprintTypeId
        val product = when (val productResult = blueprintProductsRepository.getManufacturedProduct(blueprintTypeId)) {
            is Result.Success -> productResult.data
            is Result.Failure -> {
                return structured.summaryFields to "无法查询蓝图产出映射，已跳过市场利润计算"
            }
        } ?: return structured.summaryFields to "该蓝图没有制造产出映射，已跳过市场利润计算"

        val outputQuantity = (structured.producedQuantity ?: product.quantity).coerceAtLeast(1)
        val productTypeId = product.typeId
        val productName = typesRepository.getTypeName(productTypeId) ?: "Type ID $productTypeId"
        val appraisalInput = "$productName $outputQuantity"
        val prices = janiceApiClient.appraise(
            input = appraisalInput,
            pricePercentage = 1.0,
        ).getOrNull()?.body?.let(::parseAppraisalPrices)
            ?: return structured.summaryFields + listOf(
                SummaryField("产出物品", "$productName x$outputQuantity"),
                SummaryField("产出物品 Type ID", productTypeId.toString()),
            ) to "无法获取产出物市场价格，已跳过利润计算"

        val buyProfit = prices.buyTotal - totalCost
        val sellProfit = prices.sellTotal - totalCost
        val sellMarginPercent = if (totalCost > 0) sellProfit / totalCost * 100 else null

        val marketFields = listOf(
            SummaryField("产出物品", "$productName x$outputQuantity"),
            SummaryField("参考市场", prices.marketName ?: "吉他"),
            SummaryField("产出总价（买单）", formatIsk(prices.buyTotal)),
            SummaryField("产出总价（卖单）", formatIsk(prices.sellTotal)),
            SummaryField("预计利润（买单）", formatIsk(buyProfit)),
            SummaryField("预计利润（卖单）", formatIsk(sellProfit)),
        ) + (sellMarginPercent?.let { listOf(SummaryField("预计利润率（卖单）", "%.2f%%".format(it))) } ?: emptyList())

        return (structured.summaryFields + marketFields).distinctBy { it.label to it.value } to null
    }

    private fun parseAppraisalPrices(rawBody: String): AppraisalPrices? {
        return runCatching {
            val root = Json.parseToJsonElement(rawBody).jsonObject
            val result = root["result"]?.jsonObject ?: root
            val marketName = result["market"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            val effective = result["effectivePrices"]?.jsonObject ?: return null
            AppraisalPrices(
                marketName = marketName,
                buyTotal = effective["totalBuyPrice"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                sellTotal = effective["totalSellPrice"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            )
        }.getOrNull()
    }

    private fun formatIsk(value: Double): String {
        return "%,.2f ISK".format(value)
    }

    private fun mergeWarnings(primary: String?, secondary: String?): String? {
        return when {
            primary.isNullOrBlank() && secondary.isNullOrBlank() -> null
            primary.isNullOrBlank() -> secondary
            secondary.isNullOrBlank() -> primary
            primary == secondary -> primary
            else -> "$primary；$secondary"
        }
    }

    private fun getInitialState(): UiState {
        val saved = settings.manufacturingAssistant
        return UiState(
            quantity = saved.quantity,
            additionalCosts = saved.additionalCosts,
            baseMe = saved.baseMe,
            componentsMe = saved.componentsMe,
            system = saved.system,
            facilityTax = saved.facilityTax,
            priceMode = PriceMode.entries.firstOrNull { it.apiValue == saved.priceMode } ?: PriceMode.Buy,
            includeReactionJobs = saved.includeReactionJobs,
            industryStructureType = IndustryStructureType.entries.firstOrNull { it.apiValue == saved.industryStructureType } ?: IndustryStructureType.Sotiyo,
            industryRigType = IndustryRigType.entries.firstOrNull { it.apiValue == saved.industryRigType } ?: IndustryRigType.T2,
            reactionStructureType = ReactionStructureType.entries.firstOrNull { it.apiValue == saved.reactionStructureType } ?: ReactionStructureType.Tatara,
            reactionRigType = ReactionRigType.entries.firstOrNull { it.apiValue == saved.reactionRigType } ?: ReactionRigType.T2,
        )
    }

    private fun saveParameters() {
        val state = _state.value
        settings.manufacturingAssistant = ManufacturingAssistantSettings(
            quantity = state.quantity,
            additionalCosts = state.additionalCosts,
            baseMe = state.baseMe,
            componentsMe = state.componentsMe,
            system = state.system,
            facilityTax = state.facilityTax,
            priceMode = state.priceMode.apiValue,
            includeReactionJobs = state.includeReactionJobs,
            industryStructureType = state.industryStructureType.apiValue,
            industryRigType = state.industryRigType.apiValue,
            reactionStructureType = state.reactionStructureType.apiValue,
            reactionRigType = state.reactionRigType.apiValue,
        )
    }
}
