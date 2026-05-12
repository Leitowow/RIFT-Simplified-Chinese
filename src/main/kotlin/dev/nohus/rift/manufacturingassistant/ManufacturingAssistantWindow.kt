package dev.nohus.rift.manufacturingassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.RiftAutocompleteTextField
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftWarningBanner
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.pi_processor
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ManufacturingAssistantWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: ManufacturingAssistantViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    var copyHint by remember { mutableStateOf<String?>(null) }
    var loadingSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(copyHint) {
        if (copyHint != null) {
            delay(1200)
            copyHint = null
        }
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            loadingSeconds = 0
            return@LaunchedEffect
        }
        loadingSeconds = 0
        while (isActive && state.isLoading) {
            delay(1000)
            loadingSeconds += 1
        }
    }
    RiftWindow(
        title = "制造助手",
        icon = Res.drawable.pi_processor,
        state = windowState,
        onCloseClick = onCloseRequest,
        withContentPadding = false,
    ) {
        ScrollbarColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier
                .fillMaxSize()
                .background(RiftTheme.colors.windowBackground)
                .padding(Spacing.large),
        ) {
            Text(
                text = "制造助手",
                style = RiftTheme.typography.displayHighlighted,
            )
            Text(
                text = "输入蓝图英文名、中文名或 Type ID。",
                style = RiftTheme.typography.headerPrimary,
            )
            RiftAutocompleteTextField(
                text = state.input,
                suggestions = state.suggestions.map { it.label },
                placeholder = "例如：Damage Control I Blueprint / Damage Control I / 2047",
                onTextChanged = viewModel::onInputChanged,
                onSuggestionConfirmed = viewModel::onSuggestionConfirmed,
                selectAllOnFocus = true,
                suggestionRowHeight = 28.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            RiftButton(
                text = if (state.isParametersExpanded) "收起计算参数" else "计算参数",
                cornerCut = ButtonCornerCut.Both,
                onClick = { viewModel.onParametersExpandedChange(!state.isParametersExpanded) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isParametersExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RiftTheme.colors.windowBackgroundSecondary.copy(alpha = 0.6f))
                        .padding(Spacing.small),
                ) {
                    Text(
                        text = "计算参数",
                        style = RiftTheme.typography.headerSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), modifier = Modifier.fillMaxWidth()) {
                        LabeledTextField("数量", state.quantity, viewModel::onQuantityChanged, Modifier.weight(1f), TextAlign.Center)
                        LabeledTextField("附加成本 ISK（如复制图等）", state.additionalCosts, viewModel::onAdditionalCostsChanged, Modifier.weight(1f), TextAlign.Center)
                        LabeledAutocompleteTextField(
                            label = "制造星系",
                            value = state.system,
                            suggestions = state.systemSuggestions,
                            onValueChange = viewModel::onSystemChanged,
                            onSuggestionConfirmed = viewModel::onSystemSuggestionConfirmed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), modifier = Modifier.fillMaxWidth()) {
                        LabeledTextField("主蓝图材料效率", state.baseMe, viewModel::onBaseMeChanged, Modifier.weight(1f), TextAlign.Center)
                        LabeledTextField("组件材料效率", state.componentsMe, viewModel::onComponentsMeChanged, Modifier.weight(1f), TextAlign.Center)
                        LabeledTextField("建筑税率 %", state.facilityTax, viewModel::onFacilityTaxChanged, Modifier.weight(1f), TextAlign.Center)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), modifier = Modifier.fillMaxWidth()) {
                        LabeledDropdown("价格模式", state.priceMode, ManufacturingAssistantViewModel.PriceMode.entries, viewModel::onPriceModeChanged, Modifier.weight(1f)) { it.label }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("反应作业", style = RiftTheme.typography.bodySecondary)
                            RiftCheckboxWithLabel(
                                label = "计算反应",
                                isChecked = state.includeReactionJobs,
                                onCheckedChange = viewModel::onIncludeReactionJobsChanged,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("蓝图版本", style = RiftTheme.typography.bodySecondary)
                            Text("TQ（正式服）", style = RiftTheme.typography.bodyPrimary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), modifier = Modifier.fillMaxWidth()) {
                        LabeledDropdown("工业结构", state.industryStructureType, ManufacturingAssistantViewModel.IndustryStructureType.entries, viewModel::onIndustryStructureTypeChanged, Modifier.weight(1f)) { it.label }
                        LabeledDropdown("工业改装件", state.industryRigType, ManufacturingAssistantViewModel.IndustryRigType.entries, viewModel::onIndustryRigTypeChanged, Modifier.weight(1f)) { it.label }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), modifier = Modifier.fillMaxWidth()) {
                        LabeledDropdown("反应结构", state.reactionStructureType, ManufacturingAssistantViewModel.ReactionStructureType.entries, viewModel::onReactionStructureTypeChanged, Modifier.weight(1f)) { it.label }
                        LabeledDropdown("反应改装件", state.reactionRigType, ManufacturingAssistantViewModel.ReactionRigType.entries, viewModel::onReactionRigTypeChanged, Modifier.weight(1f)) { it.label }
                    }
                }
            }
            RiftButton(
                text = if (state.isLoading) "查询中…（已等待 ${loadingSeconds}s）" else "查询建造成本",
                cornerCut = ButtonCornerCut.Both,
                onClick = viewModel::onQueryClick,
                modifier = Modifier.fillMaxWidth(),
            )
            val warning = state.warning
            if (warning != null) {
                RiftWarningBanner(text = warning)
            }
            if (state.resultTitle.isNotBlank()) {
                if (state.summaryFields.isNotEmpty()) {
                    RiftButton(
                        text = "复制结构化结果",
                        isCompact = true,
                        cornerCut = ButtonCornerCut.Both,
                        onClick = {
                            Clipboard.copy(buildStructuredSummaryText(state.summaryFields))
                            copyHint = "已复制至粘贴板"
                        },
                    )
                }
                Text(
                    text = state.resultTitle,
                    style = RiftTheme.typography.headerPrimary,
                )
            }
            if (state.summaryFields.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RiftTheme.colors.windowBackgroundSecondary.copy(alpha = 0.85f)),
                ) {
                    state.summaryFields.forEachIndexed { index, field ->
                        val rowBackground = if (index % 2 == 0) {
                            RiftTheme.colors.windowBackgroundSecondary.copy(alpha = 0.4f)
                        } else {
                            RiftTheme.colors.windowBackgroundSecondary.copy(alpha = 0.15f)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBackground)
                                .padding(horizontal = Spacing.small, vertical = 6.dp),
                        ) {
                            Text(
                                text = "${field.label}：",
                                style = RiftTheme.typography.bodySecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(170.dp),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (shouldShowNoMarketHint(field)) {
                                    Text(
                                        text = "（参考市场没有此产出物品）",
                                        style = RiftTheme.typography.bodySecondary,
                                        color = RiftTheme.colors.warningColor,
                                        modifier = Modifier.padding(end = Spacing.verySmall),
                                    )
                                }
                                val valueStyle = getProfitValueColor(field)
                                    ?.let { RiftTheme.typography.bodyPrimary.copy(color = it) }
                                    ?: RiftTheme.typography.bodyPrimary
                                if (isCopyableIskValue(field.value)) {
                                    LinkText(
                                        text = field.value,
                                        normalStyle = valueStyle,
                                        hoveredStyle = RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.textLink),
                                        onClick = {
                                            Clipboard.copy(extractCopyableValue(field.value))
                                            copyHint = "已复制至粘贴板"
                                        },
                                    )
                                } else {
                                    Text(
                                        text = field.value,
                                        style = valueStyle,
                                        textAlign = TextAlign.End,
                                    )
                                }
                            }
                        }
                    }
                }
                if (copyHint != null) {
                    Text(
                        text = copyHint!!,
                        style = RiftTheme.typography.detailPrimary,
                        color = RiftTheme.colors.textLink,
                        modifier = Modifier.padding(top = Spacing.verySmall),
                    )
                }
            }
            state.selectedBlueprint?.let { selected ->
                val name = selected.zhName?.let { "${selected.enName}（$it）" } ?: selected.enName
                Text(
                    text = "当前蓝图：$name (ID: ${selected.typeId})",
                    style = RiftTheme.typography.bodySecondary,
                    color = RiftTheme.colors.textSecondary,
                )
            }
            Text(
                text = "提示：若中文输入未匹配，请先注册中文别名或直接使用 Type ID。",
                style = RiftTheme.typography.detailPrimary,
                color = RiftTheme.colors.textDisabled,
            )
        }
    }
}

private fun isCopyableIskValue(value: String): Boolean {
    return "ISK" in value.uppercase()
}

private fun buildStructuredSummaryText(fields: List<ManufacturingAssistantViewModel.SummaryField>): String {
    return fields.joinToString(separator = "\n") { field ->
        "${field.label}：${field.value}"
    }
}

private fun extractCopyableValue(value: String): String {
    val iskIndex = value.indexOf("ISK", ignoreCase = true)
    val numericPart = if (iskIndex >= 0) value.substring(0, iskIndex) else value
    return numericPart.trim()
}

private fun shouldShowNoMarketHint(field: ManufacturingAssistantViewModel.SummaryField): Boolean {
    if (field.label != "产出总价（卖单）") return false
    val numeric = field.value
        .replace("ISK", "", ignoreCase = true)
        .replace(",", "")
        .trim()
        .toDoubleOrNull()
    return numeric == 0.0
}

@Composable
private fun getProfitValueColor(field: ManufacturingAssistantViewModel.SummaryField): Color? {
    val profitLabels = setOf("预计利润（买单）", "预计利润（卖单）", "预计利润率（卖单）")
    if (field.label !in profitLabels) return null
    val numeric = field.value
        .replace("ISK", "", ignoreCase = true)
        .replace("%", "")
        .replace(",", "")
        .trim()
        .toDoubleOrNull()
        ?: return null
    return when {
        numeric < 0 -> RiftTheme.colors.textRed
        numeric > 0 -> RiftTheme.colors.textGreen
        else -> null
    }
}

@Composable
private fun LabeledAutocompleteTextField(
    label: String,
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    onSuggestionConfirmed: (String) -> Unit,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = RiftTheme.typography.bodySecondary,
        )
        RiftAutocompleteTextField(
            text = value,
            suggestions = suggestions,
            onTextChanged = onValueChange,
            onSuggestionConfirmed = onSuggestionConfirmed,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = RiftTheme.typography.bodySecondary,
        )
        RiftTextField(
            text = value,
            onTextChanged = onValueChange,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun <T> LabeledDropdown(
    label: String,
    value: T,
    values: List<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    labeler: (T) -> String,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = RiftTheme.typography.bodySecondary,
        )
        RiftDropdown(
            items = values,
            selectedItem = value,
            onItemSelected = onValueChange,
            getItemName = labeler,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
