package dev.nohus.rift.alerts.list

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.alerts.Alert
import dev.nohus.rift.alerts.AlertAction
import dev.nohus.rift.alerts.AlertTrigger
import dev.nohus.rift.alerts.ChatMessageChannel
import dev.nohus.rift.alerts.GameActionType
import dev.nohus.rift.alerts.IntelChannel
import dev.nohus.rift.alerts.IntelReportLocation
import dev.nohus.rift.alerts.IntelReportType
import dev.nohus.rift.alerts.JabberMessageChannel
import dev.nohus.rift.alerts.JabberPingType
import dev.nohus.rift.alerts.JumpRange
import dev.nohus.rift.alerts.PapType
import dev.nohus.rift.alerts.PiEventType
import dev.nohus.rift.alerts.create.CreateAlertDialog
import dev.nohus.rift.alerts.creategroup.CreateGroupDialog
import dev.nohus.rift.alerts.list.AlertsViewModel.UiState
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ExpandChevron
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckbox
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.contacts.ContactsRepository.Label
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.delete
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.toggle_off_18
import dev.nohus.rift.generated.resources.toggle_on_18
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.utils.sound.Sound
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.nameWithoutExtension

@Composable
fun AlertsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: AlertsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "告警",
        icon = Res.drawable.window_loudspeaker_icon,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        AlertsWindowContent(
            state = state,
            onAlertClick = viewModel::onAlertClick,
            onGroupClick = viewModel::onGroupClick,
            onToggleAlert = viewModel::onToggleAlert,
            onGroupChange = viewModel::onGroupChange,
            onTestAlertSound = viewModel::onTestAlertSound,
            onEditAlertAction = viewModel::onEditAlertAction,
            onDeleteAlert = viewModel::onDeleteAlert,
            onCreateAlertClick = viewModel::onCreateAlertClick,
            onCreateGroupClick = viewModel::onCreateGroupClick,
            onGroupRenameClick = viewModel::onGroupRenameClick,
            onGroupDeleteClick = viewModel::onGroupDeleteClick,
            onGroupToggleAlerts = viewModel::onGroupToggleAlerts,
        )

        val isCreateAlertDialogOpen = state.isCreateAlertDialogOpen
        if (isCreateAlertDialogOpen != null) {
            CreateAlertDialog(
                inputModel = isCreateAlertDialogOpen,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseCreateAlert,
            )
        }
        val isCreateGroupDialogOpen = state.isCreateGroupDialogOpen
        if (isCreateGroupDialogOpen != null) {
            CreateGroupDialog(
                inputModel = isCreateGroupDialogOpen,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseCreateGroup,
                onConfirmClick = viewModel::onCreateGroupConfirm,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlertsWindowContent(
    state: UiState,
    onAlertClick: (id: String) -> Unit,
    onGroupClick: (name: String?) -> Unit,
    onToggleAlert: (id: String, isEnabled: Boolean) -> Unit,
    onGroupChange: (id: String, group: String?) -> Unit,
    onTestAlertSound: (id: String) -> Unit,
    onEditAlertAction: (id: String) -> Unit,
    onDeleteAlert: (id: String) -> Unit,
    onCreateAlertClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onGroupRenameClick: (group: String) -> Unit,
    onGroupDeleteClick: (group: String) -> Unit,
    onGroupToggleAlerts: (group: String?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        if (state.alerts.isNotEmpty()) {
            ScrollbarLazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                val nonEmptyGroups = state.alerts.mapNotNull { it.group }.toSet()
                val emptyGroups = state.groups - nonEmptyGroups
                state.alerts
                    .groupBy { it.group }
                    .let { it + emptyGroups.associateWith { emptyList() } }
                    .entries
                    .sortedWith(compareBy({ it.key == null }, { it.key }))
                    .forEach { (group, alertsInGroup) ->
                        val isExpanded = group !in state.collapsedGroups
                        stickyHeader {
                            val text = buildAnnotatedString {
                                withColor(RiftTheme.colors.textPrimary) {
                                    append(group ?: "默认")
                                }
                                val total = alertsInGroup.size
                                val enabled = alertsInGroup.count { it.isEnabled }
                                append(" - ")
                                append(total.toString())
                                append(" 个告警")
                                if (enabled < total) {
                                    append(" - ")
                                    append(enabled.toString())
                                    append(" 个已启用")
                                }
                            }
                            AlertGroupHeader(
                                name = text,
                                isEmpty = alertsInGroup.isEmpty(),
                                isExpanded = isExpanded,
                                isDefault = group == null,
                                hasEnabledAlerts = alertsInGroup.any { it.isEnabled },
                                onClick = { onGroupClick(group) },
                                onGroupToggleAlerts = { onGroupToggleAlerts(group) },
                                onGroupRenameClick = { onGroupRenameClick(group!!) },
                                onGroupDeleteClick = { onGroupDeleteClick(group!!) },
                            )
                        }
                        if (isExpanded) {
                            if (group in emptyGroups) {
                                item {
                                    EmptyGroup()
                                }
                            }
                            items(alertsInGroup, key = { it.id }) { alert ->
                                val isExpanded = alert.id == state.expandedAlert
                                AlertItem(
                                    onAlertClick = onAlertClick,
                                    alert = alert,
                                    onToggleAlert = onToggleAlert,
                                    state = state,
                                    isExpanded = isExpanded,
                                    groups = state.groups,
                                    onGroupChange = { onGroupChange(alert.id, it) },
                                    onTestAlertSound = onTestAlertSound,
                                    onEditAlertAction = onEditAlertAction,
                                    onDeleteAlert = onDeleteAlert,
                                )
                            }
                        }
                    }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "尚未创建告警。\n点击下方按钮添加。",
                    style = RiftTheme.typography.headerPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.align(Alignment.End),
        ) {
            if (state.alerts.isNotEmpty()) {
                RiftButton(
                    text = "新建分组",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = onCreateGroupClick,
                )
            }
            RiftButton(
                text = "新建告警",
                onClick = onCreateAlertClick,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.AlertGroupHeader(
    name: AnnotatedString,
    isEmpty: Boolean,
    isExpanded: Boolean,
    isDefault: Boolean,
    hasEnabledAlerts: Boolean,
    onClick: () -> Unit,
    onGroupToggleAlerts: () -> Unit,
    onGroupRenameClick: () -> Unit,
    onGroupDeleteClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pointerInteraction(pointerState)
            .background(RiftTheme.colors.backgroundPrimary)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize()
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .onClick { onClick() },
    ) {
        ExpandChevron(isExpanded = isExpanded)
        Text(
            text = name,
            style = RiftTheme.typography.headerSecondary,
            modifier = Modifier.padding(vertical = Spacing.small),
        )
        Spacer(Modifier.weight(1f))

        val buttonsAlpha by animateFloatAsState(if (pointerState.isHovered) 1f else 0f)
        if (!isEmpty) {
            RiftTooltipArea(
                text = if (hasEnabledAlerts) "全部关闭" else "全部启用",
            ) {
                RiftImageButton(
                    resource = if (hasEnabledAlerts) Res.drawable.toggle_on_18 else Res.drawable.toggle_off_18,
                    size = 18.dp,
                    onClick = onGroupToggleAlerts,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
        }
        if (!isDefault) {
            RiftTooltipArea(
                text = "重命名分组",
            ) {
                RiftImageButton(
                    resource = Res.drawable.editplanicon,
                    size = 20.dp,
                    onClick = onGroupRenameClick,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
            RiftTooltipArea(
                text = if (isEmpty) "删除分组" else "删除分组并将告警移至默认分组",
            ) {
                RiftImageButton(
                    resource = Res.drawable.delete,
                    size = 20.dp,
                    onClick = onGroupDeleteClick,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
        }
        Spacer(Modifier.width(Spacing.small))
    }
}

@Composable
private fun LazyItemScope.EmptyGroup() {
    Row(
        modifier = Modifier
            .padding(vertical = Spacing.medium)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize(),
    ) {
        Text(
            text = "该分组暂无告警",
            style = RiftTheme.typography.bodySecondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.AlertItem(
    onAlertClick: (id: String) -> Unit,
    alert: Alert,
    onToggleAlert: (id: String, isEnabled: Boolean) -> Unit,
    state: UiState,
    isExpanded: Boolean,
    groups: Set<String>,
    onGroupChange: (group: String?) -> Unit,
    onTestAlertSound: (id: String) -> Unit,
    onEditAlertAction: (id: String) -> Unit,
    onDeleteAlert: (id: String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .hoverBackground()
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .padding(vertical = Spacing.medium)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize()
            .onClick { onAlertClick(alert.id) },
    ) {
        val alpha = if (alert.isEnabled) 1f else 0.5f
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.small),
        ) {
            RiftCheckbox(
                isChecked = alert.isEnabled,
                onCheckedChange = { onToggleAlert(alert.id, it) },
            )
            val text = getAlertText(alert, state.characters, state.sounds, state.labels)

            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .alpha(alpha)
                    .padding(horizontal = Spacing.medium),
            )
        }
        if (isExpanded) {
            listOfNotNull(
                getSpecificCharactersDetailText(alert),
                getSpecificShipClassesDetailText(alert),
                getSpecificFleetCommandersDetailText(alert),
                getDecloakIgnoredKeywordsDetailText(alert),
                getSpecificColoniesDetailText(alert, state.colonies),
                getLabeledContactsDetailText(alert, state.labels),
            ).forEach {
                Row(
                    modifier = Modifier
                        .padding(horizontal = Spacing.small)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = it,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(alpha),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .padding(horizontal = Spacing.medium)
                    .fillMaxWidth(),
            ) {
                RiftDropdownWithLabel(
                    label = "分组：",
                    items = (groups.sorted() + listOf(null)).toList(),
                    selectedItem = alert.group,
                    onItemSelected = onGroupChange,
                    getItemName = { it ?: "默认" },
                    maxItems = 3,
                    modifier = Modifier
                        .widthIn(max = 170.dp)
                        .padding(end = Spacing.medium),
                )
                if (alert.actions.any { it is AlertAction.Sound || it is AlertAction.CustomSound }) {
                    RiftButton(
                        text = "试听音效",
                        type = ButtonType.Secondary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = { onTestAlertSound(alert.id) },
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                }
                RiftButton(
                    text = "编辑动作",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = { onEditAlertAction(alert.id) },
                    modifier = Modifier.padding(end = Spacing.medium),
                )
                RiftButton(
                    text = "删除",
                    type = ButtonType.Negative,
                    onClick = { onDeleteAlert(alert.id) },
                )
            }
        }
    }
}

@Composable
private fun getSpecificCharactersDetailText(alert: Alert): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.IntelReported) {
        val specificCharacters = alert.trigger.reportTypes
            .firstOrNull { it is IntelReportType.SpecificCharacters }
        if (specificCharacters != null) {
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            val characters = (specificCharacters as IntelReportType.SpecificCharacters).characters
            buildAnnotatedString {
                withStyle(secondary) {
                    append("监视角色：")
                    characters.forEach { character ->
                        withStyle(primary) {
                            append(character)
                        }
                        if (character != characters.last()) {
                            append(", ")
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getSpecificFleetCommandersDetailText(alert: Alert): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.JabberPing && alert.trigger.pingType is JabberPingType.Fleet) {
        val fleetCommanders = alert.trigger.pingType.fleetCommanders
        if (fleetCommanders.isNotEmpty()) {
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            buildAnnotatedString {
                withStyle(secondary) {
                    append("集结指挥：")
                    fleetCommanders.forEach { character ->
                        withStyle(primary) {
                            append(character)
                        }
                        if (character != fleetCommanders.last()) {
                            append(", ")
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getDecloakIgnoredKeywordsDetailText(alert: Alert): AnnotatedString? {
    val decloakedTrigger = (alert.trigger as? AlertTrigger.GameAction)?.actionTypes
        ?.filterIsInstance<GameActionType.Decloaked>()?.firstOrNull() ?: return null
    return if (decloakedTrigger.ignoredKeywords.isNotEmpty()) {
        val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
        val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
        buildAnnotatedString {
            withStyle(secondary) {
                append("忽略含以下关键字的显隐对象：")
                decloakedTrigger.ignoredKeywords.forEach { keyword ->
                    withStyle(primary) {
                        append(keyword)
                    }
                    if (keyword != decloakedTrigger.ignoredKeywords.last()) {
                        append(", ")
                    }
                }
            }
        }
    } else {
        null
    }
}

@Composable
private fun getSpecificColoniesDetailText(alert: Alert, colonies: List<ColonyItem>): AnnotatedString? {
    val colonyIds = (alert.trigger as? AlertTrigger.PlanetaryIndustry)?.coloniesFilter ?: return null
    val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
    val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
    return buildAnnotatedString {
        withStyle(secondary) {
            append("目标殖民地：")
            colonyIds
                .mapNotNull { id ->
                    val colony = colonies.find { it.colony.id == id } ?: return@mapNotNull null
                    colony.colony.characterId to colony
                }.groupBy {
                    it.first
                }.forEach { (_, entries) ->
                    val items = entries.map { it.second }
                    val characterName = items.first().characterName ?: "加载中…"
                    append("\n")
                    withStyle(primary) {
                        append(characterName)
                    }
                    append(": ")
                    items.forEachIndexed { index, colony ->
                        if (index != 0) append(", ")
                        withStyle(primary) {
                            append(colony.colony.planet.name)
                        }
                    }
                }
        }
    }
}

@Composable
private fun getSpecificShipClassesDetailText(alert: Alert): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.IntelReported) {
        val specificClasses = alert.trigger.reportTypes
            .firstOrNull { it is IntelReportType.SpecificShipClasses }
        if (specificClasses != null) {
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            val characters = (specificClasses as IntelReportType.SpecificShipClasses).classes
            buildAnnotatedString {
                withStyle(secondary) {
                    append("监视舰船类别：")
                    characters.forEach { character ->
                        withStyle(primary) {
                            append(character)
                        }
                        if (character != characters.last()) {
                            append(", ")
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getLabeledContactsDetailText(alert: Alert, labels: List<Label>): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.IntelReported) {
        val labeledContacts = alert.trigger.reportTypes
            .firstOrNull { it is IntelReportType.LabeledContacts }
        if (labeledContacts != null) {
            val labels = (labeledContacts as IntelReportType.LabeledContacts).labels.mapNotNull { label ->
                labels.firstOrNull { it.owner.id == label.ownerId && it.id == label.id }
            }
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            buildAnnotatedString {
                withStyle(secondary) {
                    append("联系人标签：")
                    labels.forEachIndexed { index, label ->
                        withStyle(primary) {
                            append(label.name)
                        }
                        append("（")
                        withStyle(primary) {
                            append(label.owner.name)
                        }
                        append("）")
                        if (index != labels.lastIndex) {
                            appendLine()
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

private fun intelSystemLocationText(jumpsRange: JumpRange, systemName: String): String {
    val min = jumpsRange.min
    val max = jumpsRange.max
    return when {
        min == 0 && max == 0 -> "在 $systemName"
        min == 0 -> "距 $systemName 最远 $max 跳"
        min == max -> "距 $systemName 正好 $min 跳"
        else -> "$systemName 周边 $min–$max 跳"
    }
}

private fun intelAnyOwnedLocationText(jumpsRange: JumpRange, onlyUndocked: Boolean): String {
    val base = if (onlyUndocked) "任意离站角色位置" else "任意在线角色位置"
    val min = jumpsRange.min
    val max = jumpsRange.max
    return when {
        min == 0 && max == 0 -> base
        min == 0 -> "距$base 最远 $max 跳"
        min == max -> "距$base 正好 $min 跳"
        else -> "$base 周边 $min–$max 跳"
    }
}

private fun intelOwnedCharacterLocationText(
    jumpsRange: JumpRange,
    character: String,
    onlyUndocked: Boolean,
): String {
    val place = if (onlyUndocked) "${character}的离站位置" else "${character}的在线位置"
    val min = jumpsRange.min
    val max = jumpsRange.max
    return when {
        min == 0 && max == 0 -> place
        min == 0 -> "距$place 最远 $max 跳"
        min == max -> "距$place 正好 $min 跳"
        else -> "$place 周边 $min–$max 跳"
    }
}

@Composable
private fun getAlertText(
    alert: Alert,
    characters: List<LocalCharacter>,
    sounds: List<Sound>,
    labels: List<Label>,
): AnnotatedString {
    val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
    val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
    return buildAnnotatedString {
        withStyle(secondary) {
            append("当 ")
            when (val trigger = alert.trigger) {
                is AlertTrigger.IntelReported -> {
                    if (trigger.reportTypes.size != 1) {
                        append("以下任一：")
                    }
                    val types = trigger.reportTypes.joinToString { type ->
                        when (type) {
                            IntelReportType.AnyCharacter -> "角色"
                            is IntelReportType.SpecificCharacters -> {
                                if (type.characters.size == 1) {
                                    type.characters.single()
                                } else {
                                    "${type.characters.size} 个指定角色"
                                }
                            }
                            IntelReportType.AnyShip -> "舰船"
                            is IntelReportType.SpecificShipClasses -> {
                                if (type.classes.size == 1) {
                                    "${type.classes.single()} 级舰船"
                                } else {
                                    "${type.classes.size} 种船型"
                                }
                            }
                            is IntelReportType.LabeledContacts -> {
                                if (type.labels.size == 1) {
                                    val label = type.labels.single()
                                    val name = labels.firstOrNull { it.owner.id == label.ownerId && it.id == label.id }?.name ?: "?"
                                    "带标签「$name」的联系人"
                                } else {
                                    "位于 ${type.labels.size} 个标签下的角色"
                                }
                            }
                            IntelReportType.Bubbles -> "泡泡"
                            IntelReportType.GateCamp -> "星门蹲守"
                            IntelReportType.Wormhole -> "虫洞"
                            IntelReportType.Ess -> "ESS"
                            IntelReportType.Skyhook -> "天钩"
                        }
                    }
                    withStyle(primary) {
                        append(types)
                    }
                    append(" 在预警中出现，位置靠近 ")
                    val location = when (val location = trigger.reportLocation) {
                        is IntelReportLocation.System ->
                            intelSystemLocationText(location.jumpsRange, location.systemName)
                        is IntelReportLocation.AnyOwnedCharacter if location.onlyUndocked ->
                            intelAnyOwnedLocationText(location.jumpsRange, onlyUndocked = true)
                        is IntelReportLocation.AnyOwnedCharacter ->
                            intelAnyOwnedLocationText(location.jumpsRange, onlyUndocked = false)
                        is IntelReportLocation.OwnedCharacter if location.onlyUndocked -> {
                            val character = characters.firstOrNull { it.characterId == location.characterId }?.info?.name
                                ?: location.characterId.toString()
                            intelOwnedCharacterLocationText(location.jumpsRange, character, onlyUndocked = true)
                        }
                        is IntelReportLocation.OwnedCharacter -> {
                            val character = characters.firstOrNull { it.characterId == location.characterId }?.info?.name
                                ?: location.characterId.toString()
                            intelOwnedCharacterLocationText(location.jumpsRange, character, onlyUndocked = false)
                        }
                    }
                    withStyle(primary) {
                        append(location)
                    }
                }
                is AlertTrigger.GameAction -> {
                    trigger.actionTypes.forEachIndexed { index, type ->
                        if (index != 0) append("，或 ")
                        when (type) {
                            is GameActionType.InCombat -> {
                                append("你已")
                                withStyle(primary) { append("进入战斗") }
                                if (type.nameContaining != null) {
                                    append("，对象含 ")
                                    withStyle(primary) { append(type.nameContaining) }
                                }
                            }
                            is GameActionType.UnderAttack -> {
                                append("你正")
                                withStyle(primary) { append("遭受攻击") }
                                if (type.nameContaining != null) {
                                    append("，来源含 ")
                                    withStyle(primary) { append(type.nameContaining) }
                                }
                            }
                            is GameActionType.Attacking -> {
                                append("你正在")
                                withStyle(primary) { append("攻击") }
                                if (type.nameContaining != null) {
                                    append("，目标含 ")
                                    withStyle(primary) { append(type.nameContaining) }
                                }
                            }
                            GameActionType.BeingWarpScrambled -> {
                                append("你正被")
                                withStyle(primary) { append("反跳") }
                            }
                            is GameActionType.Decloaked -> {
                                append("你被")
                                withStyle(primary) { append("破隐") }
                                if (type.ignoredKeywords.isNotEmpty()) {
                                    append("（含例外关键词）")
                                }
                            }
                            is GameActionType.CombatStopped -> {
                                append("你已")
                                withStyle(primary) { append("脱离战斗") }
                                if (type.nameContaining != null) {
                                    append("，相关对象含 ")
                                    withStyle(primary) { append(type.nameContaining) }
                                }
                                append("，时长达 ")
                                val minutes = type.durationSeconds / 60
                                withStyle(primary) {
                                    when {
                                        minutes == 1 -> append("1 分钟")
                                        minutes > 1 -> append("$minutes 分钟")
                                        else -> append("${type.durationSeconds} 秒")
                                    }
                                }
                            }
                            GameActionType.RanOutOfCharges -> {
                                append("某装备")
                                withStyle(primary) { append("弹药/电量耗尽") }
                            }

                            is GameActionType.Custom -> {
                                append("游戏日志含 ")
                                if (type.isRegex) append("正则 ")
                                withStyle(primary) { append(type.messageContaining) }
                            }
                        }
                    }
                }
                is AlertTrigger.PlanetaryIndustry -> {
                    append("作用于 ")
                    val coloniesFilter = when {
                        trigger.coloniesFilter == null -> "任意殖民地"
                        trigger.coloniesFilter.size == 1 -> "指定殖民地"
                        else -> "${trigger.coloniesFilter.size} 个指定殖民地"
                    }
                    withStyle(primary) {
                        append(coloniesFilter)
                    }
                    append("，当 ")
                    trigger.eventTypes.forEachIndexed { index, type ->
                        if (index != 0) append("，或 ")
                        val text = when (type) {
                            PiEventType.ExtractorInactive -> "开采设施停工"
                            PiEventType.Idle -> "工厂停产"
                            PiEventType.NotSetup -> "未设置完毕"
                            PiEventType.StorageFull -> "仓库已满"
                        }
                        withStyle(primary) {
                            append(text)
                        }
                    }
                    if (trigger.alertBeforeSeconds > 0) {
                        val duration = Duration.ofSeconds(trigger.alertBeforeSeconds.toLong())
                        val text = when {
                            duration.toHours() >= 1 -> {
                                val h = duration.toHours()
                                "$h 小时"
                            }
                            else -> "${duration.toMinutes()} 分钟"
                        }
                        append("，提前 ")
                        withStyle(primary) {
                            append(text)
                        }
                    }
                }
                is AlertTrigger.ChatMessage -> {
                    append("聊天消息")
                    if (trigger.messageContaining != null) {
                        append("包含 ")
                        if (trigger.isRegex) {
                            append("正则 ")
                        }
                        withStyle(primary) {
                            append(trigger.messageContaining)
                        }
                    }
                    append(" 发出时")
                    if (trigger.sender != null) {
                        append("，发送者 ")
                        withStyle(primary) {
                            append(trigger.sender)
                        }
                    }
                    if (trigger.isExcludingSelf) {
                        append("（不含自己）")
                    }
                    append("，频道 ")
                    val channel = when (val channel = trigger.channel) {
                        ChatMessageChannel.Any -> "任意"
                        is ChatMessageChannel.Channel -> channel.name
                    }
                    withStyle(primary) {
                        append(channel)
                    }
                }
                is AlertTrigger.JabberPing -> {
                    @Suppress("DEPRECATION")
                    when (trigger.pingType) {
                        JabberPingType.Message -> {}
                        is JabberPingType.Message2 -> {
                            append("收到文字集结 ")
                            if (trigger.pingType.target != null) {
                                append("目标 ")
                                withStyle(primary) {
                                    append(trigger.pingType.target)
                                }
                                append(" ")
                            }
                        }
                        is JabberPingType.Fleet -> {
                            append("收到舰队集结 ")
                            if (trigger.pingType.target != null) {
                                append("目标 ")
                                withStyle(primary) {
                                    append(trigger.pingType.target)
                                }
                                append(" ")
                            }
                            if (trigger.pingType.fleetCommanders.isNotEmpty()) {
                                append("，FC：")
                                if (trigger.pingType.fleetCommanders.size == 1) {
                                    withStyle(primary) {
                                        append(trigger.pingType.fleetCommanders.single())
                                    }
                                } else {
                                    withStyle(primary) {
                                        append("${trigger.pingType.fleetCommanders.size} 名指定 FC")
                                    }
                                }
                            }
                            if (trigger.pingType.formupSystem != null) {
                                append("，集结星系 ")
                                withStyle(primary) {
                                    append(trigger.pingType.formupSystem)
                                }
                            }
                            if (trigger.pingType.papType != PapType.Any) {
                                append("，PAP：")
                                withStyle(primary) {
                                    val type = when (trigger.pingType.papType) {
                                        PapType.Any -> "任意"
                                        PapType.Peacetime -> "平时"
                                        PapType.Strategic -> "战略"
                                    }
                                    append(type)
                                }
                            }
                            if (trigger.pingType.doctrineContaining != null) {
                                append("，建制含 ")
                                withStyle(primary) {
                                    append(trigger.pingType.doctrineContaining)
                                }
                            }
                        }
                    }
                }
                is AlertTrigger.JabberMessage -> {
                    append("Jabber 消息")
                    if (trigger.messageContaining != null) {
                        append("包含 ")
                        if (trigger.isRegex) {
                            append("正则 ")
                        }
                        withStyle(primary) {
                            append(trigger.messageContaining)
                        }
                    }
                    append(" 发出时")
                    if (trigger.sender != null) {
                        append("，发送者 ")
                        withStyle(primary) {
                            append(trigger.sender)
                        }
                    }
                    append("，频道 ")
                    val channel = when (val channel = trigger.channel) {
                        JabberMessageChannel.Any -> "任意"
                        is JabberMessageChannel.Channel -> channel.name
                        JabberMessageChannel.DirectMessage -> "私聊"
                    }
                    withStyle(primary) {
                        append(channel)
                    }
                }
                is AlertTrigger.NoChannelActivity -> {
                    append("在 ")
                    val channel = when (val channel = trigger.channel) {
                        IntelChannel.All -> "全部预警频道"
                        IntelChannel.Any -> "任意预警频道"
                        is IntelChannel.Channel -> channel.name
                    }
                    withStyle(primary) {
                        append(channel)
                    }
                    append(" 中 ")
                    val minutes = trigger.durationSeconds / 60
                    withStyle(primary) {
                        when {
                            minutes == 1 -> append("1 分钟")
                            minutes > 1 -> append("$minutes 分钟")
                            else -> append("${trigger.durationSeconds} 秒")
                        }
                    }
                    append("无新消息")
                }
            }
            append(" 时，")
            val actions = alert.actions.joinToString { action ->
                when (action) {
                    AlertAction.RiftNotification -> "显示 RIFT 通知"
                    AlertAction.SystemNotification -> "发送系统通知"
                    AlertAction.PushNotification -> "发送推送通知"
                    is AlertAction.Sound -> "播放音效「${sounds.firstOrNull { it.id == action.id }?.name ?: "?"}」"
                    is AlertAction.CustomSound -> "播放音效 ${Path.of(action.path).nameWithoutExtension}"
                    AlertAction.ShowPing -> "显示集结内容"
                    AlertAction.ShowColonies -> "显示殖民地"
                }
            }
            withStyle(primary) {
                append(actions)
            }
            if (alert.cooldownSeconds != 0) {
                append("；冷却 ")
                val text = when (val minutes = alert.cooldownSeconds / 60) {
                    0 -> "${alert.cooldownSeconds} 秒"
                    1 -> "1 分钟"
                    else -> "$minutes 分钟"
                }
                withStyle(primary) {
                    append(text)
                }
            }
        }
    }
}
