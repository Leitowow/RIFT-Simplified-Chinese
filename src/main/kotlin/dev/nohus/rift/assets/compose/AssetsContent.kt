package dev.nohus.rift.assets.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.assets.AssetsFilters
import dev.nohus.rift.assets.AssetsRepository
import dev.nohus.rift.assets.AssetsViewModel.Asset
import dev.nohus.rift.assets.AssetsViewModel.AssetLocation
import dev.nohus.rift.assets.AssetsViewModel.FitAction
import dev.nohus.rift.assets.AssetsViewModel.LoadedData
import dev.nohus.rift.assets.AssetsViewModel.SortType
import dev.nohus.rift.assets.AssetsViewModel.UiState
import dev.nohus.rift.assets.FittingController.Fitting
import dev.nohus.rift.assets.LocationFlags
import dev.nohus.rift.assets.getTotalPrice
import dev.nohus.rift.assets.getTotalVolume
import dev.nohus.rift.compose.AsyncCharacterPortrait
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.EntityInteractionProvider
import dev.nohus.rift.compose.ExpandChevron
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuArea
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.fadingRightEdge
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.corphangar
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.goaldeliveries
import dev.nohus.rift.generated.resources.menu_hide
import dev.nohus.rift.generated.resources.menu_pinned
import dev.nohus.rift.generated.resources.menu_unhide
import dev.nohus.rift.generated.resources.menu_unpin
import dev.nohus.rift.map.SecurityColors
import dev.nohus.rift.repositories.IdRanges
import dev.nohus.rift.settings.persistence.LocationPinStatus
import dev.nohus.rift.utils.formatIskCompact
import dev.nohus.rift.utils.formatNumberCompact
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.roundSecurity
import dev.nohus.rift.utils.withColor
import org.jetbrains.compose.resources.painterResource
import java.text.NumberFormat

@Composable
fun AssetsContent(
    state: UiState,
    data: LoadedData,
    onFiltersUpdate: (AssetsFilters) -> Unit,
    onFitAction: (Fitting, FitAction) -> Unit,
    onPinChange: (Long, LocationPinStatus) -> Unit,
    onRenameClick: (locationId: Long) -> Unit,
    onReloadClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.padding(bottom = Spacing.medium),
        ) {
            RiftDropdownWithLabel(
                label = "Sort By",
                items = SortType.entries,
                selectedItem = state.filters.sort,
                onItemSelected = { onFiltersUpdate(state.filters.copy(sort = it)) },
                getItemName = {
                    when (it) {
                        SortType.Distance -> "Distance"
                        SortType.Name -> "Name"
                        SortType.Count -> "Asset count"
                        SortType.Price -> "Total price"
                    }
                },
            )
            Spacer(Modifier.weight(1f))
            RiftSearchField(
                search = state.filters.search,
                isCompact = false,
                onSearchChange = { onFiltersUpdate(state.filters.copy(search = it.takeIf { it.isNotBlank() })) },
            )
        }

        var expandedLocations by remember { mutableStateOf<Set<AssetLocation>>(emptySet()) }
        var expandedItems by remember { mutableStateOf<Set<Long>>(emptySet()) }
        var isHiddenExpanded by remember { mutableStateOf(false) }
        ScrollbarLazyColumn {
            data.assetTotals?.let { totals ->
                item(key = "totals") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RiftTheme.colors.windowBackgroundSecondary)
                            .padding(start = 24.dp)
                            .padding(vertical = Spacing.small),
                    ) {
                        val text = buildAnnotatedString {
                            append("Total: ")
                            append("${totals.locations} Location${totals.locations.plural}")
                            append(" - ")
                            append("${totals.items} Item${totals.items.plural}")
                            append(" - ")
                            append(formatIskCompact(totals.price))
                            append(" - ")
                            append(formatNumberCompact(totals.volume) + " m3")
                        }
                        Text(
                            text = text,
                            style = RiftTheme.typography.bodySecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false,
                            modifier = Modifier.clipToBounds(),
                        )
                    }
                }
            }
            var previousPinStatus: LocationPinStatus? = null
            data.filteredAssets.forEach { (location, assets) ->
                val pinStatus = state.pins[location.locationId] ?: LocationPinStatus.None
                if (previousPinStatus != pinStatus && pinStatus == LocationPinStatus.Hidden) {
                    item(key = "hidden-location-${location.locationId}") {
                        HiddenLocationsHeader(
                            isExpanded = isHiddenExpanded,
                            hiddenCount = data.filteredAssets.count { state.pins[it.first.locationId] == LocationPinStatus.Hidden },
                            onClick = { isHiddenExpanded = !isHiddenExpanded },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                previousPinStatus = pinStatus

                if (isHiddenExpanded || pinStatus != LocationPinStatus.Hidden) {
                    val isLocationExpanded = location in expandedLocations
                    item(key = location.locationId) {
                        LocationHeader(
                            location = location,
                            assets = assets,
                            isExpanded = isLocationExpanded,
                            expandedItems = expandedItems,
                            depth = if (pinStatus == LocationPinStatus.Hidden) 1 else 0,
                            pinStatus = pinStatus,
                            onClick = {
                                if (isLocationExpanded) expandedLocations -= location else expandedLocations += location
                            },
                            onItemClick = { itemId ->
                                if (itemId in expandedItems) expandedItems -= itemId else expandedItems += itemId
                            },
                            onFitAction = onFitAction,
                            onPinChange = { onPinChange(location.locationId, it) },
                            onRenameClick = onRenameClick,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            item(key = { "footer" }) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.animateItem(),
                ) {
                    if (data.filteredAssets.isEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.medium),
                        ) {
                            val isFiltering = state.filters.search != null || state.filters.ownerTypes.isNotEmpty()
                            val text = if (isFiltering) {
                                "All assets filtered out"
                            } else {
                                "No assets"
                            }
                            Text(
                                text = text,
                                style = RiftTheme.typography.displaySecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.large),
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                    ) {
                        if (data.filteredAssets.isNotEmpty()) {
                            Text("Delayed up to 1 hour")
                        }
                        AnimatedContent(
                            state.isLoading,
                            modifier = Modifier
                                .height(36.dp)
                                .padding(start = Spacing.medium),
                        ) {
                            if (it) {
                                LoadingSpinner(
                                    modifier = Modifier.size(36.dp),
                                )
                            } else {
                                RiftButton(
                                    text = "Reload",
                                    type = ButtonType.Secondary,
                                    onClick = onReloadClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HiddenLocationsHeader(
    isExpanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RiftTheme.colors.windowBackgroundSecondary)
            .hoverBackground()
            .padding(vertical = Spacing.small)
            .onClick { onClick() },
    ) {
        ExpandChevron(isExpanded = isExpanded)
        Image(
            painter = painterResource(Res.drawable.menu_unhide),
            contentDescription = null,
            modifier = Modifier
                .padding(end = Spacing.small)
                .size(16.dp),
        )
        Text(
            text = "Hidden [$hiddenCount]",
            style = RiftTheme.typography.bodyPrimary,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            modifier = Modifier
                .weight(1f)
                .clipToBounds()
                .fadingRightEdge(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationHeader(
    location: AssetLocation,
    assets: List<Asset>,
    isExpanded: Boolean,
    expandedItems: Set<Long>,
    depth: Int,
    pinStatus: LocationPinStatus,
    onClick: () -> Unit,
    onItemClick: (itemId: Long) -> Unit,
    onFitAction: (Fitting, FitAction) -> Unit,
    onPinChange: (LocationPinStatus) -> Unit,
    onRenameClick: (locationId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        val contextMenuItems = buildList {
            add(ContextMenuItem.DividerItem)
            if (pinStatus == LocationPinStatus.Pinned) {
                add(
                    ContextMenuItem.TextItem(
                        text = "Unpin in Assets",
                        iconResource = Res.drawable.menu_unpin,
                        onClick = { onPinChange(LocationPinStatus.None) },
                    ),
                )
            } else if (pinStatus == LocationPinStatus.Hidden) {
                add(
                    ContextMenuItem.TextItem(
                        text = "Unhide in Assets",
                        iconResource = Res.drawable.menu_unhide,
                        onClick = { onPinChange(LocationPinStatus.None) },
                    ),
                )
            } else {
                add(
                    ContextMenuItem.TextItem(
                        text = "Pin in Assets",
                        iconResource = Res.drawable.menu_pinned,
                        onClick = { onPinChange(LocationPinStatus.Pinned) },
                    ),
                )
                add(
                    ContextMenuItem.TextItem(
                        text = "Hide in Assets",
                        iconResource = Res.drawable.menu_hide,
                        onClick = { onPinChange(LocationPinStatus.Hidden) },
                    ),
                )
            }
        }
        val interactionProvider: EntityInteractionProvider = remember { koin.get() }
        val locationContextMenuItems = if (location.systemId != null) {
            interactionProvider.getLocation(
                systemId = location.systemId,
                locationId = location.locationId,
                locationTypeId = location.locationTypeId,
                locationName = location.name,
            ).contextMenuItems
        } else {
            emptyList()
        }
        RiftContextMenuArea(
            items = locationContextMenuItems + contextMenuItems,
            modifier = Modifier.pointerHoverIcon(PointerIcon(Cursors.pointerInteractive)),
        ) {
            val depthOffset = 16.dp * depth
            val pointerState = remember { PointerInteractionStateHolder() }
            Row(
                modifier = Modifier
                    .pointerInteraction(pointerState)
                    .fillMaxWidth()
                    .background(RiftTheme.colors.windowBackgroundSecondary)
                    .hoverBackground(pointerInteractionStateHolder = pointerState)
                    .onClick { onClick() }
                    .padding(vertical = Spacing.verySmall)
                    .padding(start = depthOffset),
            ) {
                ExpandChevron(isExpanded = isExpanded)
                val text = buildAnnotatedString {
                    location.security?.let {
                        withStyle(style = SpanStyle(color = SecurityColors[it], fontWeight = FontWeight.Bold)) {
                            append(it.roundSecurity().toString())
                        }
                        append(" ")
                    }
                    if (location.isNameAuthoritative) {
                        append(location.name)
                        if (location.customName != null) {
                            withColor(RiftTheme.colors.textSpecialHighlighted) {
                                append(" (${location.customName})")
                            }
                        }
                    } else {
                        append(location.customName ?: location.name)
                    }
                    append(" - ")
                    append("${assets.size} Item${if (assets.size != 1) "s" else ""}")
                    append(" - ")
                    val totalPrice = assets.sumOf { it.getTotalPrice() }
                    append(formatIskCompact(totalPrice))
                    append(" - ")
                    val totalVolume = assets.sumOf { it.getTotalVolume() }
                    append(formatNumberCompact(totalVolume) + " m3")
                    location.distance?.let {
                        append(" - ")
                        append("Route: $it Jump${if (it != 1) "s" else ""}")
                    }
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.bodyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds()
                        .fadingRightEdge(),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(20.dp)
                        .padding(end = Spacing.small),
                ) {
                    if (pointerState.isHovered) {
                        RiftTooltipArea(
                            text = "Rename location",
                        ) {
                            RiftImageButton(
                                resource = Res.drawable.editplanicon,
                                size = 20.dp,
                                onClick = { onRenameClick(location.locationId) },
                            )
                        }
                        when (pinStatus) {
                            LocationPinStatus.Pinned -> {
                                RiftTooltipArea(
                                    text = "Unpin",
                                ) {
                                    RiftImageButton(
                                        resource = Res.drawable.menu_unpin,
                                        size = 16.dp,
                                        onClick = { onPinChange(LocationPinStatus.None) },
                                    )
                                }
                            }
                            LocationPinStatus.Hidden -> {
                                RiftTooltipArea(
                                    text = "Unhide",
                                ) {
                                    RiftImageButton(
                                        resource = Res.drawable.menu_hide,
                                        size = 16.dp,
                                        onClick = { onPinChange(LocationPinStatus.None) },
                                    )
                                }
                            }
                            LocationPinStatus.None -> {
                                RiftTooltipArea(
                                    text = "Hide",
                                ) {
                                    RiftImageButton(
                                        resource = Res.drawable.menu_unhide,
                                        size = 16.dp,
                                        onClick = { onPinChange(LocationPinStatus.Hidden) },
                                    )
                                }
                                RiftTooltipArea(
                                    text = "Pin",
                                ) {
                                    RiftImageButton(
                                        resource = Res.drawable.menu_pinned,
                                        size = 16.dp,
                                        onClick = { onPinChange(LocationPinStatus.Pinned) },
                                    )
                                }
                            }
                        }
                    } else {
                        if (pinStatus == LocationPinStatus.Pinned) {
                            Image(
                                painter = painterResource(Res.drawable.menu_pinned),
                                contentDescription = null,
                                modifier = Modifier
                                    .alpha(0.75f)
                                    .size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(isExpanded) {
            Column {
                assets.forEach { asset ->
                    key(asset.itemId) {
                        AssetRow(
                            asset = asset,
                            expandedItems = expandedItems,
                            depth = depth + 1,
                            onClick = onItemClick,
                            onFitAction = onFitAction,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssetRow(
    asset: Asset,
    expandedItems: Set<Long>,
    depth: Int,
    onClick: (Long) -> Unit,
    onFitAction: (Fitting, FitAction) -> Unit,
) {
    Column {
        val isExpanded = asset.itemId in expandedItems
        val depthOffset = 24.dp * if (asset.children.isNotEmpty()) (depth - 1) else depth
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .hoverBackground()
                .padding(vertical = Spacing.small)
                .padding(start = depthOffset)
                .onClick { onClick(asset.itemId) },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (asset.children.isNotEmpty()) {
                    ExpandChevron(isExpanded = isExpanded)
                }
                AssetIcon(asset)
                Column(
                    modifier = Modifier.padding(start = Spacing.medium),
                ) {
                    val text = buildAnnotatedString {
                        if (asset.type.id == IdRanges.corporationOffice) {
                            asset.name?.takeIf { it.isNotBlank() }?.let {
                                append(it)
                            }
                        } else {
                            asset.name?.takeIf { it.isNotBlank() }?.let {
                                append("$it - ")
                            }
                            append(asset.type.name.trim())
                            asset.type.volume.let { volume ->
                                val formatted = NumberFormat.getNumberInstance().format(asset.quantity * volume)
                                withStyle(style = SpanStyle(color = RiftTheme.colors.textSecondary)) {
                                    append(" - $formatted m3")
                                }
                            }
                        }
                        if (asset.price != null) {
                            withStyle(style = SpanStyle(color = RiftTheme.colors.textSecondary)) {
                                append(" - ${formatIskCompact(asset.price * asset.quantity)}")
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (depth == 1) {
                            val text = when (asset.owner) {
                                is AssetsRepository.AssetOwner.Character -> asset.owner.character.info?.name
                                is AssetsRepository.AssetOwner.Corporation -> asset.owner.corporationName
                            } ?: "Unknown Owner"
                            RiftTooltipArea(
                                text = text,
                                modifier = Modifier.padding(end = Spacing.small),
                            ) {
                                when (asset.owner) {
                                    is AssetsRepository.AssetOwner.Character -> {
                                        AsyncCharacterPortrait(
                                            characterId = asset.owner.character.characterId,
                                            size = 32,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, RiftTheme.colors.borderGrey, CircleShape),
                                        )
                                    }
                                    is AssetsRepository.AssetOwner.Corporation -> {
                                        AsyncCorporationLogo(
                                            corporationId = asset.owner.corporationId,
                                            size = 32,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, RiftTheme.colors.borderGrey, CircleShape),
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = text,
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                    val secondaryText = buildList {
                        val flag = LocationFlags.getName(asset.locationFlag)
                        if (flag != null) {
                            add(flag)
                        }
                        if (asset.quantity > 1) {
                            val formatted = NumberFormat.getIntegerInstance().format(asset.quantity)
                            add("$formatted units")
                        }
                        if (asset.children.isNotEmpty()) {
                            add("${asset.children.size} item${if (asset.children.size != 1) "s" else ""}")
                            val volume = asset.children.map { it.quantity * (it.type.volume) }.sum()
                            val formatted = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(volume)
                            add("$formatted m3")

                            val totalPrice = asset.children.sumOf { it.getTotalPrice() }
                            add(formatIskCompact(totalPrice))
                        }
                    }.joinToString(" - ")
                    if (secondaryText.isNotEmpty()) {
                        Text(
                            text = secondaryText,
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
            }
            AnimatedVisibility(isExpanded && asset.fitting != null) {
                if (asset.fitting != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier
                            .padding(top = Spacing.small)
                            .padding(start = 24.dp),
                    ) {
                        RiftButton(
                            text = "Copy fit",
                            type = ButtonType.Secondary,
                            cornerCut = ButtonCornerCut.BottomLeft,
                            onClick = { onFitAction(asset.fitting, FitAction.Copy) },
                        )
                        RiftButton(
                            text = "Copy fit & cargo",
                            type = ButtonType.Secondary,
                            cornerCut = ButtonCornerCut.None,
                            onClick = { onFitAction(asset.fitting, FitAction.CopyWithCargo) },
                        )
                        RiftButton(
                            text = "View fit",
                            cornerCut = ButtonCornerCut.BottomRight,
                            onClick = { onFitAction(asset.fitting, FitAction.Open) },
                        )
                    }
                }
            }
        }
        AnimatedVisibility(isExpanded) {
            Column {
                asset.children.forEach { child ->
                    AssetRow(
                        asset = child,
                        expandedItems = expandedItems,
                        depth = depth + 1,
                        onClick = onClick,
                        onFitAction = onFitAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetIcon(asset: Asset) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(32.dp),
    ) {
        if (asset.type.id == IdRanges.corporationOffice) {
            if (asset.locationFlag == "CorporationGoalDeliveries") {
                Image(
                    painter = painterResource(Res.drawable.goaldeliveries),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.corphangar),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            AsyncTypeIcon(
                type = asset.type,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
