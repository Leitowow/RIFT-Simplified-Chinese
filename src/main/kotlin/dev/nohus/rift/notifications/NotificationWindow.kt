package dev.nohus.rift.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ClickableCharacter
import dev.nohus.rift.compose.ClickableSystem
import dev.nohus.rift.compose.FlagIcon
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.SystemEntities
import dev.nohus.rift.compose.SystemIllustrationIconSmall
import dev.nohus.rift.compose.UiScaleController
import dev.nohus.rift.compose.modifyIfNotNull
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitStandings
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.generated.resources.window_titlebar_close
import dev.nohus.rift.notifications.NotificationsController.Notification
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.utils.Pos
import dev.nohus.rift.utils.withColor
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NotificationWindow(
    notifications: List<Notification>,
    position: Pos?,
    onHoldDisappearance: (hold: Boolean) -> Unit,
    onCloseClick: (Notification) -> Unit,
) {
    val state = rememberWindowState(
        width = Dp.Unspecified,
        height = Dp.Unspecified,
        position = position?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition.PlatformDefault,
    )
    Window(
        onCloseRequest = {},
        state = state,
        undecorated = true,
        alwaysOnTop = true,
        resizable = false,
        focusable = false,
        title = "Notification",
        icon = painterResource(Res.drawable.window_loudspeaker_icon),
    ) {
        val uiScaleController: UiScaleController = remember { koin.get() }
        uiScaleController.withScale {
            var boxSize by remember { mutableStateOf<IntSize?>(null) }
            val density = LocalDensity.current
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(Color.Black)
                    .border(1.dp, RiftTheme.colors.borderGreyLight)
                    .pointerHoverIcon(PointerIcon(Cursors.pointer))
                    .onSizeChanged {
                        // Force first measured size to be kept. On macOS for some reason there is a second measure
                        // that is 1 pixel smaller, making text wrap, this will make it ignore it
                        if (boxSize == null) boxSize = it
                    }
                    .modifyIfNotNull(boxSize) {
                        with(density) {
                            requiredSize(it.width.toDp(), it.height.toDp())
                        }
                    }
                    .onPointerEvent(PointerEventType.Enter) {
                        onHoldDisappearance(true)
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        onHoldDisappearance(false)
                    },
            ) {
                ScrollbarColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    contentPadding = PaddingValues(vertical = Spacing.medium),
                    scrollbarModifier = Modifier.padding(vertical = Spacing.small),
                    modifier = Modifier
                        .heightIn(max = 500.dp)
                        .widthIn(max = 500.dp)
                        .width(IntrinsicSize.Max),
                ) {
                    notifications.reversed().forEachIndexed { index, notification ->
                        if (index != 0) Divider(color = RiftTheme.colors.borderGreyLight, thickness = 1.dp)
                        NotificationContent(
                            notification = notification,
                            onCloseClick = { onCloseClick(notification) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationContent(
    notification: Notification,
    onCloseClick: (() -> Unit)?,
) {
    when (notification) {
        is Notification.TextNotification -> {
            NotificationTitle(
                title = buildAnnotatedString {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
                        append(notification.title)
                    }
                },
                onCloseClick = onCloseClick,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            ) {
                if (notification.characterId != null) {
                    Character(notification.characterId, "Your character")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (notification.type != null) {
                        AsyncTypeIcon(
                            type = notification.type,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            val annotations = notification.message
                                .getStringAnnotations(
                                    Notification.TextNotification.STYLE_TAG,
                                    0,
                                    notification.message.length,
                                )
                            val styledMessage = buildAnnotatedString {
                                append(notification.message)
                                annotations.forEach { annotation ->
                                    addStyle(
                                        SpanStyle(color = RiftTheme.colors.textHighlighted),
                                        annotation.start,
                                        annotation.end,
                                    )
                                }
                            }
                            append(styledMessage)
                        },
                        style = RiftTheme.typography.headerPrimary,
                    )
                }
            }
        }

        is Notification.ChatMessageNotification -> {
            NotificationTitle(
                title = buildAnnotatedString {
                    append("Chat message in ")
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
                        append(notification.channel)
                    }
                },
                onCloseClick = onCloseClick,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            ) {
                for (message in notification.messages) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (message.senderCharacterId != null) {
                            ClickableCharacter(message.senderCharacterId) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    DynamicCharacterPortraitStandings(
                                        characterId = message.senderCharacterId,
                                        size = 32.dp,
                                        standingLevel = message.senderStanding ?: Standing.Neutral,
                                        isAnimated = true,
                                    )
                                    Text(
                                        text = message.sender,
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                    if (message.senderStanding != null) {
                                        FlagIcon(message.senderStanding)
                                    }
                                    Text(
                                        text = " >",
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                }
                            }
                        }
                        Text(
                            text = buildAnnotatedString {
                                if (message.senderCharacterId == null) {
                                    append(message.sender)
                                    append(" > ")
                                }
                                appendMessageWithHighlight(message.message, message.highlight)
                            },
                            style = RiftTheme.typography.headerPrimary,
                        )
                    }
                }
            }
        }

        is Notification.JabberMessageNotification -> {
            NotificationTitle(
                title = buildAnnotatedString {
                    append("Jabber message ")
                    if (notification.sender == notification.chat) {
                        append("from ")
                    } else {
                        append("in ")
                    }
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
                        append(notification.chat)
                    }
                },
                onCloseClick = onCloseClick,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            if (notification.sender != notification.chat) {
                                append(notification.sender)
                                append(" > ")
                            }
                            appendMessageWithHighlight(notification.message, notification.highlight)
                        },
                        style = RiftTheme.typography.headerPrimary,
                    )
                }
            }
        }

        is Notification.IntelNotification -> {
            NotificationTitle(
                title = AnnotatedString(notification.title),
                onCloseClick = onCloseClick,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            ) {
                when (notification.locationMatch) {
                    is AlertsTriggerController.AlertLocationMatch.System -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val systemSubtext = when (val distance = notification.locationMatch.distance) {
                                0 -> "In the system"
                                1 -> "1 jump away"
                                else -> "$distance jumps away"
                            }
                            SolarSystem(notification.solarSystem, systemSubtext)
                            SolarSystem(notification.locationMatch.system, "Reference system")
                        }
                    }

                    is AlertsTriggerController.AlertLocationMatch.Character -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val systemSubtext = when (val distance = notification.locationMatch.distance) {
                                0 -> "In your system"
                                1 -> "1 jump away"
                                else -> "$distance jumps away"
                            }
                            SolarSystem(notification.solarSystem, systemSubtext)
                            Character(notification.locationMatch.characterId, "Your character")
                        }
                    }
                }
                Divider(color = RiftTheme.colors.borderGrey, modifier = Modifier.padding(start = Spacing.medium))
                SystemEntities(
                    entities = notification.systemEntities,
                    system = notification.solarSystem,
                    rowHeight = 32.dp,
                )
            }
        }

        is Notification.SovereigntyUpgradeImportNotification -> {
            NotificationTitle(
                title = AnnotatedString("Sovereignty upgrades saved"),
                onCloseClick = onCloseClick,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            ) {
                SolarSystem(notification.system, "Sovereignty Hub")
                notification.upgrades.forEach { upgrade ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AsyncTypeIcon(
                            type = upgrade,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = upgrade.name,
                            style = RiftTheme.typography.headerHighlighted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotatedString.Builder.appendMessageWithHighlight(message: String, highlight: String?) {
    if (highlight != null) {
        val index = message.lowercase().indexOf(highlight.lowercase())
        if (index >= 0) {
            append(message.take(index))
            withColor(RiftTheme.colors.textSpecialHighlighted) {
                append(message.drop(index).take(highlight.length))
            }
            append(message.drop(index + highlight.length))
        } else {
            append(message)
        }
    } else {
        append(message)
    }
}

@Composable
private fun NotificationTitle(
    title: AnnotatedString,
    onCloseClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = RiftTheme.typography.headerPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
                .padding(horizontal = Spacing.medium),
        )
        if (onCloseClick != null) {
            RiftImageButton(Res.drawable.window_titlebar_close, 16.dp, onCloseClick)
        }
    }
}

@Composable
private fun SolarSystem(system: MapSolarSystem, subtext: String?) {
    ClickableSystem(system.id) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            SystemIllustrationIconSmall(
                solarSystemId = system.id,
                size = 32.dp,
            )
            Column {
                Text(
                    text = system.name,
                    style = RiftTheme.typography.bodyLink,
                )
                if (subtext != null) {
                    Text(
                        text = subtext,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Character(characterId: Int, subtext: String) {
    val localCharactersRepository: LocalCharactersRepository by koin.inject()
    val name = localCharactersRepository.characters.value.firstOrNull { it.characterId == characterId }?.info?.name ?: "Character"
    ClickableCharacter(characterId) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            DynamicCharacterPortraitParallax(
                characterId = characterId,
                size = 32.dp,
                enterTimestamp = null,
                pointerInteractionStateHolder = null,
            )
            Column {
                Text(
                    text = name,
                    style = RiftTheme.typography.bodyHighlighted,
                )
                Text(
                    text = subtext,
                    style = RiftTheme.typography.bodyPrimary,
                )
            }
        }
    }
}
