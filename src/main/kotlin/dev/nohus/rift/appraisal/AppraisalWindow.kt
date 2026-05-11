package dev.nohus.rift.appraisal

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftSlider
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftWarningBanner
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_wallet
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.delay

@Composable
fun AppraisalWindow(
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: AppraisalViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "估价",
        icon = Res.drawable.window_wallet,
        state = windowState,
        onCloseClick = onCloseRequest,
        withContentPadding = false,
    ) {
        AnimatedContent(
            targetState = state.screen,
            modifier = Modifier.fillMaxSize(),
        ) { screen ->
            when (screen) {
                AppraisalViewModel.Screen.Input -> AppraisalInputContent(
                    input = state.input,
                    pricePercentage = state.pricePercentage,
                    isLoading = state.isLoading,
                    warning = state.warning,
                    onInputChanged = viewModel::onInputChanged,
                    onPricePercentageChanged = viewModel::onPricePercentageChanged,
                    onPricePercentageTextChanged = viewModel::onPricePercentageTextChanged,
                    onEstimateClick = viewModel::onEstimateClick,
                )

                AppraisalViewModel.Screen.Result -> AppraisalResultContent(
                    title = state.resultTitle,
                    result = state.resultBody,
                    appraisal = state.appraisalResult,
                    showReprocessButton = true,
                    isReprocessLoading = state.isReprocessLoading,
                    onReprocessClick = viewModel::onReprocessEstimateClick,
                    showBackToPreviousButton = false,
                    onBackToPreviousClick = null,
                    onBackClick = viewModel::onBackClick,
                )

                AppraisalViewModel.Screen.ReprocessResult -> AppraisalResultContent(
                    title = state.reprocessResultTitle,
                    result = state.reprocessResultBody,
                    appraisal = state.reprocessAppraisalResult,
                    showReprocessButton = false,
                    isReprocessLoading = false,
                    onReprocessClick = null,
                    showBackToPreviousButton = true,
                    onBackToPreviousClick = viewModel::onBackToPrimaryResultClick,
                    onBackClick = viewModel::onBackClick,
                )
            }
        }
    }
}

@Composable
private fun AppraisalInputContent(
    input: String,
    pricePercentage: Int,
    isLoading: Boolean,
    warning: String?,
    onInputChanged: (String) -> Unit,
    onPricePercentageChanged: (Int) -> Unit,
    onPricePercentageTextChanged: (String) -> Unit,
    onEstimateClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.medium),
    ) {
        Text(
            text = "输入要估价的物品文本（可粘贴游戏内物品列表）",
            style = RiftTheme.typography.headerPrimary,
        )
        RiftTextField(
            text = input,
            placeholder = "支持中英文混合输入\n例如：\n奇奇莫拉级 10\nTypoon 20",
            onTextChanged = onInputChanged,
            singleLine = false,
            minLines = 10,
            maxLines = 20,
            modifier = Modifier.fillMaxWidth(),
        )
        if (warning != null) {
            RiftWarningBanner(text = warning)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "折扣比例",
                style = RiftTheme.typography.headerSecondary,
            )
            RiftTextField(
                text = pricePercentage.toString(),
                onTextChanged = onPricePercentageTextChanged,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(44.dp),
            )
            Text(
                text = "%",
                style = RiftTheme.typography.bodyPrimary,
            )
            BoxWithConstraints(
                modifier = Modifier.weight(1f),
            ) {
                RiftSlider(
                    width = this@BoxWithConstraints.maxWidth,
                    range = 1..200,
                    currentValue = pricePercentage,
                    onValueChange = onPricePercentageChanged,
                    getValueName = { "$it%" },
                    isImmediate = true,
                    height = 32.dp,
                    trackHeight = 24.dp,
                    thumbWidth = 48.dp,
                    thumbHeight = 30.dp,
                    thumbCoreWidth = 6.dp,
                    thumbCoreHeight = 26.dp,
                    thumbVerticalOffset = (-2).dp,
                )
            }
        }
        RiftButton(
            text = if (isLoading) "估价中…" else "开始估价",
            cornerCut = ButtonCornerCut.Both,
            onClick = {
                if (!isLoading) onEstimateClick()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AppraisalResultContent(
    title: String,
    result: String,
    appraisal: AppraisalViewModel.AppraisalResult?,
    showReprocessButton: Boolean,
    isReprocessLoading: Boolean,
    onReprocessClick: (() -> Unit)?,
    showBackToPreviousButton: Boolean,
    onBackToPreviousClick: (() -> Unit)?,
    onBackClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = RiftTheme.typography.headerPrimary,
                modifier = Modifier.weight(1f),
            )
            if (showReprocessButton && onReprocessClick != null) {
                RiftButton(
                    text = if (isReprocessLoading) "化矿估价中…" else "进行化矿估价",
                    cornerCut = ButtonCornerCut.Both,
                    onClick = {
                        if (!isReprocessLoading) onReprocessClick()
                    },
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (appraisal != null) {
                AppraisalSummaryContent(
                    appraisal = appraisal,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ScrollbarColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = result,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }
        }
        if (showBackToPreviousButton && onBackToPreviousClick != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.fillMaxWidth(),
            ) {
                RiftButton(
                    text = "返回上一页",
                    cornerCut = ButtonCornerCut.Both,
                    onClick = onBackToPreviousClick,
                    modifier = Modifier.weight(1f),
                )
                RiftButton(
                    text = "返回估价首页",
                    cornerCut = ButtonCornerCut.Both,
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            RiftButton(
                text = "返回估价首页",
                cornerCut = ButtonCornerCut.Both,
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AppraisalSummaryContent(
    appraisal: AppraisalViewModel.AppraisalResult,
    modifier: Modifier = Modifier,
) {
    var copyHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(copyHint) {
        if (copyHint != null) {
            delay(1500)
            copyHint = null
        }
    }
    ScrollbarColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth(),
    ) {
        if (!appraisal.code.isNullOrBlank()) {
            Text(
                text = "估价编号：${appraisal.code}",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
        appraisal.janiceUrl?.let { janiceUrl ->
            LinkText(
                text = "复制估价链接至粘贴板",
                onClick = { Clipboard.copy(janiceUrl) },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RiftTheme.colors.windowBackgroundSecondary)
                .padding(Spacing.small),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.verySmall)) {
                CopyableValueRow(
                    label = "平均价估值",
                    value = formatIsk(appraisal.prices.split, withCents = true),
                    valueStyle = RiftTheme.typography.headlineHighlighted,
                    onValueCopied = { copyHint = "已复制至粘贴板" },
                )
                Text("市场：${appraisal.marketName}", style = RiftTheme.typography.bodyPrimary)
                Text("模式：${localizePricingLabel(appraisal.pricing)} / ${localizePricingLabel(appraisal.pricingVariant)}", style = RiftTheme.typography.bodyPrimary)
                CopyableValueRow(
                    label = "总体积",
                    value = appraisal.totalVolume.toString(),
                    valueStyle = RiftTheme.typography.bodyPrimary,
                    onValueCopied = { copyHint = "已复制至粘贴板" },
                )
                CopyableValueRow(
                    label = "打包体积",
                    value = appraisal.totalPackagedVolume.toString(),
                    valueStyle = RiftTheme.typography.bodyPrimary,
                    onValueCopied = { copyHint = "已复制至粘贴板" },
                )
                CopyableValueRow(
                    label = "买入总价",
                    value = formatIsk(appraisal.prices.buy, withCents = true),
                    valueStyle = RiftTheme.typography.bodyPrimary,
                    onValueCopied = { copyHint = "已复制至粘贴板" },
                )
                CopyableValueRow(
                    label = "平均价总价",
                    value = formatIsk(appraisal.prices.split, withCents = true),
                    valueStyle = RiftTheme.typography.headerHighlighted,
                    onValueCopied = { copyHint = "已复制至粘贴板" },
                )
                CopyableValueRow(
                    label = "卖出总价",
                    value = formatIsk(appraisal.prices.sell, withCents = true),
                    valueStyle = RiftTheme.typography.bodyPrimary,
                    onValueCopied = { copyHint = "已复制至粘贴板" },
                )
                if (copyHint != null) {
                    Text(
                        text = copyHint!!,
                        style = RiftTheme.typography.bodyHighlighted.copy(color = RiftTheme.colors.textLink),
                    )
                }
                if (appraisal.failures.isNotBlank()) {
                    RiftWarningBanner(text = "解析警告：${appraisal.failures}")
                }
            }
        }

        Text(
            text = "物品明细",
            style = RiftTheme.typography.headerPrimary,
            modifier = Modifier.padding(top = Spacing.small),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(RiftTheme.colors.windowBackgroundSecondaryHovered)
                .padding(horizontal = Spacing.small, vertical = Spacing.verySmall),
        ) {
            Text("物品", style = RiftTheme.typography.bodySecondary, modifier = Modifier.weight(1f))
            Text("数量", style = RiftTheme.typography.bodySecondary, modifier = Modifier.width(72.dp))
            Text("买入", style = RiftTheme.typography.bodySecondary, modifier = Modifier.width(120.dp))
            Text("平均价", style = RiftTheme.typography.bodySecondary, modifier = Modifier.width(120.dp))
            Text("卖出", style = RiftTheme.typography.bodySecondary, modifier = Modifier.width(120.dp))
        }
        appraisal.items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RiftTheme.colors.windowBackground.copy(alpha = 0.5f))
                    .padding(horizontal = Spacing.small, vertical = Spacing.verySmall),
            ) {
                Text(
                    text = item.name,
                    style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatNumber(item.amount),
                    style = RiftTheme.typography.bodySecondary,
                    modifier = Modifier.width(72.dp),
                )
                Text(
                    text = formatIsk(item.buyTotal, withCents = true),
                    style = RiftTheme.typography.bodyPrimary,
                    modifier = Modifier.width(120.dp),
                )
                Text(
                    text = formatIsk(item.splitTotal, withCents = true),
                    style = RiftTheme.typography.bodyHighlighted,
                    modifier = Modifier.width(120.dp),
                )
                Text(
                    text = formatIsk(item.sellTotal, withCents = true),
                    style = RiftTheme.typography.bodyPrimary,
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}

@Composable
private fun CopyableValueRow(
    label: String,
    value: String,
    valueStyle: TextStyle,
    onValueCopied: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.verySmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .hoverBackground()
            .padding(horizontal = Spacing.verySmall),
    ) {
        Text(
            text = "$label：",
            style = RiftTheme.typography.bodyPrimary,
        )
        LinkText(
            text = value,
            normalStyle = valueStyle,
            hoveredStyle = valueStyle.copy(color = RiftTheme.typography.bodyLink.color),
            onClick = {
                Clipboard.copy(value)
                onValueCopied()
            },
        )
    }
}

private fun localizePricingLabel(value: String): String {
    return when (value.lowercase()) {
        "split" -> "平均价"
        "immediate" -> "此时此刻"
        "purchase" -> "化矿"
        else -> value
    }
}
