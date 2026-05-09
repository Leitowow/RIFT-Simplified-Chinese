package dev.nohus.rift.opportunities

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ClickableCorporation
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.OnVisibilityChange
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuPopup
import dev.nohus.rift.compose.RiftOpportunityCard
import dev.nohus.rift.compose.RiftOpportunityCardBottomContent
import dev.nohus.rift.compose.RiftOpportunityCardButton
import dev.nohus.rift.compose.RiftOpportunityCardTopRight.RiftOpportunityCardCorporation
import dev.nohus.rift.compose.RiftOpportunityCardTopRight.RiftOpportunityCardProgressGauge
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftVerticalGlowLine
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyVerticalGrid
import dev.nohus.rift.compose.SharedTransitionAnimatedContent
import dev.nohus.rift.compose.Side
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.TransparentWindowController
import dev.nohus.rift.compose.animatedcontentfixed.animateContentSize
import dev.nohus.rift.compose.getActiveWindowTransitionSpec
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.sharedTransitionElement
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.allcontent
import dev.nohus.rift.generated.resources.bars_sort_ascending_16px
import dev.nohus.rift.generated.resources.contact_corporation
import dev.nohus.rift.generated.resources.contribution_16px
import dev.nohus.rift.generated.resources.corporation_project_state_checkmark_16px
import dev.nohus.rift.generated.resources.corporation_project_state_close_16px
import dev.nohus.rift.generated.resources.corporation_project_state_time_16px
import dev.nohus.rift.generated.resources.corporation_projects_banner
import dev.nohus.rift.generated.resources.expand_less_16px
import dev.nohus.rift.generated.resources.expand_more_16px
import dev.nohus.rift.generated.resources.freelance_banner
import dev.nohus.rift.generated.resources.isk
import dev.nohus.rift.generated.resources.open_window_16px
import dev.nohus.rift.generated.resources.window_opportunities
import dev.nohus.rift.network.esi.models.OpportunityState
import dev.nohus.rift.opportunities.OpportunitiesUtils.getOpportunityCategory
import dev.nohus.rift.opportunities.OpportunitiesUtils.getOpportunityType
import dev.nohus.rift.opportunities.OpportunitiesViewModel.CorporationFilter
import dev.nohus.rift.opportunities.OpportunitiesViewModel.CorporationProjectsStats
import dev.nohus.rift.opportunities.OpportunitiesViewModel.FreelanceJobsStats
import dev.nohus.rift.opportunities.OpportunitiesViewModel.OpportunityLifecycleFilter
import dev.nohus.rift.opportunities.OpportunitiesViewModel.OpportunitySorting
import dev.nohus.rift.opportunities.OpportunitiesViewModel.UiState
import dev.nohus.rift.opportunities.OpportunitiesViewModel.View
import dev.nohus.rift.opportunities.compose.DetailsView
import dev.nohus.rift.opportunities.compose.OpportunityCategoryFilterChips
import dev.nohus.rift.opportunities.compose.SideNavigation
import dev.nohus.rift.utils.formatDateTime
import dev.nohus.rift.utils.formatDuration
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.formatIskCompact
import dev.nohus.rift.utils.formatIskReadable
import dev.nohus.rift.utils.formatNumberCompact
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant

@Composable
fun OpportunitiesWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: OpportunitiesViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Opportunities",
        icon = Res.drawable.window_opportunities,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarContent = { height ->
            ToolbarRow(
                state = state,
                fixedHeight = height,
                onParticipatingFilterChange = viewModel::onParticipatingFilterChange,
            )
        },
        withContentPadding = false,
    ) {
        OpportunitiesWindowContent(
            state = state,
            onLifecycleFilterChange = viewModel::onLifecycleFilterChange,
            onIsFiltersShownChange = viewModel::onIsFiltersShownChange,
            onCategoryFilterChange = viewModel::onCategoryFilterChange,
            onCategoryFilterSet = viewModel::onCategoryFilterSet,
            onSortingChange = viewModel::onSortingChange,
            onSearchChange = viewModel::onSearchChange,
            onCorporationFilterSelect = viewModel::onCorporationFilterSelect,
            onProjectClick = viewModel::onProjectClick,
            onViewInGameClick = viewModel::onViewInGameClick,
            onBackClick = viewModel::onBackClick,
        )
        OnVisibilityChange(viewModel::onVisibilityChange)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun OpportunitiesWindowContent(
    state: UiState,
    onLifecycleFilterChange: (OpportunityLifecycleFilter) -> Unit = {},
    onIsFiltersShownChange: (Boolean) -> Unit,
    onCategoryFilterChange: (OpportunityCategoryFilter) -> Unit,
    onCategoryFilterSet: (OpportunityCategoryFilter?) -> Unit = {},
    onSortingChange: (sorting: OpportunitySorting) -> Unit,
    onSearchChange: (String) -> Unit,
    onCorporationFilterSelect: (CorporationFilter) -> Unit = {},
    onProjectClick: (Opportunity) -> Unit,
    onViewInGameClick: (Opportunity) -> Unit,
    onBackClick: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RiftTheme.colors.borderGrey),
        ) {}

        Row {
            SideNavigation(
                state = state,
                onLifecycleFilterChange = onLifecycleFilterChange,
                onClick = onCategoryFilterSet,
            )

            val gridState: LazyGridState = rememberLazyGridState()
            LaunchedEffect(state.primaryFilter) {
                // Scroll to top when primary navigation filter is changed
                gridState.animateScrollToItem(0)
            }
            SharedTransitionAnimatedContent(
                targetState = state.view,
                contentKey = { it::class },
            ) { view ->
                when (view) {
                    View.ProjectsView -> ProjectsView(
                        state = state,
                        gridState = gridState,
                        onIsFiltersShownChange = onIsFiltersShownChange,
                        onCategoryFilterChange = onCategoryFilterChange,
                        onSortingChange = onSortingChange,
                        onSearchChange = onSearchChange,
                        onCorporationFilterSelect = onCorporationFilterSelect,
                        onProjectClick = onProjectClick,
                        onViewInGameClick = onViewInGameClick,
                    )
                    is View.DetailsView -> DetailsView(
                        opportunity = view.opportunity,
                        onBackClick = onBackClick,
                        onCategoryFilterClick = onCategoryFilterChange,
                        onViewInGameClick = { onViewInGameClick(view.opportunity) },
                    )
                }
            }
        }
    }
}

@Composable
fun ToolbarRow(
    state: UiState,
    fixedHeight: Dp,
    onParticipatingFilterChange: (Boolean) -> Unit,
) {
    val participatingText = if (state.lifecycleFilter == OpportunityLifecycleFilter.Active) {
        "Active"
    } else {
        "Participated"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiftTabBar(
            tabs = listOf(
                Tab(
                    id = 0,
                    title = "Opportunities",
                    isCloseable = false,
                ),
                Tab(
                    id = 1,
                    title = participatingText,
                    isCloseable = false,
                    badge = state.participatingCount.takeIf { it > 0 }?.toString(),
                ),
            ),
            selectedTab = if (state.participatingFilter) 1 else 0,
            onTabSelected = { tab ->
                onParticipatingFilterChange(tab == 1)
            },
            onTabClosed = {},
            withUnderline = false,
            withWideTabs = true,
            fixedHeight = fixedHeight,
        )
    }
}

@Composable
private fun ProjectsView(
    state: UiState,
    gridState: LazyGridState,
    onIsFiltersShownChange: (Boolean) -> Unit,
    onCategoryFilterChange: (OpportunityCategoryFilter) -> Unit,
    onSortingChange: (OpportunitySorting) -> Unit,
    onSearchChange: (String) -> Unit,
    onCorporationFilterSelect: (CorporationFilter) -> Unit,
    onProjectClick: (Opportunity) -> Unit,
    onViewInGameClick: (Opportunity) -> Unit,
) {
    Box {
        val cardOffset = 7.dp
        var currentMaxLineSpan by remember { mutableStateOf(1) }
        ScrollbarLazyVerticalGrid(
            gridState = gridState,
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(start = Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.large - cardOffset),
            scrollbarModifier = Modifier.padding(horizontal = Spacing.small),
            modifier = Modifier
                .clipToBounds()
                .fillMaxSize()
                .padding(bottom = Spacing.large),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Box {
                    Box(Modifier.matchParentSize()) {
                        if (state.primaryFilter == OpportunityCategoryFilter.FreelanceJobs) {
                            Image(
                                painter = painterResource(Res.drawable.freelance_banner),
                                contentDescription = null,
                                alignment = Alignment.TopStart,
                                contentScale = ContentScale.None,
                                modifier = Modifier.height(239.dp),
                            )
                        } else if (state.primaryFilter == OpportunityCategoryFilter.CorporationProjects) {
                            Image(
                                painter = painterResource(Res.drawable.corporation_projects_banner),
                                contentDescription = null,
                                alignment = Alignment.TopStart,
                                contentScale = ContentScale.FillHeight,
                                modifier = Modifier.height(230.dp),
                            )
                        }
                    }
                    Column {
                        state.primaryFilter?.let {
                            PrimaryFilterHeader(it)
                        }
                        if (state.corporationProjectsStats != null) {
                            CorporationProjectsStatsRow(state.corporationProjectsStats)
                        }
                        if (state.freelanceJobsStats != null) {
                            FreelanceJobsStatsRow(state.freelanceJobsStats)
                        }
                    }
                }
            }

            stickyHeader(key = "filters-header") {
                FiltersRow(state, onIsFiltersShownChange, onCategoryFilterChange, onSortingChange, onSearchChange, onCorporationFilterSelect)
            }

            if (state.opportunities != null) {
                if (state.opportunities.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "empty-state") {
                        EmptyState()
                    }
                }

                itemsIndexed(
                    items = state.opportunities,
                    key = { _, it -> it.id },
                    span = { _, _ ->
                        currentMaxLineSpan = maxLineSpan
                        GridItemSpan(1)
                    },
                ) { index, it ->
                    ProjectCard(
                        opportunity = it,
                        onProjectClick = { onProjectClick(it) },
                        onViewInGameClick = { onViewInGameClick(it) },
                        modifier = Modifier
                            .sharedTransitionElement("card-${it.id}")
                            .animateItem()
                            .padding(top = if (index >= currentMaxLineSpan) Spacing.large else 0.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryFilterHeader(primary: OpportunityCategoryFilter) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(top = 32.dp, bottom = 16.dp, start = Spacing.large),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            primary.icon?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = Spacing.medium)
                        .size(16.dp),
                )
            }
            Text(
                text = primary.name,
                style = RiftTheme.typography.displayHighlighted.copy(fontWeight = FontWeight.Bold),
            )
        }
        Text(
            text = primary.description,
            style = RiftTheme.typography.bodyPrimary,
        )
    }
}

@Composable
private fun CorporationProjectsStatsRow(
    stats: CorporationProjectsStats,
) {
    Row(
        modifier = Modifier
            .wrapContentWidth(align = Alignment.Start, unbounded = true)
            .padding(start = Spacing.large - 7.dp, end = Spacing.large, top = 40.dp, bottom = 32.dp),
    ) {
        StatsRowItem(
            value = formatIskCompact(stats.availableToYou),
            text = "Available to you",
            color = if (stats.availableToYou > 0) EveColors.successGreen else RiftTheme.colors.textPrimary,
            tooltip = buildAnnotatedString {
                withStyle(RiftTheme.typography.headlinePrimary.toSpanStyle()) {
                    appendLine(formatIsk(stats.availableToYou, withCents = false))
                }
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    append(formatIskReadable(stats.availableToYou))
                }
            },
        )
        StatsRowItem(
            value = formatIskCompact(stats.availableTotal),
            text = "Available to all members",
            color = if (stats.availableTotal > 0) EveColors.successGreen else RiftTheme.colors.textPrimary,
            tooltip = buildAnnotatedString {
                withStyle(RiftTheme.typography.headlinePrimary.toSpanStyle()) {
                    appendLine(formatIsk(stats.availableTotal, withCents = false))
                }
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    append(formatIskReadable(stats.availableTotal))
                }
            },
        )
        StatsRowItem(
            value = stats.active.toString(),
            text = "Active Project${stats.active.plural}",
            color = EveColors.airTurquoise,
            tooltip = buildAnnotatedString {
                append("A maximum of 100 Projects\ncan be active at once per corporation")
            },
        )
        StatsRowItem(
            value = stats.completedToday.toString(),
            text = "Completed Today",
            color = if (stats.completedToday > 0) EveColors.successGreen else RiftTheme.colors.textPrimary,
            tooltip = buildAnnotatedString {
                append("Number of projects completed\nin the last 24 hours")
            },
        )
        StatsRowItem(
            value = stats.completedThisWeek.toString(),
            text = "Completed This Week",
            color = if (stats.completedThisWeek > 0) EveColors.successGreen else RiftTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun FreelanceJobsStatsRow(
    stats: FreelanceJobsStats,
) {
    Row(
        modifier = Modifier
            .wrapContentWidth(align = Alignment.Start, unbounded = true)
            .padding(start = Spacing.large - 7.dp, end = Spacing.large, top = 40.dp, bottom = 32.dp),
    ) {
        StatsRowItem(
            value = stats.available.toString(),
            text = "Job${stats.available.plural} Available",
            color = if (stats.available > 0) EveColors.airTurquoise else RiftTheme.colors.textPrimary,
        )
        StatsRowItem(
            value = stats.corporations.toString(),
            text = "Corporation${stats.corporations.plural}",
            color = if (stats.corporations > 0) EveColors.airTurquoise else RiftTheme.colors.textPrimary,
        )
        StatsRowItem(
            value = stats.accepted.toString(),
            text = "Job${stats.accepted.plural} Accepted",
            color = if (stats.accepted > 0) EveColors.airTurquoise else RiftTheme.colors.textPrimary,
            tooltip = buildAnnotatedString {
                append("The Freelancing skill determines the maximum number of\nFreelance Jobs that can be accepted simultaneously")
            },
        )
    }
}

@Composable
private fun StatsRowItem(
    value: String,
    text: String,
    suffix: String? = null,
    color: Color,
    tooltip: AnnotatedString? = null,
) {
    val activeWindowTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
    val colorWindowTransitionSpec = getActiveWindowTransitionSpec<Color>()
    val color by activeWindowTransition.animateColor(colorWindowTransitionSpec) {
        if (it) color else RiftTheme.colors.textPrimary
    }
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    RiftTooltipArea(
        text = tooltip,
    ) {
        Row(
            modifier = Modifier
                .pointerInteraction(pointerInteractionStateHolder)
                .padding(end = Spacing.large)
                .height(IntrinsicSize.Max),
        ) {
            RiftVerticalGlowLine(pointerInteractionStateHolder, color, Side.Right)
            Spacer(Modifier.width(Spacing.large))
            Column {
                AnimatedContent(value) { value ->
                    Text(
                        text = buildAnnotatedString {
                            append(value)
                            if (suffix != null) {
                                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                                    append(suffix)
                                }
                            }
                        },
                        style = RiftTheme.typography.displayHighlighted,
                    )
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FiltersRow(
    state: UiState,
    onIsFiltersShownChange: (Boolean) -> Unit,
    onCategoryFilterChange: (OpportunityCategoryFilter) -> Unit,
    onSortingChange: (sorting: OpportunitySorting) -> Unit,
    onSearchChange: (String) -> Unit,
    onCorporationFilterSelect: (CorporationFilter) -> Unit,
) {
    val visibleApplicable = state.categoryFilters.applicable.filter {
        it != state.primaryFilter
    }.toSet()
    val visibleEnabled = state.categoryFilters.enabled.filter { it in visibleApplicable }.toSet()

    val isFiltersVisible = state.isFiltersShown || visibleEnabled.isNotEmpty()

    var isOnTop by remember { mutableStateOf(false) }
    val activeTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
    val transparentWindowController: TransparentWindowController = remember { koin.get() }

    Column(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures { }
            }
            .modifyIf(isOnTop) {
                background(transparentWindowController.getWindowBackgroundColor(activeTransition, isTransparent = false))
            }
            .fillMaxWidth()
            .onGloballyPositioned {
                @Suppress("AssignedValueIsNeverRead")
                isOnTop = it.positionInParent().y == 0f
            }
            .padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.large, top = Spacing.large),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                val count = state.opportunities?.size ?: 0
                Text(
                    text = if (count == 1) "1 Opportunity" else "$count Opportunities",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RiftSearchField(
                    search = state.search,
                    isCompact = false,
                    onSearchChange = onSearchChange,
                )

                RiftButton(
                    text = "Filters",
                    icon = if (state.isFiltersShown) Res.drawable.expand_less_16px else Res.drawable.expand_more_16px,
                    type = ButtonType.Secondary,
                    onClick = { onIsFiltersShownChange(!state.isFiltersShown) },
                )

                fun OpportunitySorting.getName() = when (this) {
                    OpportunitySorting.Name -> "Name"
                    OpportunitySorting.NameReversed -> "Name (Reversed)"
                    OpportunitySorting.DateCreated -> "Date Created"
                    OpportunitySorting.DateCreatedReversed -> "Date Created (Newest first)"
                    OpportunitySorting.TimeRemaining -> "Time Remaining"
                    OpportunitySorting.TimeRemainingReversed -> "Time Remaining (Longest first)"
                    OpportunitySorting.Progress -> "Progress"
                    OpportunitySorting.ProgressReversed -> "Progress (Highest first)"
                    OpportunitySorting.NumberOfJumps -> "Number of Jumps"
                    OpportunitySorting.NumberOfJumpsReversed -> "Number of Jumps (Farthest first)"
                    OpportunitySorting.LastUpdated -> "Last Updated"
                    OpportunitySorting.LastUpdatedReversed -> "Last Updated (Newest first)"
                }

                val sortingFilterItems: List<ContextMenuItem> = OpportunitySorting.entries.map { sorting ->
                    ContextMenuItem.RadioItem(
                        text = sorting.getName(),
                        onClick = { onSortingChange(sorting) },
                        isSelected = sorting == state.sorting,
                    )
                }.chunked(2).flatMap { (a, b) ->
                    listOf(a, b, ContextMenuItem.DividerItem)
                }.dropLast(1).let {
                    listOf(ContextMenuItem.HeaderItem("Sort By")) + it
                }
                Box(contentAlignment = Alignment.BottomStart) {
                    var isShown by remember { mutableStateOf(false) }
                    RiftButton(
                        text = "Sort By",
                        icon = Res.drawable.bars_sort_ascending_16px,
                        type = ButtonType.Secondary,
                        onClick = { isShown = true },
                    )
                    if (isShown) {
                        val offset = with(LocalDensity.current) {
                            32.dp.toPx().toInt()
                        }
                        RiftContextMenuPopup(
                            items = sortingFilterItems,
                            offset = IntOffset(0, offset),
                            onDismissRequest = { isShown = false },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(isFiltersVisible) {
            if (visibleApplicable.isNotEmpty()) {
                val (enabled, disabled) = visibleApplicable.partition { it in visibleEnabled }
                val filters = if (state.isFiltersShown) enabled + disabled else enabled
                OpportunityCategoryFilterChips(
                    filters = filters,
                    enabledFilters = visibleEnabled,
                    onCategoryFilterChange = onCategoryFilterChange,
                    modifier = Modifier.padding(top = Spacing.mediumLarge),
                )
            }
        }

        val isInCorporationProjects = state.primaryFilter == OpportunityCategoryFilter.CorporationProjects
        val corporationFilters = buildList {
            if (state.corporations.isNotEmpty()) {
                if (!isInCorporationProjects) add(CorporationFilter.All)
                if (state.corporations.size > 1) add(CorporationFilter.MemberCorporations)
                addAll(state.corporations.map { CorporationFilter.SpecificCorporation(it) })
            }
        }
        val selectedFilter = state.corporationFilter
            .takeIf { !isInCorporationProjects || it != CorporationFilter.All }
            ?: CorporationFilter.MemberCorporations.takeIf { it in corporationFilters }
            ?: corporationFilters.firstOrNull()
            ?: state.corporationFilter
        if (corporationFilters.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Spacing.mediumLarge),
            ) {
                LazyRow(Modifier.animateContentSize()) {
                    items(corporationFilters.sortedBy { it == selectedFilter }, key = { it }) { filter ->
                        val name = when (filter) {
                            CorporationFilter.All -> "All creators"
                            CorporationFilter.MemberCorporations -> "All your corporations"
                            is CorporationFilter.SpecificCorporation -> filter.corporation.name
                        }
                        RiftTooltipArea(name, modifier = Modifier.animateItem()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.mediumLarge),
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                                    .onClick { onCorporationFilterSelect(filter) },
                            ) {
                                when (filter) {
                                    CorporationFilter.All -> {
                                        Image(
                                            painter = painterResource(Res.drawable.allcontent),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                        )
                                    }
                                    CorporationFilter.MemberCorporations -> {
                                        Image(
                                            painter = painterResource(Res.drawable.contact_corporation),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                        )
                                    }
                                    is CorporationFilter.SpecificCorporation -> {
                                        AsyncCorporationLogo(
                                            corporationId = filter.corporation.id,
                                            size = 64,
                                            modifier = Modifier.size(48.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(Spacing.mediumLarge))
                AnimatedContent(selectedFilter) { filter ->
                    when (filter) {
                        CorporationFilter.All -> {
                            Text(
                                text = "All creators",
                                style = RiftTheme.typography.headlinePrimary.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        CorporationFilter.MemberCorporations -> {
                            Text(
                                text = "All your corporations",
                                style = RiftTheme.typography.headlinePrimary.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        is CorporationFilter.SpecificCorporation -> {
                            ClickableCorporation(filter.corporation.id) {
                                LinkText(
                                    text = filter.corporation.name,
                                    normalStyle = RiftTheme.typography.headlinePrimary.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    hoveredStyle = RiftTheme.typography.headlinePrimary.copy(
                                        color = RiftTheme.colors.textLink,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    hasHoverCursor = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    opportunity: Opportunity,
    onProjectClick: () -> Unit,
    onViewInGameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = getOpportunityCategory(opportunity)
    val type = getOpportunityType(opportunity)

    val bottomContent = getProjectBottomContent(opportunity)

    val progressGauge = RiftOpportunityCardProgressGauge(
        currentProgress = opportunity.currentProgress,
        desiredProgress = opportunity.desiredProgress,
        ownProgress = opportunity.contributions.sumOf { it.contribution.success ?: 0 },
        participationLimit = opportunity.details.participationLimit,
        state = opportunity.state,
    )
    val topRift = when (opportunity.type) {
        OpportunityType.CorporationProject -> progressGauge
        OpportunityType.FreelanceJob -> RiftOpportunityCardCorporation(
            name = opportunity.creator.corporation.name,
            id = opportunity.creator.corporation.id,
            progressGauge = progressGauge,
        )
    }
    val buttons = buildList {
        add(
            RiftOpportunityCardButton(
                resource = Res.drawable.open_window_16px,
                isAlwaysVisible = false,
                tooltip = "View In-Game",
                action = onViewInGameClick,
            ),
        )
        if (opportunity.contributions.isNotEmpty()) {
            val verb = if (opportunity.state == OpportunityState.Active) "are" else "were"
            val noun = when (opportunity.type) {
                OpportunityType.CorporationProject -> "project"
                OpportunityType.FreelanceJob -> "job"
            }
            add(
                RiftOpportunityCardButton(
                    resource = Res.drawable.contribution_16px,
                    isAlwaysVisible = true,
                    colorTint = Color(0xFFA9DBE9),
                    tooltip = "You $verb a participant in this $noun",
                    action = null,
                ),
            )
        }
        if (opportunity.details.expires != null && opportunity.state == OpportunityState.Active) {
            val expiresIn = Duration.between(getNow(), opportunity.details.expires).coerceAtLeast(Duration.ZERO)
            val color = when {
                expiresIn < Duration.ofHours(6) -> EveColors.dangerRed
                expiresIn < Duration.ofDays(1) -> EveColors.warningOrange
                else -> null
            }
            add(
                RiftOpportunityCardButton(
                    resource = Res.drawable.corporation_project_state_time_16px,
                    colorTint = color,
                    tooltipContent = {
                        Text(
                            text = buildAnnotatedString {
                                if (expiresIn.isPositive) {
                                    append("Expires in ")
                                    withColor(RiftTheme.colors.textPrimary) {
                                        append(formatDuration(expiresIn))
                                    }
                                } else {
                                    withColor(EveColors.dangerRed) {
                                        append("Expired")
                                    }
                                }
                            },
                            style = RiftTheme.typography.bodySecondary,
                            modifier = Modifier.padding(Spacing.large),
                        )
                    },
                    action = null,
                ),
            )
        }
    }

    RiftOpportunityCard(
        category = category,
        type = type,
        solarSystemChipState = opportunity.details.solarSystemChipState,
        topRight = topRift,
        bottomContent = bottomContent,
        buttons = buttons,
        isEnforcingHeight = true,
        onClick = onProjectClick,
        modifier = modifier,
    ) {
        Text(
            text = opportunity.name,
            style = RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = Spacing.mediumLarge),
        )
    }
}

@Composable
private fun getProjectBottomContent(opportunity: Opportunity): RiftOpportunityCardBottomContent = when (opportunity.state) {
    OpportunityState.Active, OpportunityState.Unspecified -> {
        if (opportunity.reward != null) {
            val totalRemainingRewardForCharacters = if (opportunity.details.rewardPerContribution != null && opportunity.details.participationLimit != null) {
                val remainingRewardPerCharacter = opportunity.eligibleCharacters.map { character ->
                    val characterContribution = opportunity.contributions.find { it.characterId == character.characterId }
                    val characterProgress = (characterContribution?.contribution?.success ?: 0).coerceAtMost(opportunity.details.participationLimit)
                    val characterRemainingProgress = (opportunity.details.participationLimit - characterProgress).coerceIn(0, opportunity.details.participationLimit)
                    val characterRemainingReward = opportunity.details.rewardPerContribution * characterRemainingProgress
                    character to characterRemainingReward
                }
                remainingRewardPerCharacter.sumOf { it.second }.coerceAtMost(opportunity.reward.remaining)
            } else {
                null
            }

            val bottomText = buildAnnotatedString {
                if (totalRemainingRewardForCharacters != null) {
                    append(formatIsk(totalRemainingRewardForCharacters, withCents = false))
                    append(" ")
                    withColor(RiftTheme.colors.textSecondary) {
                        append("(${formatNumberCompact(opportunity.reward.remaining)})")
                    }
                } else {
                    append(formatIsk(opportunity.reward.remaining, withCents = false))
                }
            }
            val bottomTextTooltip = buildAnnotatedString {
                if (totalRemainingRewardForCharacters != null) {
                    withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                        appendLine("Available to you")
                    }
                    withStyle(RiftTheme.typography.headerPrimary.toSpanStyle()) {
                        appendLine(formatIsk(totalRemainingRewardForCharacters, withCents = false))
                    }
                    withStyle(RiftTheme.typography.detailDisabled.toSpanStyle()) {
                        appendLine(formatIskReadable(totalRemainingRewardForCharacters))
                    }
                    appendLine()
                }

                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    appendLine("Available in Project")
                }
                withStyle(RiftTheme.typography.headerPrimary.toSpanStyle()) {
                    appendLine(formatIsk(opportunity.reward.remaining, withCents = false))
                }
                withStyle(RiftTheme.typography.detailDisabled.toSpanStyle()) {
                    append(formatIskReadable(opportunity.reward.remaining))
                }
            }
            RiftOpportunityCardBottomContent.Text(Res.drawable.isk, bottomText, bottomTextTooltip)
        } else {
            RiftOpportunityCardBottomContent.None
        }
    }

    OpportunityState.Closed -> {
        getProjectBottomText(
            text = "Closed",
            color = EveColors.hotRed,
            icon = Res.drawable.corporation_project_state_close_16px,
            timestamp = opportunity.details.finished ?: opportunity.lastModified,
        )
    }

    OpportunityState.Completed -> {
        getProjectBottomText(
            text = "Completed",
            color = EveColors.successGreen,
            icon = Res.drawable.corporation_project_state_checkmark_16px,
            timestamp = opportunity.details.finished ?: opportunity.lastModified,
        )
    }

    OpportunityState.Expired -> {
        getProjectBottomText(
            text = "Expired",
            color = EveColors.hotRed,
            icon = Res.drawable.corporation_project_state_time_16px,
            timestamp = opportunity.details.finished ?: opportunity.lastModified,
        )
    }

    OpportunityState.Deleted -> {
        getProjectBottomText(
            text = "Deleted",
            color = EveColors.hotRed,
            icon = Res.drawable.corporation_project_state_close_16px,
            timestamp = opportunity.details.finished ?: opportunity.lastModified,
        )
    }
}

@Composable
private fun getProjectBottomText(
    text: String,
    color: Color,
    icon: DrawableResource,
    timestamp: Instant?,
): RiftOpportunityCardBottomContent.Text {
    val bottomText = buildAnnotatedString {
        withColor(color) {
            append(text)
        }
        timestamp?.let {
            withColor(RiftTheme.colors.textSecondary) {
                append(" ")
                append(formatDateTime(it))
            }
        }
    }
    return RiftOpportunityCardBottomContent.Text(icon, bottomText)
}

@Composable
private fun EmptyState() {
    Text(
        text = "No Opportunities found",
        style = RiftTheme.typography.headlineSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}
