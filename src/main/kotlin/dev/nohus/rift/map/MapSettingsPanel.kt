package dev.nohus.rift.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftAutocompleteTextField
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftPill
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.backicon
import dev.nohus.rift.generated.resources.expand_more_16px
import dev.nohus.rift.map.DistanceMapController.DistanceMapState
import dev.nohus.rift.map.MapJumpRangeController.MapJumpRangeState
import dev.nohus.rift.map.MapLayoutRepository.Layout
import dev.nohus.rift.map.MapPlanetsController.MapPlanetsState
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.MapViewModel.MapType.ClusterRegionsMap
import dev.nohus.rift.map.MapViewModel.MapType.ClusterSystemsMap
import dev.nohus.rift.map.MapViewModel.MapType.DistanceMap
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.map.MapViewModel.SystemInfoTypes
import dev.nohus.rift.map.PanelState.CellColor
import dev.nohus.rift.map.PanelState.Collapsed
import dev.nohus.rift.map.PanelState.DistanceMapCenter
import dev.nohus.rift.map.PanelState.Expanded
import dev.nohus.rift.map.PanelState.Indicators
import dev.nohus.rift.map.PanelState.InfoBox
import dev.nohus.rift.map.PanelState.JumpRange
import dev.nohus.rift.map.PanelState.Planets
import dev.nohus.rift.map.PanelState.SovereigntyUpgrades
import dev.nohus.rift.map.PanelState.StarColor
import dev.nohus.rift.repositories.PlanetTypes
import dev.nohus.rift.repositories.PlanetTypes.PlanetType
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.MapSystemInfoType
import dev.nohus.rift.sovupgrades.MapSovereigntyUpgradesController.MapSovereigntyUpgradesState
import dev.nohus.rift.sovupgrades.SovereigntyUpgradesRepository
import org.jetbrains.compose.resources.painterResource
import dev.nohus.rift.settings.persistence.MapType as SettingsMapType

enum class PanelState {
    Collapsed,
    Expanded,
    StarColor,
    CellColor,
    Indicators,
    InfoBox,
    JumpRange,
    Planets,
    SovereigntyUpgrades,
    DistanceMapCenter,
}

private val editableInfoTypes = mapOf(
    MapSystemInfoType.JumpRange to JumpRange,
    MapSystemInfoType.Planets to Planets,
    MapSystemInfoType.SovereigntyUpgrades to SovereigntyUpgrades,
)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun MapSettingsPanel(
    hazeState: HazeState,
    mapType: MapType,
    systemInfoTypes: SystemInfoTypes,
    mapJumpRangeState: MapJumpRangeState,
    mapPlanetsState: MapPlanetsState,
    mapSovereigntyUpgradesState: MapSovereigntyUpgradesState,
    distanceMapState: DistanceMapState,
    alternativeLayouts: List<Layout>,
    onSystemColorChange: (SettingsMapType, MapSystemInfoType) -> Unit,
    onSystemColorHover: (SettingsMapType, MapSystemInfoType, Boolean) -> Unit,
    onCellColorChange: (SettingsMapType, MapSystemInfoType?) -> Unit,
    onCellColorHover: (SettingsMapType, MapSystemInfoType?, Boolean) -> Unit,
    onIndicatorChange: (SettingsMapType, MapSystemInfoType) -> Unit,
    onInfoBoxChange: (SettingsMapType, MapSystemInfoType) -> Unit,
    onJumpRangeTargetUpdate: (String) -> Unit,
    onJumpRangeDistanceUpdate: (Double) -> Unit,
    onPlanetTypesUpdate: (List<PlanetType>) -> Unit,
    onSovereigntyUpgradeTypesUpdate: (List<Type>) -> Unit,
    onLayoutSelected: (Int) -> Unit,
    onDistanceMapCenterUpdate: (String) -> Unit,
    onDistanceMapRangeUpdate: (Int) -> Unit,
) {
    val settingsMapType = when (mapType) {
        ClusterRegionsMap -> null
        is ClusterSystemsMap -> SettingsMapType.NewEden
        is RegionMap -> SettingsMapType.Region
        is DistanceMap -> SettingsMapType.Distance
    } ?: return
    Column(
        modifier = Modifier.padding(1.dp),
    ) {
        var previousPanelState: PanelState by remember { mutableStateOf(Collapsed) }
        var panelState: PanelState by remember { mutableStateOf(Collapsed) }
        ScrollbarColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            isScrollbarConditional = true,
            hasScrollbarBackground = false,
            modifier = Modifier
                .heightIn(max = 200.dp)
                .onPointerEvent(PointerEventType.Enter) {
                    if (panelState == Collapsed) panelState = Expanded
                }
                .onPointerEvent(PointerEventType.Exit) {
                    if (panelState == Expanded) panelState = Collapsed
                }
                .hazeChild(hazeState),
        ) {
            AnimatedContent(targetState = panelState) { state ->
                when (state) {
                    Collapsed -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.expand_more_16px),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = Spacing.small)
                                    .size(16.dp),
                            )
                        }
                    }
                    Expanded -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier
                                .padding(Spacing.medium)
                                .fillMaxWidth(),
                        ) {
                            FlowRow(
                                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                maxItemsInEachRow = 2,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                ) {
                                    Text(
                                        text = "星系：",
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                    SystemColorPills(
                                        isExpanded = false,
                                        isCellColor = false,
                                        selected = systemInfoTypes.starSelected[settingsMapType],
                                        onPillClick = {
                                            panelState = StarColor
                                        },
                                        onPillEditClick = {
                                            previousPanelState = panelState
                                            panelState = editableInfoTypes[it]!!
                                        },
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                ) {
                                    Text(
                                        text = "背景：",
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                    SystemColorPills(
                                        isExpanded = false,
                                        isCellColor = true,
                                        selected = systemInfoTypes.cellSelected[settingsMapType],
                                        onPillClick = {
                                            panelState = CellColor
                                        },
                                        onPillEditClick = {
                                            previousPanelState = panelState
                                            panelState = editableInfoTypes[it]!!
                                        },
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                ) {
                                    Text(
                                        text = "指示：",
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                    val text = systemInfoTypes.indicators[settingsMapType].orEmpty().let {
                                        if (it.isEmpty()) "无" else "${it.size} 项已启用"
                                    }
                                    RiftPill(
                                        text = text,
                                        onClick = {
                                            panelState = Indicators
                                        },
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                ) {
                                    Text(
                                        text = "信息框：",
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                    val text = systemInfoTypes.infoBox[settingsMapType].orEmpty().let {
                                        if (it.isEmpty()) "无" else "${it.size} 项已启用"
                                    }
                                    RiftPill(
                                        text = text,
                                        onClick = {
                                            panelState = InfoBox
                                        },
                                    )
                                }
                            }
                            if (mapType is RegionMap) {
                                AlternativeLayoutsPills(
                                    alternativeLayouts = alternativeLayouts,
                                    selectedLayoutId = mapType.layoutId,
                                    onLayoutSelected = onLayoutSelected,
                                )
                            } else if (mapType is DistanceMap) {
                                DistanceMapPills(
                                    state = distanceMapState,
                                    onDistanceMapCenterClick = {
                                        panelState = DistanceMapCenter
                                    },
                                    onDistanceMapRangeClick = {
                                        panelState = DistanceMapCenter
                                    },
                                )
                            }
                        }
                    }
                    StarColor -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = "星系颜色",
                                onBack = { panelState = Expanded },
                            )
                            SystemColorPills(
                                isExpanded = true,
                                isCellColor = false,
                                selected = systemInfoTypes.starSelected[settingsMapType],
                                onPillClick = {
                                    onSystemColorChange(settingsMapType, it!!)
                                    panelState = Expanded
                                },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                                onPillHover = { color, isHovered ->
                                    onSystemColorHover(settingsMapType, color!!, isHovered)
                                },
                            )
                        }
                    }
                    CellColor -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = "星系背景色",
                                onBack = { panelState = Expanded },
                            )
                            SystemColorPills(
                                isExpanded = true,
                                isCellColor = true,
                                selected = systemInfoTypes.cellSelected[settingsMapType],
                                onPillClick = {
                                    onCellColorChange(settingsMapType, it)
                                    panelState = Expanded
                                },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                                onPillHover = { color, isHovered ->
                                    onCellColorHover(settingsMapType, color, isHovered)
                                },
                            )
                        }
                    }
                    Indicators -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = "指示（始终显示）",
                                onBack = { panelState = Expanded },
                            )
                            SystemIndicatorsPills(
                                hidden = setOf(
                                    MapSystemInfoType.StarColor,
                                    MapSystemInfoType.NullSecurity,
                                    MapSystemInfoType.IntelHostiles,
                                    MapSystemInfoType.FactionWarfare,
                                    MapSystemInfoType.RatsType,
                                ),
                                getInfoTypeNames = ::getMapStarInfoTypeIndicatorName,
                                selected = systemInfoTypes.indicators[settingsMapType].orEmpty(),
                                onPillClick = { onIndicatorChange(settingsMapType, it) },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                            )
                        }
                    }
                    InfoBox -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = "信息框详情（悬停显示）",
                                onBack = { panelState = Expanded },
                            )
                            SystemIndicatorsPills(
                                hidden = setOf(
                                    MapSystemInfoType.StarColor,
                                    MapSystemInfoType.NullSecurity,
                                    MapSystemInfoType.IntelHostiles,
                                ),
                                getInfoTypeNames = ::getMapStarInfoTypeInfoBoxName,
                                selected = systemInfoTypes.infoBox[settingsMapType].orEmpty(),
                                onPillClick = { onInfoBoxChange(settingsMapType, it) },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                            )
                        }
                    }
                    JumpRange -> {
                        JumpRangePanel(
                            mapJumpRangeState = mapJumpRangeState,
                            onBack = { panelState = previousPanelState },
                            onJumpRangeTargetUpdate = onJumpRangeTargetUpdate,
                            onJumpRangeDistanceUpdate = onJumpRangeDistanceUpdate,
                        )
                    }
                    Planets -> {
                        PlanetsPanel(
                            mapPlanetsState = mapPlanetsState,
                            onBack = { panelState = previousPanelState },
                            onPlanetTypesUpdate = onPlanetTypesUpdate,
                        )
                    }
                    SovereigntyUpgrades -> {
                        SovereigntyUpgradesPanel(
                            mapSovereigntyUpgradesState = mapSovereigntyUpgradesState,
                            onBack = { panelState = previousPanelState },
                            onSovereigntyUpgradeTypesUpdate = onSovereigntyUpgradeTypesUpdate,
                        )
                    }
                    DistanceMapCenter -> {
                        DistanceMapPanel(
                            state = distanceMapState,
                            onBack = { panelState = Expanded },
                            onDistanceMapCenterUpdate = onDistanceMapCenterUpdate,
                            onDistanceMapRangeUpdate = onDistanceMapRangeUpdate,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp).background(RiftTheme.colors.borderGrey),
        )
    }
}

@Composable
private fun JumpRangePanel(
    mapJumpRangeState: MapJumpRangeState,
    onBack: () -> Unit,
    onJumpRangeTargetUpdate: (String) -> Unit,
    onJumpRangeDistanceUpdate: (Double) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = "旗舰跳跃范围",
            onBack = onBack,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
            val charactersRepository: LocalCharactersRepository = remember { koin.get() }
            var targetText by remember { mutableStateOf("") }

            val suggestions by derivedStateOf {
                val possibleCharacters = charactersRepository.characters.value
                    .mapNotNull { it.info?.name }
                val possibleSystems = solarSystemsRepository.getSystems()
                    .map { it.name }
                (possibleCharacters + possibleSystems)
                    .filter { it.lowercase().startsWith(targetText.lowercase()) }
                    .filter { it.lowercase() != targetText.lowercase() }
            }

            LaunchedEffect(mapJumpRangeState.target) {
                when (mapJumpRangeState.target) {
                    is MapJumpRangeController.MapJumpRangeTarget.Character -> targetText = mapJumpRangeState.target.name
                    is MapJumpRangeController.MapJumpRangeTarget.System -> targetText = mapJumpRangeState.target.name
                    null -> {}
                }
            }

            Text(
                text = "起点：",
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier.padding(end = Spacing.small),
            )
            RiftAutocompleteTextField(
                text = targetText,
                suggestions = suggestions.take(5),
                placeholder = "星系或角色",
                onTextChanged = {
                    targetText = it
                    onJumpRangeTargetUpdate(it)
                },
                modifier = Modifier
                    .width(150.dp),
            )
            AnimatedVisibility(targetText.isNotBlank()) {
                RequirementIcon(
                    isFulfilled = mapJumpRangeState.target != null,
                    fulfilledTooltip = when (mapJumpRangeState.target) {
                        is MapJumpRangeController.MapJumpRangeTarget.Character -> "有效角色"
                        is MapJumpRangeController.MapJumpRangeTarget.System -> "有效星系"
                        null -> ""
                    },
                    notFulfilledTooltip = "未找到该星系或角色",
                )
            }
        }
        val ranges = listOf(
            "6 光年 — 超旗、泰坦" to 6.0,
            "7 光年 — 航母、无畏、力航" to 7.0,
            "8 光年 — 黑隐" to 8.0,
            "10 光年 — 跳货、长须鲸" to 10.0,
        )
        RiftDropdownWithLabel(
            label = "范围：",
            items = ranges,
            selectedItem = ranges.firstOrNull { it.second == mapJumpRangeState.distanceLy } ?: ranges.first(),
            onItemSelected = { onJumpRangeDistanceUpdate(it.second) },
            getItemName = { it.first },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanetsPanel(
    mapPlanetsState: MapPlanetsState,
    onBack: () -> Unit,
    onPlanetTypesUpdate: (List<PlanetType>) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = "行星类型",
            onBack = onBack,
        )
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            PlanetTypes.types.forEach { type ->
                val isSelected = type in mapPlanetsState.selectedTypes
                RiftPill(
                    text = type.name,
                    icon = type.icon,
                    isIconColor = true,
                    isSelected = isSelected,
                    onClick = {
                        val new = if (isSelected) {
                            mapPlanetsState.selectedTypes - type
                        } else {
                            mapPlanetsState.selectedTypes + type
                        }
                        onPlanetTypesUpdate(new)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SovereigntyUpgradesPanel(
    mapSovereigntyUpgradesState: MapSovereigntyUpgradesState,
    onBack: () -> Unit,
    onSovereigntyUpgradeTypesUpdate: (List<Type>) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = "主权升级类型",
            onBack = onBack,
        )
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            val sovereigntyUpgradesRepository: SovereigntyUpgradesRepository = remember { koin.get() }
            sovereigntyUpgradesRepository.groupedUpgradeTypes.forEach { group ->
                val isSelected = group.any { it in mapSovereigntyUpgradesState.selectedTypes }
                val name = group.first().name.replace(Regex("\\s+\\d+$"), "")
                RiftPill(
                    text = name,
                    icon = {
                        Row {
                            for (type in group) {
                                AsyncTypeIcon(
                                    type = type,
                                    modifier = Modifier
                                        .padding(end = Spacing.small)
                                        .size(32.dp),
                                )
                            }
                        }
                    },
                    isSelected = isSelected,
                    onClick = {
                        val new = if (isSelected) {
                            mapSovereigntyUpgradesState.selectedTypes - group
                        } else {
                            mapSovereigntyUpgradesState.selectedTypes + group
                        }
                        onSovereigntyUpgradeTypesUpdate(new)
                    },
                )
            }
        }
    }
}

@Composable
private fun DistanceMapPanel(
    state: DistanceMapState,
    onBack: () -> Unit,
    onDistanceMapCenterUpdate: (String) -> Unit,
    onDistanceMapRangeUpdate: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = "距离图",
            onBack = onBack,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
            val charactersRepository: LocalCharactersRepository = remember { koin.get() }
            var targetText by remember { mutableStateOf("") }
            var isEdited by remember { mutableStateOf(false) }

            val suggestions by derivedStateOf {
                val possibleCharacters = charactersRepository.characters.value
                    .mapNotNull { it.info?.name }
                val possibleSystems = solarSystemsRepository.getSystems()
                    .map { it.name }
                (possibleCharacters + possibleSystems)
                    .filter { it.lowercase().startsWith(targetText.lowercase()) }
                    .filter { it.lowercase() != targetText.lowercase() }
            }

            LaunchedEffect(state.followingCharacterId, state.followingCharacterName, state.centerSystemId, state.centerSystemName) {
                targetText = when {
                    state.followingCharacterId != null -> state.followingCharacterName ?: state.followingCharacterId.toString()
                    else -> state.centerSystemName ?: state.centerSystemId.toString()
                }
            }

            Text(
                text = "中心：",
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier.padding(end = Spacing.small),
            )
            RiftAutocompleteTextField(
                text = targetText,
                suggestions = suggestions.take(5),
                placeholder = "星系或角色",
                onTextChanged = {
                    targetText = it
                    isEdited = true
                    onDistanceMapCenterUpdate(it)
                },
                modifier = Modifier
                    .width(150.dp),
            )
            AnimatedVisibility(targetText.isNotBlank()) {
                RequirementIcon(
                    isFulfilled = state.isEditedCenterValid || !isEdited,
                    fulfilledTooltip = when {
                        state.followingCharacterId != null -> "有效角色"
                        else -> "有效星系"
                    },
                    notFulfilledTooltip = "未找到该星系或角色",
                )
            }
        }
        val ranges = List(5) {
            val range = it + 1
            "$range 跳" to range
        }
        RiftDropdownWithLabel(
            label = "范围：",
            items = ranges,
            selectedItem = ranges.firstOrNull { it.second == state.distance } ?: ranges.first(),
            onItemSelected = { onDistanceMapRangeUpdate(it.second) },
            getItemName = { it.first },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsPanelTitle(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.onClick { onBack() },
    ) {
        RiftImageButton(
            resource = Res.drawable.backicon,
            size = 20.dp,
            onClick = onBack,
        )
        Text(
            text = title,
            style = RiftTheme.typography.headerPrimary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlternativeLayoutsPills(
    alternativeLayouts: List<Layout>,
    selectedLayoutId: Int,
    onLayoutSelected: (Int) -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = "备用布局：",
            style = RiftTheme.typography.headerPrimary,
        )
        alternativeLayouts.forEach { layout ->
            RiftPill(
                text = layout.name,
                isSelected = layout.layoutId == selectedLayoutId,
                onClick = {
                    onLayoutSelected(layout.layoutId)
                },
            )
        }
    }
}

@Composable
private fun DistanceMapPills(
    state: DistanceMapState,
    onDistanceMapCenterClick: () -> Unit,
    onDistanceMapRangeClick: () -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = "中心：",
                style = RiftTheme.typography.headerPrimary,
            )
            if (state.followingCharacterId != null) {
                RiftPill(
                    text = state.followingCharacterName ?: state.followingCharacterId.toString(),
                    onClick = onDistanceMapCenterClick,
                )
            } else {
                RiftPill(
                    text = state.centerSystemName ?: state.centerSystemId.toString(),
                    onClick = onDistanceMapCenterClick,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = "范围：",
                style = RiftTheme.typography.headerPrimary,
            )
            RiftPill(
                text = "${state.distance} 跳",
                onClick = onDistanceMapRangeClick,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemColorPills(
    isExpanded: Boolean,
    isCellColor: Boolean,
    selected: MapSystemInfoType?,
    onPillClick: (MapSystemInfoType?) -> Unit,
    onPillEditClick: (MapSystemInfoType?) -> Unit,
    onPillHover: (MapSystemInfoType?, Boolean) -> Unit = { _, _ -> },
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        val colorEntries = MapSystemInfoType.entries - listOf(MapSystemInfoType.Planets, MapSystemInfoType.Region, MapSystemInfoType.Constellation)
        val pills = if (isCellColor) colorEntries + null else colorEntries
        pills.filter { isExpanded || selected == it }
            .forEach { type ->
                val (text, tooltip) = getMapStarInfoTypeColorName(type)
                RiftTooltipArea(
                    text = tooltip,
                ) {
                    RiftPill(
                        text = text,
                        isSelected = isExpanded && selected == type,
                        onClick = {
                            onPillClick(type)
                        },
                        onEditClick = editableInfoTypes[type]?.let { { onPillEditClick(type) } },
                        onHoverChange = {
                            onPillHover(type, it)
                        },
                    )
                }
            }
    }
}

@Composable
private fun SystemIndicatorsPills(
    hidden: Set<MapSystemInfoType>,
    getInfoTypeNames: (color: MapSystemInfoType?) -> Pair<String, String>,
    selected: List<MapSystemInfoType>,
    onPillClick: (MapSystemInfoType) -> Unit,
    onPillEditClick: (MapSystemInfoType) -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier,
    ) {
        val pills = MapSystemInfoType.entries - hidden
        pills.forEach { type ->
            val (text, tooltip) = getInfoTypeNames(type)
            RiftTooltipArea(
                text = tooltip,
            ) {
                RiftPill(
                    text = text,
                    isSelected = type in selected,
                    onClick = {
                        onPillClick(type)
                    },
                    onEditClick = editableInfoTypes[type]?.let { { onPillEditClick(type) } },
                )
            }
        }
    }
}

/**
 * System coloring
 */
private fun getMapStarInfoTypeColorName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "恒星实际颜色" to "按恒星的实际颜色着色"
        MapSystemInfoType.Security -> "安等状态" to "按安等状态着色"
        MapSystemInfoType.NullSecurity -> "零安状态" to "按负安等状态着色"
        MapSystemInfoType.IntelHostiles -> "敌对数量" to "按上报敌对数量着色"
        MapSystemInfoType.Jumps -> "跃迁数" to "按最近一小时跃迁数着色"
        MapSystemInfoType.Kills -> "击杀数" to "按最近一小时舰船与蛋击杀数着色"
        MapSystemInfoType.NpcKills -> "NPC 击杀数" to "按最近一小时 NPC 击杀数着色"
        MapSystemInfoType.Assets -> "资产" to "按此处资产数量着色"
        MapSystemInfoType.Incursions -> "入侵" to "按入侵状态着色"
        MapSystemInfoType.Stations -> "空间站" to "按空间站数量着色"
        MapSystemInfoType.FactionWarfare -> "势力战争" to "按势力战争占领方着色"
        MapSystemInfoType.Sovereignty -> "主权" to "按主权持有者着色"
        MapSystemInfoType.SovereigntyUpgrades -> "主权升级" to "按已安装主权升级着色"
        MapSystemInfoType.MetaliminalStorms -> "亚稳风暴" to "按是否存在亚稳风暴着色"
        MapSystemInfoType.JumpRange -> "跳驱范围" to "按跳驱距离着色"
        MapSystemInfoType.Planets -> throw IllegalArgumentException("不用于着色")
        MapSystemInfoType.JoveObservatories -> "尤瓦观测站" to "存在尤瓦观测站时着色"
        MapSystemInfoType.Wormholes -> "虫洞" to "存在 Thera 或 Turnur 虫洞时着色"
        MapSystemInfoType.Colonies -> "PI 殖民地" to "存在你的 PI 殖民地时着色"
        MapSystemInfoType.Clones -> "克隆体" to "存在你的跳克隆时着色"
        MapSystemInfoType.Standings -> "声望" to "按你对主权持有者的声望着色。\n低安和高安始终为黄绿色。"
        MapSystemInfoType.RatsType -> "海盗势力" to "按星系内海盗势力着色"
        MapSystemInfoType.AsteroidBelts -> "小行星带" to "存在小行星带时着色"
        MapSystemInfoType.IceFields -> "冰矿带" to "存在冰矿带时着色"
        MapSystemInfoType.Region -> throw IllegalArgumentException("不用于着色")
        MapSystemInfoType.Constellation -> throw IllegalArgumentException("不用于着色")
        MapSystemInfoType.IndustryIndexCopying -> "复制指数" to "按复制工业成本指数着色"
        MapSystemInfoType.IndustryIndexInvention -> "发明指数" to "按发明工业成本指数着色"
        MapSystemInfoType.IndustryIndexManufacturing -> "制造指数" to "按制造工业成本指数着色"
        MapSystemInfoType.IndustryIndexReaction -> "反应指数" to "按反应工业成本指数着色"
        MapSystemInfoType.IndustryIndexMaterialEfficiency -> "材料效率指数" to "按材料效率研究工业成本指数着色"
        MapSystemInfoType.IndustryIndexTimeEfficiency -> "时间效率指数" to "按时间效率研究工业成本指数着色"
        null -> "无" to "不显示背景色"
    }
}

/**
 * System indicators
 */
private fun getMapStarInfoTypeIndicatorName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "" to ""
        MapSystemInfoType.Security -> "安等状态" to "星系安等状态"
        MapSystemInfoType.NullSecurity -> "" to ""
        MapSystemInfoType.IntelHostiles -> "" to ""
        MapSystemInfoType.Jumps -> "跃迁数" to "最近一小时跃迁数"
        MapSystemInfoType.Kills -> "击杀数" to "最近一小时舰船与蛋击杀数"
        MapSystemInfoType.NpcKills -> "NPC 击杀数" to "最近一小时 NPC 击杀数"
        MapSystemInfoType.Assets -> "资产" to "此处资产数量"
        MapSystemInfoType.Incursions -> "入侵" to "显示有入侵的星系"
        MapSystemInfoType.Stations -> "空间站" to "空间站数量"
        MapSystemInfoType.FactionWarfare -> "" to ""
        MapSystemInfoType.Sovereignty -> "主权" to "主权持有者标识"
        MapSystemInfoType.SovereigntyUpgrades -> "主权升级" to "已安装主权升级标识"
        MapSystemInfoType.MetaliminalStorms -> "亚稳风暴" to "显示有风暴的星系"
        MapSystemInfoType.JumpRange -> "跳驱范围" to "显示位于跳驱范围内的星系"
        MapSystemInfoType.Planets -> "行星" to "行星指示"
        MapSystemInfoType.JoveObservatories -> "尤瓦观测站" to "尤瓦观测站指示"
        MapSystemInfoType.Wormholes -> "虫洞" to "Thera 与 Turnur 虫洞指示"
        MapSystemInfoType.Colonies -> "PI 殖民地" to "PI 殖民地指示"
        MapSystemInfoType.Clones -> "克隆体" to "跳克隆指示"
        MapSystemInfoType.Standings -> "声望" to "对主权持有者的声望"
        MapSystemInfoType.RatsType -> "" to ""
        MapSystemInfoType.AsteroidBelts -> "小行星带" to "小行星带指示"
        MapSystemInfoType.IceFields -> "冰矿带" to "冰矿带指示"
        MapSystemInfoType.Region -> "星域" to "星域名称"
        MapSystemInfoType.Constellation -> "星座" to "星座名称"
        MapSystemInfoType.IndustryIndexCopying -> "复制指数" to "复制工业成本指数"
        MapSystemInfoType.IndustryIndexInvention -> "发明指数" to "发明工业成本指数"
        MapSystemInfoType.IndustryIndexManufacturing -> "制造指数" to "制造工业成本指数"
        MapSystemInfoType.IndustryIndexReaction -> "反应指数" to "反应工业成本指数"
        MapSystemInfoType.IndustryIndexMaterialEfficiency -> "材料效率指数" to "材料效率研究工业成本指数"
        MapSystemInfoType.IndustryIndexTimeEfficiency -> "时间效率指数" to "时间效率研究工业成本指数"
        null -> "无" to "不显示背景色"
    }
}

/**
 * System info box indicators
 */
private fun getMapStarInfoTypeInfoBoxName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "" to ""
        MapSystemInfoType.Security -> "安等状态" to "星系安等状态"
        MapSystemInfoType.NullSecurity -> "" to ""
        MapSystemInfoType.IntelHostiles -> "" to ""
        MapSystemInfoType.Jumps -> "跃迁数" to "最近一小时跃迁数"
        MapSystemInfoType.Kills -> "击杀数" to "最近一小时舰船与蛋击杀数"
        MapSystemInfoType.NpcKills -> "NPC 击杀数" to "最近一小时 NPC 击杀数"
        MapSystemInfoType.Assets -> "资产" to "此处资产数量"
        MapSystemInfoType.Incursions -> "入侵" to "入侵状态"
        MapSystemInfoType.Stations -> "空间站" to "空间站数量"
        MapSystemInfoType.FactionWarfare -> "势力战争" to "势力战争详情"
        MapSystemInfoType.Sovereignty -> "主权" to "主权持有者"
        MapSystemInfoType.SovereigntyUpgrades -> "主权升级" to "已安装主权升级"
        MapSystemInfoType.MetaliminalStorms -> "亚稳风暴" to "亚稳风暴类型"
        MapSystemInfoType.JumpRange -> "跳驱范围" to "到该星系的跳驱距离"
        MapSystemInfoType.Planets -> "行星" to "行星信息"
        MapSystemInfoType.JoveObservatories -> "尤瓦观测站" to "尤瓦观测站存在信息"
        MapSystemInfoType.Wormholes -> "虫洞" to "Thera 与 Turnur 虫洞信息"
        MapSystemInfoType.Colonies -> "PI 殖民地" to "PI 殖民地信息"
        MapSystemInfoType.Clones -> "克隆体" to "跳克隆信息"
        MapSystemInfoType.Standings -> "声望" to "对主权持有者的声望"
        MapSystemInfoType.RatsType -> "海盗势力" to "星系内海盗势力"
        MapSystemInfoType.AsteroidBelts -> "小行星带" to "小行星带存在信息"
        MapSystemInfoType.IceFields -> "冰矿带" to "冰矿带存在信息"
        MapSystemInfoType.Region -> "星域" to "星域名称"
        MapSystemInfoType.Constellation -> "星座" to "星座名称"
        MapSystemInfoType.IndustryIndexCopying -> "复制指数" to "复制工业成本指数"
        MapSystemInfoType.IndustryIndexInvention -> "发明指数" to "发明工业成本指数"
        MapSystemInfoType.IndustryIndexManufacturing -> "制造指数" to "制造工业成本指数"
        MapSystemInfoType.IndustryIndexReaction -> "反应指数" to "反应工业成本指数"
        MapSystemInfoType.IndustryIndexMaterialEfficiency -> "材料效率指数" to "材料效率研究工业成本指数"
        MapSystemInfoType.IndustryIndexTimeEfficiency -> "时间效率指数" to "时间效率研究工业成本指数"
        null -> "无" to "不显示背景色"
    }
}
