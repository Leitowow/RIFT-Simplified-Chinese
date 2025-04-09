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
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftPill
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.backicon
import dev.nohus.rift.generated.resources.expand_more_16px
import dev.nohus.rift.map.MapJumpRangeController.MapJumpRangeState
import dev.nohus.rift.map.MapLayoutRepository.Layout
import dev.nohus.rift.map.MapPlanetsController.MapPlanetsState
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.MapViewModel.MapType.ClusterRegionsMap
import dev.nohus.rift.map.MapViewModel.MapType.ClusterSystemsMap
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.map.MapViewModel.SystemInfoTypes
import dev.nohus.rift.map.PanelState.CellColor
import dev.nohus.rift.map.PanelState.Collapsed
import dev.nohus.rift.map.PanelState.Expanded
import dev.nohus.rift.map.PanelState.Indicators
import dev.nohus.rift.map.PanelState.InfoBox
import dev.nohus.rift.map.PanelState.JumpRange
import dev.nohus.rift.map.PanelState.Planets
import dev.nohus.rift.map.PanelState.StarColor
import dev.nohus.rift.repositories.PlanetTypes
import dev.nohus.rift.repositories.PlanetTypes.PlanetType
import dev.nohus.rift.settings.persistence.MapSystemInfoType
import org.jetbrains.compose.resources.painterResource
import dev.nohus.rift.settings.persistence.MapType as SettingsMapType

enum class PanelState {
    Collapsed, Expanded,
    StarColor, CellColor, Indicators, InfoBox,
    JumpRange, Planets
}

private val editableInfoTypes = mapOf(
    MapSystemInfoType.JumpRange to JumpRange,
    MapSystemInfoType.Planets to Planets,
)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun MapSettingsPanel(
    hazeState: HazeState,
    mapType: MapType,
    systemInfoTypes: SystemInfoTypes,
    mapJumpRangeState: MapJumpRangeState,
    mapPlanetsState: MapPlanetsState,
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
    onLayoutSelected: (Int) -> Unit,
) {
    val settingsMapType = when (mapType) {
        ClusterRegionsMap -> null
        ClusterSystemsMap -> SettingsMapType.NewEden
        is RegionMap -> SettingsMapType.Region
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
                                        text = "星系:",
                                        style = RiftTheme.typography.titlePrimary,
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
                                        text = "背景:",
                                        style = RiftTheme.typography.titlePrimary,
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
                                        text = "筛选:",
                                        style = RiftTheme.typography.titlePrimary,
                                    )
                                    val text = systemInfoTypes.indicators[settingsMapType].orEmpty().let {
                                        if (it.isEmpty()) "None" else "${it.size} enabled"
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
                                        text = "信息框:",
                                        style = RiftTheme.typography.titlePrimary,
                                    )
                                    val text = systemInfoTypes.infoBox[settingsMapType].orEmpty().let {
                                        if (it.isEmpty()) "None" else "${it.size} enabled"
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
                                title = "星系背景颜色",
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
                                title = "筛选(始终可见)",
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
                                title = "信息框详情(悬停时可见)",
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
            title = "跳跃范围",
            onBack = onBack,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            var targetText by remember { mutableStateOf("") }
            LaunchedEffect(mapJumpRangeState.target) {
                when (mapJumpRangeState.target) {
                    is MapJumpRangeController.MapJumpRangeTarget.Character -> targetText = mapJumpRangeState.target.name
                    is MapJumpRangeController.MapJumpRangeTarget.System -> targetText = mapJumpRangeState.target.name
                    null -> {}
                }
            }
            Text(
                text = "从:",
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier.padding(end = Spacing.small),
            )
            RiftTextField(
                text = targetText,
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
                    notFulfilledTooltip = "没有找到该星系或角色",
                )
            }
        }
        val ranges = listOf(
            "6光年 – 超级航母, 泰坦" to 6.0,
            "7光年 – 航母, 无畏舰, 后勤舰" to 7.0,
            "8光年 – 黑隐特勤舰" to 8.0,
            "10光年 – 跳跃货舰, 长须鲸级" to 10.0,
        )
        RiftDropdownWithLabel(
            label = "范围:",
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
            style = RiftTheme.typography.titlePrimary,
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
            text = "替代地图:",
            style = RiftTheme.typography.titlePrimary,
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
        val colorEntries = MapSystemInfoType.entries - listOf(MapSystemInfoType.Planets)
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

@OptIn(ExperimentalLayoutApi::class)
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
        MapSystemInfoType.StarColor -> "实际颜色" to "使用恒星的实际颜色"
        MapSystemInfoType.Security -> "安全等级" to "根据安全等级着色"
        MapSystemInfoType.NullSecurity -> "零安状态" to "根据负安全等级着色"
        MapSystemInfoType.IntelHostiles -> "敌对数量" to "根据报告的敌对数量着色"
        MapSystemInfoType.Jumps -> "跳跃" to "根据过去一小时的跳跃数量着色"
        MapSystemInfoType.Kills -> "击杀" to "根据过去一小时的舰船和舱体击杀数量着色"
        MapSystemInfoType.NpcKills -> "NPC击杀" to "根据过去一小时的NPC击杀数量着色"
        MapSystemInfoType.Assets -> "资产" to "根据拥有的资产数量着色"
        MapSystemInfoType.Incursions -> "入侵" to "根据入侵状态着色"
        MapSystemInfoType.Stations -> "空间站" to "根据空间站数量着色"
        MapSystemInfoType.FactionWarfare -> "势力战争" to "根据势力战争占领者着色"
        MapSystemInfoType.Sovereignty -> "主权" to "根据主权持有者着色"
        MapSystemInfoType.MetaliminalStorms -> "金属风暴" to "根据金属风暴存在情况着色"
        MapSystemInfoType.JumpRange -> "跳跃范围" to "根据跳跃范围着色"
        MapSystemInfoType.Planets -> throw IllegalArgumentException("Not used for colors")
        MapSystemInfoType.JoveObservatories -> "朱庇特观测站" to "当存在朱庇特观测站时着色"
        MapSystemInfoType.Colonies -> "行星工业殖民地" to "当存在行星工业殖民地时着色"
        MapSystemInfoType.Clones -> "克隆体" to "当存在跳跃克隆体时着色"
        MapSystemInfoType.Standings -> "声望" to "根据对主权持有者的声望着色\n高安和低安区域始终为黄色和绿色"
        MapSystemInfoType.RatsType -> "海盗" to "根据星系中的海盗势力着色"
        MapSystemInfoType.IndustryIndexCopying -> "复制指数" to "根据复制工业成本指数着色"
        MapSystemInfoType.IndustryIndexInvention -> "发明指数" to "根据发明工业成本指数着色"
        MapSystemInfoType.IndustryIndexManufacturing -> "制造指数" to "根据制造工业成本指数着色"
        MapSystemInfoType.IndustryIndexReaction -> "反应指数" to "根据反应工业成本指数着色"
        MapSystemInfoType.IndustryIndexMaterialEfficiency -> "材料效率指数" to "根据材料效率研究工业成本指数着色"
        MapSystemInfoType.IndustryIndexTimeEfficiency -> "时间效率指数" to "根据时间效率研究工业成本指数着色"
        null -> "无" to "无背景颜色"
    }
}

/**
 * System indicators
 */
private fun getMapStarInfoTypeIndicatorName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "" to ""
        MapSystemInfoType.Security -> "安全等级" to "星系的安全等级"
        MapSystemInfoType.NullSecurity -> "" to ""
        MapSystemInfoType.IntelHostiles -> "" to ""
        MapSystemInfoType.Jumps -> "跳跃" to "过去一小时的跳跃数量"
        MapSystemInfoType.Kills -> "击杀" to "过去一小时的舰船和舱体击杀数量"
        MapSystemInfoType.NpcKills -> "NPC击杀" to "过去一小时的NPC击杀数量"
        MapSystemInfoType.Assets -> "资产" to "位于此处的资产数量"
        MapSystemInfoType.Incursions -> "入侵" to "存在入侵的星系的指示器"
        MapSystemInfoType.Stations -> "空间站" to "空间站数量"
        MapSystemInfoType.FactionWarfare -> "" to ""
        MapSystemInfoType.Sovereignty -> "主权" to "主权持有者标志"
        MapSystemInfoType.MetaliminalStorms -> "金属风暴" to "存在风暴的星系的指示器"
        MapSystemInfoType.JumpRange -> "跳跃范围" to "跳跃范围内的星系的指示器"
        MapSystemInfoType.Planets -> "行星" to "行星指示器"
        MapSystemInfoType.JoveObservatories -> "朱庇特观测站" to "朱庇特观测站指示器"
        MapSystemInfoType.Colonies -> "行星工业殖民地" to "行星工业殖民地指示器"
        MapSystemInfoType.Clones -> "克隆体" to "跳跃克隆体指示器"
        MapSystemInfoType.Standings -> "声望" to "对主权持有者的声望"
        MapSystemInfoType.RatsType -> "" to ""
        MapSystemInfoType.IndustryIndexCopying -> "复制指数" to "复制工业成本指数"
        MapSystemInfoType.IndustryIndexInvention -> "发明指数" to "发明工业成本指数"
        MapSystemInfoType.IndustryIndexManufacturing -> "制造指数" to "制造工业成本指数"
        MapSystemInfoType.IndustryIndexReaction -> "反应指数" to "反应工业成本指数"
        MapSystemInfoType.IndustryIndexMaterialEfficiency -> "材料效率指数" to "材料效率研究工业成本指数"
        MapSystemInfoType.IndustryIndexTimeEfficiency -> "时间效率指数" to "时间效率研究工业成本指数"
        null -> "无" to "无背景颜色"
    }
}

/**
 * System info box indicators
 */
private fun getMapStarInfoTypeInfoBoxName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "" to ""
        MapSystemInfoType.Security -> "安全等级" to "星系的安全等级"
        MapSystemInfoType.NullSecurity -> "" to ""
        MapSystemInfoType.IntelHostiles -> "" to ""
        MapSystemInfoType.Jumps -> "跳跃" to "过去一小时的跳跃数量"
        MapSystemInfoType.Kills -> "击杀" to "过去一小时的舰船和舱体击杀数量"
        MapSystemInfoType.NpcKills -> "NPC击杀" to "过去一小时的NPC击杀数量"
        MapSystemInfoType.Assets -> "资产" to "位于此处的资产数量"
        MapSystemInfoType.Incursions -> "入侵" to "入侵状态"
        MapSystemInfoType.Stations -> "空间站" to "空间站数量"
        MapSystemInfoType.FactionWarfare -> "势力战争" to "势力战争详情"
        MapSystemInfoType.Sovereignty -> "主权" to "主权持有者"
        MapSystemInfoType.MetaliminalStorms -> "金属风暴" to "金属风暴类型"
        MapSystemInfoType.JumpRange -> "跳跃范围" to "到星系的跳跃距离"
        MapSystemInfoType.Planets -> "行星" to "行星信息"
        MapSystemInfoType.JoveObservatories -> "朱庇特观测站" to "朱庇特观测站存在信息"
        MapSystemInfoType.Colonies -> "行星工业殖民地" to "行星工业殖民地信息"
        MapSystemInfoType.Clones -> "克隆体" to "跳跃克隆体信息"
        MapSystemInfoType.Standings -> "声望" to "对主权持有者的声望"
        MapSystemInfoType.RatsType -> "海盗" to "星系中的海盗势力"
        MapSystemInfoType.IndustryIndexCopying -> "复制指数" to "复制工业成本指数"
        MapSystemInfoType.IndustryIndexInvention -> "发明指数" to "发明工业成本指数"
        MapSystemInfoType.IndustryIndexManufacturing -> "制造指数" to "制造工业成本指数"
        MapSystemInfoType.IndustryIndexReaction -> "反应指数" to "反应工业成本指数"
        MapSystemInfoType.IndustryIndexMaterialEfficiency -> "材料效率指数" to "材料效率研究工业成本指数"
        MapSystemInfoType.IndustryIndexTimeEfficiency -> "时间效率指数" to "时间效率研究工业成本指数"
        null -> "无" to "无背景颜色"
    }
}
