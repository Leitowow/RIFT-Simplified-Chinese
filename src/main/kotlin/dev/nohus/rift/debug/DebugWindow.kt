package dev.nohus.rift.debug

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftProgressBar
import dev.nohus.rift.compose.RiftRadioButtonWithLabel
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.VerticalDivider
import dev.nohus.rift.compose.animateBackgroundHover
import dev.nohus.rift.compose.animateWindowBackgroundSecondaryHover
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.debug.DebugViewModel.DebugTab
import dev.nohus.rift.debug.DebugViewModel.UiState
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_log
import dev.nohus.rift.network.interceptors.EsiRateLimitInterceptor.BucketKey
import dev.nohus.rift.network.requests.CacheStatistics.CacheStatus
import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.RequestStatisticsInterceptor
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DebugWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: DebugViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "调试",
        icon = Res.drawable.window_log,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarContent = { height ->
            ToolbarRow(
                state = state,
                fixedHeight = height,
                onTabSelected = viewModel::onTabClick,
            )
        },
    ) {
        DebugWindowContent(
            state = state,
        )
    }
}

@Composable
private fun ToolbarRow(
    state: UiState,
    fixedHeight: Dp,
    onTabSelected: (DebugTab) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tabs = remember {
            DebugTab.entries.mapIndexed { index, tab ->
                val title = when (tab) {
                    DebugTab.Logs -> "Logs"
                    DebugTab.Network -> "Network Statistics"
                    DebugTab.RateLimits -> "Rate Limits"
                    DebugTab.Cache -> "Cache Statistics"
                }
                Tab(id = index, title = title, isCloseable = false)
            }
        }
        RiftTabBar(
            tabs = tabs,
            selectedTab = DebugTab.entries.indexOf(state.tab),
            onTabSelected = { onTabSelected(DebugTab.entries[it]) },
            onTabClosed = {},
            withUnderline = false,
            withWideTabs = true,
            fixedHeight = fixedHeight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DebugWindowContent(
    state: UiState,
) {
    AnimatedContent(state.tab) { selectedTab ->
        when (selectedTab) {
            DebugTab.Logs -> {
                Column(
                    modifier = Modifier.padding(top = Spacing.large),
                ) {
                    Text(
                        text = "${state.version} (${state.operatingSystem}), ${state.vmVersion}",
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.padding(bottom = Spacing.medium),
                    )

                    Text(
                        text = "Jabber ${state.isJabberConnected.connected}",
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.padding(bottom = Spacing.medium),
                    )

                    var minLevel by remember { mutableStateOf(Level.INFO) }
                    var isAutoScrolling by remember { mutableStateOf(true) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.padding(bottom = Spacing.medium),
                    ) {
                        Text(
                            text = "Level:",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                        RiftRadioButtonWithLabel(
                            label = "All",
                            isChecked = minLevel == Level.ALL,
                            onChecked = { minLevel = Level.ALL },
                        )
                        RiftRadioButtonWithLabel(
                            label = "Info",
                            isChecked = minLevel == Level.INFO,
                            onChecked = { minLevel = Level.INFO },
                        )
                        RiftRadioButtonWithLabel(
                            label = "Warn",
                            isChecked = minLevel == Level.WARN,
                            onChecked = { minLevel = Level.WARN },
                        )
                        RiftRadioButtonWithLabel(
                            label = "Error",
                            isChecked = minLevel == Level.ERROR,
                            onChecked = { minLevel = Level.ERROR },
                        )
                        RiftCheckboxWithLabel(
                            label = "Autoscroll",
                            isChecked = isAutoScrolling,
                            onCheckedChange = { isAutoScrolling = it },
                        )
                    }
                    LogsView(state, minLevel, isAutoScrolling)
                }
            }
            DebugTab.Network -> {
                Column(
                    modifier = Modifier.padding(top = Spacing.medium),
                ) {
                    Text(
                        text = "Network requests per second",
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    val now = getNow()
                    Box(Modifier.animateContentSize().weight(1f)) {
                        NetworkChart(
                            buckets = state.buckets,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(Modifier.height(Spacing.large))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.animateContentSize(),
                    ) {
                        val oldestEpochSecond = now.epochSecond - RequestStatisticsInterceptor.MAX_BUCKETS
                        val requests = state.buckets
                            .flatMap { it.requests }
                            .takeLastWhile { it.timestamp.epochSecond >= oldestEpochSecond }
                        val inflightRequests = requests.filter { it.response == null }

                        Text(
                            text = buildAnnotatedString {
                                append("Requests by originating feature in the last 2 minutes: ")
                                withStyle(RiftTheme.typography.headerPrimary.copy(fontWeight = FontWeight.Bold).toSpanStyle()) {
                                    append(formatNumber(requests.size))
                                    val failuresCount = requests.count { it.response?.isSuccess == false }
                                    if (failuresCount > 0) {
                                        append(" ")
                                        withColor(EveColors.dangerRed) {
                                            append("($failuresCount)")
                                        }
                                    }
                                }
                            },
                            style = RiftTheme.typography.headerPrimary,
                        )
                        Divider(color = RiftTheme.colors.divider)
                        if (requests.isNotEmpty()) {
                            requests.groupBy { it.originator }.entries.sortedByDescending { it.value.size }.forEach { (originator, requests) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, RiftTheme.colors.borderGreyLight)
                                            .size(16.dp)
                                            .background(originator.color),
                                    ) {}
                                    Text(
                                        text = buildAnnotatedString {
                                            append("${originator.name}: ")
                                            withStyle(RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold).toSpanStyle()) {
                                                append(formatNumber(requests.size))
                                            }
                                        },
                                        style = RiftTheme.typography.bodyPrimary,
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.large),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                        modifier = Modifier.padding(start = Spacing.medium),
                                    ) {
                                        requests.groupBy { it.endpoint }.entries.sortedByDescending { it.value.size }.forEach { (endpoint, requests) ->
                                            Text(
                                                text = buildAnnotatedString {
                                                    append("$endpoint: ")
                                                    withStyle(RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold).toSpanStyle()) {
                                                        append(formatNumber(requests.size))
                                                        val failuresCount = requests.count { it.response?.isSuccess == false }
                                                        if (failuresCount > 0) {
                                                            append(" ")
                                                            withColor(EveColors.dangerRed) {
                                                                append("($failuresCount)")
                                                            }
                                                        }
                                                    }
                                                },
                                                style = RiftTheme.typography.bodySecondary,
                                            )
                                        }
                                    }
                                }
                                Divider(color = RiftTheme.colors.divider)
                            }
                        } else {
                            Text(
                                text = "–",
                                style = RiftTheme.typography.bodyPrimary,
                            )
                            Divider(color = RiftTheme.colors.divider)
                        }

                        Text(
                            text = buildAnnotatedString {
                                append("In-flight requests: ")
                                withColor(RiftTheme.colors.textHighlighted) {
                                    append("${inflightRequests.size}")
                                }
                            },
                            style = RiftTheme.typography.headerPrimary,
                        )
                        if (inflightRequests.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.large),
                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            ) {
                                inflightRequests.groupBy { it.endpoint }.entries.sortedByDescending { it.value.size }.forEach { (endpoint, requests) ->
                                    Text(
                                        text = buildAnnotatedString {
                                            append("$endpoint: ")
                                            withColor(RiftTheme.colors.textHighlighted) {
                                                append("${requests.size}")
                                            }
                                        },
                                        style = RiftTheme.typography.bodyPrimary,
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "–",
                                style = RiftTheme.typography.bodyPrimary,
                            )
                        }
                    }
                }
            }
            DebugTab.RateLimits -> {
                Column(
                    modifier = Modifier.padding(top = Spacing.medium),
                ) {
                    Text(
                        text = "Remaining tokens per rate limit bucket",
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(Spacing.medium))
                    val now = getNow()
                    var previousTokens by remember { mutableStateOf(mapOf<BucketKey, Int>()) }
                    val buckets = remember(now) {
                        state.rateLimitBuckets.mapValues { (bucketKey, bucket) ->
                            val spentTokens = state.spentTokens[bucketKey] ?: emptyList()
                            val tokensRegeneratedSinceLastRequest = spentTokens
                                .takeWhile { it.returnTimestamp.isBefore(now) }
                                .sumOf { it.tokens }
                            val tokensRemaining = bucket.remaining + tokensRegeneratedSinceLastRequest
                            bucket.copy(remaining = tokensRemaining)
                        }
                    }
                    val sortedBuckets = buckets.entries.toList()
                        .sortedBy { (_, bucket) -> bucket.remaining / bucket.limit.tokens.toDouble() }

                    ScrollbarLazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        items(sortedBuckets, key = { it.key }) { (bucketKey, bucket) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                modifier = Modifier.animateItem(),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                    modifier = Modifier.widthIn(min = 200.dp),
                                ) {
                                    Text(
                                        text = buildAnnotatedString {
                                            append("Group ")
                                            withColor(RiftTheme.colors.textPrimary) {
                                                append(bucketKey.group.name)
                                            }
                                        },
                                        style = RiftTheme.typography.bodySecondary,
                                    )
                                    if (bucketKey.character != null) {
                                        RiftTooltipArea(
                                            text = "Bucket for this character",
                                        ) {
                                            DynamicCharacterPortraitParallax(
                                                characterId = bucketKey.character.id,
                                                size = 32.dp,
                                                enterTimestamp = null,
                                                pointerInteractionStateHolder = null,
                                                modifier = Modifier
                                                    .border(1.dp, RiftTheme.colors.borderGreyLight),
                                            )
                                        }
                                    } else {
                                        RiftTooltipArea(
                                            text = "Bucket for your IP address.\nShared with other apps on your PC.",
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .border(1.dp, RiftTheme.colors.borderGreyLight)
                                                    .background(RiftTheme.colors.backgroundPrimaryDark)
                                                    .size(32.dp),
                                            ) {
                                                Text(
                                                    text = "IP",
                                                    style = RiftTheme.typography.bodyPrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .height(IntrinsicSize.Min)
                                        .weight(1f),
                                ) {
                                    val spentTokens = state.spentTokens[bucketKey] ?: emptyList()
                                    val tokensRegeneratedSinceLastRequest = spentTokens
                                        .takeWhile { it.returnTimestamp.isBefore(now) }
                                        .sumOf { it.tokens }
                                    val tokensRemaining = (bucket.remaining + tokensRegeneratedSinceLastRequest).coerceAtMost(bucket.limit.tokens)
                                    val previousTokensRemaining = previousTokens[bucketKey] ?: 0
                                    previousTokens = previousTokens + (bucketKey to tokensRemaining)

                                    val defaultColor = RiftTheme.colors.primary
                                    fun getTargetColor(): Color {
                                        return if (previousTokensRemaining > tokensRemaining) {
                                            EveColors.hotRed
                                        } else if (previousTokensRemaining < tokensRemaining) {
                                            EveColors.successGreen
                                        } else {
                                            defaultColor
                                        }
                                    }

                                    var targetColor by remember { mutableStateOf(getTargetColor()) }
                                    LaunchedEffect(tokensRemaining) {
                                        targetColor = getTargetColor()
                                        delay(1000)
                                        targetColor = defaultColor
                                    }
                                    val color by animateColorAsState(targetColor.copy(alpha = 0.3f))
                                    RiftProgressBar(
                                        percentage = tokensRemaining / bucket.limit.tokens.toFloat(),
                                        color = color,
                                        hasInitialAnimation = false,
                                        modifier = Modifier
                                            .height(20.dp)
                                            .fillMaxWidth(),
                                    )
                                    val nextReturnIn = spentTokens.firstOrNull { it.returnTimestamp.isAfter(now) }?.returnTimestamp?.let { Duration.between(now, it) }
                                    val returnText = if (nextReturnIn != null) {
                                        val minutes = nextReturnIn.toMinutes()
                                        val seconds = nextReturnIn.toSecondsPart()
                                        if (minutes > 0) ", ${minutes}m ${seconds}s" else ", ${seconds}s"
                                    } else {
                                        ""
                                    }
                                    Text(
                                        text = "$tokensRemaining / ${bucket.limit.tokens} tokens$returnText",
                                        style = RiftTheme.typography.bodyPrimary,
                                        modifier = Modifier.align(Alignment.Center),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            DebugTab.Cache -> {
                Column(
                    modifier = Modifier.padding(top = Spacing.medium),
                ) {
                    Text(
                        text = "Cache outcomes per endpoint",
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(Spacing.medium))
                    CacheLegend(name = "Local Hit", text = "Request used a locally cached response without accessing the network")
                    CacheLegend(name = "304 Hit", text = "Request used a locally cached response after the server confirmed from cache it was not modified")
                    CacheLegend(name = "304 Miss", text = "Request used a locally cached response after the server confirmed it was not modified")
                    CacheLegend(name = "Hit", text = "Request received a cached response from the server")
                    CacheLegend(name = "Miss", text = "Request received a fresh response from the server")
                    CacheLegend(name = "Expired", text = "Request received a fresh response from the server, because the server cache has expired")
                    CacheLegend(name = "Dynamic", text = "Request received a fresh response from the server, because the response could not be cached")
                    CacheLegend(name = "Revalidated", text = "Request received a cached response from the server, after the server rechecked it's validity")
                    Spacer(Modifier.height(Spacing.medium))

                    data class CacheOutcomes(
                        val endpoint: Endpoint,
                        val outcomes: Map<CacheStatus, Map<Int?, Int>>,
                    )
                    Spacer(Modifier.height(Spacing.medium))
                    val grouped = state.cacheRequests.entries
                        .groupBy { it.key.endpoint }
                        .map { (endpoint, entries) ->
                            val outcomes = entries.groupBy { it.key.cacheStatus }.map { (cacheStatus, entries) ->
                                val responseStatusToCount = entries.associate {
                                    val responseStatus = it.key.responseStatus
                                    val count = it.value
                                    responseStatus to count
                                }
                                cacheStatus to responseStatusToCount
                            }.toMap()
                            CacheOutcomes(
                                endpoint = endpoint,
                                outcomes = outcomes,
                            )
                        }
                        .sortedByDescending { it.outcomes.entries.sumOf { it.value.entries.sumOf { it.value } } }
                    ScrollbarLazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        items(grouped, key = { it.endpoint }) { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                modifier = Modifier
                                    .height(IntrinsicSize.Max)
                                    .animateItem(),
                            ) {
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier
                                        .border(1.dp, RiftTheme.colors.borderGrey)
                                        .padding(Spacing.medium)
                                        .fillMaxHeight()
                                        .width(200.dp),
                                ) {
                                    Text(
                                        text = "${item.endpoint}",
                                        style = RiftTheme.typography.bodyPrimary,
                                    )
                                }

                                CacheStatus.entries.forEach { cacheStatus ->
                                    val countByStatus = item.outcomes[cacheStatus] ?: emptyMap()
                                    val name = when (cacheStatus) {
                                        CacheStatus.LocalCacheHit -> "Local Hit"
                                        CacheStatus.EsiCacheHitNotModified -> "304 Hit"
                                        CacheStatus.EsiCacheMissNotModified -> "304 Miss"
                                        CacheStatus.EsiCacheHit -> "Hit"
                                        CacheStatus.EsiCacheMiss -> "Miss"
                                        CacheStatus.EsiDynamic -> "Dynamic"
                                        CacheStatus.EsiRevalidated -> "Revalidate"
                                        CacheStatus.EsiExpired -> "Expired"
                                        CacheStatus.EsiNull -> "Unknown"
                                        CacheStatus.Unknown -> "Other"
                                    }
                                    val isAlwaysShown = cacheStatus in listOf(CacheStatus.LocalCacheHit, CacheStatus.EsiCacheHitNotModified, CacheStatus.EsiCacheMissNotModified, CacheStatus.EsiCacheHit, CacheStatus.EsiCacheMiss)
                                    CacheCounter(name, countByStatus, isAlwaysShown)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheCounter(name: String, countByStatus: Map<Int?, Int>, isAlwaysShown: Boolean) {
    val totalCount = countByStatus.values.sum()
    if (totalCount == 0 && !isAlwaysShown) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (totalCount > 0) 1f else 0.3f)
            .border(1.dp, RiftTheme.colors.borderPrimaryDark)
            .background(RiftTheme.colors.backgroundPrimaryDark)
            .padding(Spacing.medium)
            .widthIn(min = 80.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            if (countByStatus.size > 1) {
                countByStatus.forEach { (status, count) ->
                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = formatNumber(count),
                            style = RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "${status ?: "None"}",
                            style = RiftTheme.typography.detailSecondary,
                        )
                    }
                }
            } else if (countByStatus.size == 1) {
                countByStatus.entries.single().let { (status, count) ->
                    Text(
                        text = formatNumber(count),
                        style = RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold),
                    )
                }
            } else {
                Text(
                    text = "0",
                    style = RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        Text(
            text = name,
            style = RiftTheme.typography.bodySecondary,
        )
    }
}

@Composable
private fun CacheLegend(name: String, text: String) {
    Text(
        text = buildAnnotatedString {
            withColor(RiftTheme.colors.textPrimary) {
                append(name)
            }
            append(" – ")
            withColor(RiftTheme.colors.textSecondary) {
                append(text)
            }
        },
        style = RiftTheme.typography.bodySecondary,
    )
}

private val Boolean.connected: String get() = if (this) "connected" else "disconnected"

@Composable
private fun LogsView(
    state: UiState,
    minLevel: Level,
    isAutoScrolling: Boolean,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.events) {
        if (isAutoScrolling) {
            state.events.lastIndex.takeIf { it > -1 }?.let {
                listState.scrollToItem(it)
            }
        }
    }

    SelectionContainer {
        ScrollbarLazyColumn(
            listState = listState,
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            val filtered = state.events.filter { it.level.isGreaterOrEqual(minLevel) }
            items(filtered) { event ->
                val pointerState = remember { PointerInteractionStateHolder() }
                val background by pointerState.animateBackgroundHover()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier
                        .pointerInteraction(pointerState)
                        .background(background)
                        .fillMaxWidth(),
                ) {
                    EventMetadata(state, event, pointerState)
                    val style = when (event.level) {
                        Level.TRACE -> RiftTheme.typography.bodySecondary
                        Level.DEBUG -> RiftTheme.typography.bodySecondary
                        Level.WARN -> RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.awayYellow)
                        Level.ERROR -> RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.hotRed)
                        else -> RiftTheme.typography.bodyPrimary
                    }
                    Text(
                        text = event.message,
                        style = style,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventMetadata(
    state: UiState,
    event: ILoggingEvent,
    pointerState: PointerInteractionStateHolder,
) {
    val background by pointerState.animateWindowBackgroundSecondaryHover()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .heightIn(min = 24.dp)
            .border(1.dp, RiftTheme.colors.borderGrey)
            .background(background),
    ) {
        val time = ZonedDateTime.ofInstant(event.instant, state.displayTimezone).toLocalTime()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formattedTime = formatter.format(time)
        Text(
            text = formattedTime,
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(Spacing.small),
        )
        VerticalDivider(color = RiftTheme.colors.borderGrey)
        Text(
            text = event.level.toString(),
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(Spacing.small),
        )
        VerticalDivider(color = RiftTheme.colors.borderGrey)
        Text(
            text = event.loggerName.substringAfterLast("."),
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(Spacing.small),
        )
    }
}
