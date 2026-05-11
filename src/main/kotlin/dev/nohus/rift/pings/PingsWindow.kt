package dev.nohus.rift.pings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftOpportunityCard
import dev.nohus.rift.compose.RiftOpportunityCardBottomContent
import dev.nohus.rift.compose.RiftOpportunityCardButton
import dev.nohus.rift.compose.RiftOpportunityCardCategory
import dev.nohus.rift.compose.RiftOpportunityCardType
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.annotateLinks
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.copy_16px
import dev.nohus.rift.generated.resources.fitting_16px
import dev.nohus.rift.generated.resources.microphone
import dev.nohus.rift.generated.resources.window_sovereignty
import dev.nohus.rift.pings.PingsViewModel.UiState
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager
import java.time.ZoneId

@Composable
fun PingsWindow(
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: PingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "集结通知",
        icon = Res.drawable.window_sovereignty,
        state = windowState,
        onCloseClick = onCloseRequest,
        withContentPadding = false,
    ) {
        PingsWindowContent(
            state = state,
            onOpenJabberClick = viewModel::onOpenJabberClick,
            onMumbleClick = viewModel::onMumbleClick,
        )
    }
}

@Composable
private fun PingsWindowContent(
    state: UiState,
    onOpenJabberClick: () -> Unit,
    onMumbleClick: (url: String) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        val scrollState = rememberScrollState()
        LaunchedEffect(state.pings) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        ScrollbarColumn(
            scrollState = scrollState,
            contentPadding = PaddingValues(start = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            scrollbarModifier = Modifier.padding(horizontal = Spacing.small),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = Spacing.large, top = Spacing.medium),
        ) {
            state.pings.forEach { ping ->
                when (ping) {
                    is PingUiModel.PlainText -> PlainTextPing(state.displayTimezone, ping)
                    is PingUiModel.FleetPing -> FleetPing(state.displayTimezone, ping, onMumbleClick)
                }
            }
        }
        if (state.pings.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val text = if (state.isJabberConnected) {
                    "目前没有接收到集结通知。\n安心刷怪挖矿吧。"
                } else {
                    "需要登陆Jabber才能接受集结通知"
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.headerPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large),
                )
                if (!state.isJabberConnected) {
                    RiftButton(
                        text = "登陆Jabber",
                        onClick = onOpenJabberClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlainTextPing(
    displayTimezone: ZoneId,
    ping: PingUiModel.PlainText,
) {
    val type = buildAnnotatedString {
        if (ping.target == null || ping.target == "all") {
            append("公告")
        } else {
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.target.replaceFirstChar { it.uppercase() })
            }
            append(" 消息")
        }
        if (ping.sender != null) {
            append(" 来自 ")
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.sender)
            }
        }
    }
    val buttons = mutableListOf<RiftOpportunityCardButton>()
    buttons += RiftOpportunityCardButton(
        resource = Res.drawable.copy_16px,
        tooltip = "复制集结消息",
        action = { Clipboard.copy(ping.sourceText) },
    )
    RiftOpportunityCard(
        category = RiftOpportunityCardCategory.Unclassified,
        type = RiftOpportunityCardType(type),
        solarSystemChipState = null,
        topRight = null,
        bottomContent = RiftOpportunityCardBottomContent.Timestamp(null, ping.timestamp, displayTimezone),
        buttons = buttons,
    ) {
        val descriptionStyle = if (ping.text.length <= 50) {
            RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold)
        } else {
            RiftTheme.typography.bodyPrimary
        }
        val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
        val localizedMessage = remember(ping.text) { toBilingualPingText(ping.text) }
        val linkifiedMessage = remember(localizedMessage) { annotateLinks(localizedMessage, linkStyle) }
        Text(
            text = linkifiedMessage,
            style = descriptionStyle,
        )
    }
}

@Composable
private fun FleetPing(
    displayTimezone: ZoneId,
    ping: PingUiModel.FleetPing,
    onMumbleClick: (url: String) -> Unit,
) {
    val type = buildAnnotatedString {
        if (ping.target == null || ping.target == "all") {
            append("舰队通知")
        } else {
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.target.replaceFirstChar { it.uppercase() })
            }
            append(" 舰队")
        }
        if (ping.fleet != null) {
            append(" ")
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.fleet)
            }
        }
        append(" 指挥官 ")
        withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
            append(ping.fleetCommander.name)
        }
    }
    val buttons = mutableListOf<RiftOpportunityCardButton>()
    if (ping.doctrine?.link != null) {
        buttons += RiftOpportunityCardButton(
            resource = Res.drawable.fitting_16px,
            tooltip = "打开编队论坛帖",
            action = { ping.doctrine.link.toURIOrNull()?.openBrowser() },
        )
    }
    buttons += RiftOpportunityCardButton(
        resource = Res.drawable.copy_16px,
        tooltip = "复制集结消息",
        action = { Clipboard.copy(ping.sourceText) },
    )
    if (ping.comms is Comms.Mumble) {
        buttons += RiftOpportunityCardButton(
            resource = Res.drawable.microphone,
            tooltip = "在 Mumble 加入 ${ping.comms.channel}",
            action = { onMumbleClick(ping.comms.link) },
        )
    }
    val title = when (ping.papType) {
        PapType.Peacetime -> "和平 PAP"
        PapType.Strategic -> "战略 PAP"
        is PapType.Text -> "${ping.papType.text.replaceFirstChar { it.uppercase() }} PAP"
        null -> "无 PAP"
    }
    RiftOpportunityCard(
        category = ping.opportunityCategory,
        type = RiftOpportunityCardType(type),
        solarSystemChipState = ping.formupLocations,
        topRight = ping.fleetCommander,
        bottomContent = RiftOpportunityCardBottomContent.Timestamp(title, ping.timestamp, displayTimezone),
        buttons = buttons,
    ) {
        val descriptionStyle = if (ping.description.length <= 50) {
            RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold)
        } else {
            RiftTheme.typography.bodyPrimary
        }
        val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
        val localizedDescription = remember(ping.description) { toBilingualPingText(ping.description) }
        val linkifiedMessage = remember(localizedDescription) { annotateLinks(localizedDescription, linkStyle) }
        Text(
            text = linkifiedMessage,
            style = descriptionStyle,
            modifier = Modifier.padding(top = Spacing.mediumLarge),
        )
        if (ping.comms is Comms.Text) {
            Text(
                text = "语音：",
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier.padding(top = Spacing.mediumLarge),
            )
            val localizedComms = remember(ping.comms.text) { toBilingualPingText(ping.comms.text) }
            val linkifiedComms = remember(localizedComms) { annotateLinks(localizedComms, linkStyle) }
            Text(
                text = linkifiedComms,
                style = RiftTheme.typography.bodyPrimary,
            )
        }
        if (ping.doctrine != null) {
            Text(
                text = "编队：",
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier.padding(top = Spacing.mediumLarge),
            )
            Text(
                text = toBilingualPingText(ping.doctrine.text),
                style = RiftTheme.typography.bodyPrimary,
            )
        }
    }
}
