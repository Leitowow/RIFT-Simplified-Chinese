package dev.nohus.rift.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.chat.ChatViewModel.Author
import dev.nohus.rift.chat.ChatViewModel.RichChatMessage
import dev.nohus.rift.chat.ChatViewModel.UiState
import dev.nohus.rift.chat.ChatsController.Channel
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ClickableCharacter
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.EntityInteractionProvider
import dev.nohus.rift.compose.FlagIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCircularCharacterPortrait
import dev.nohus.rift.compose.RiftContextMenuArea
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.TitleBarStyle
import dev.nohus.rift.compose.fadingRightEdge
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.getRelativeTime
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.keepScrolledToBottomItem
import dev.nohus.rift.compose.text.LinkedText
import dev.nohus.rift.compose.text.buildFormattedText
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitStandings
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.buttoniconplus
import dev.nohus.rift.generated.resources.chat_eve_system
import dev.nohus.rift.generated.resources.default_character
import dev.nohus.rift.generated.resources.map_marker_pilot_person
import dev.nohus.rift.generated.resources.window_chatchannels
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.utils.formatTime
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource
import java.time.Instant
import java.time.ZoneId

@Composable
fun ChatWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: ChatViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "Chat",
        icon = Res.drawable.window_chatchannels,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarStyle = TitleBarStyle.Minimal,
        titleBarContent = { height ->
            ToolbarRow(
                state = state,
                fixedHeight = height,
                onTabSelected = viewModel::onChannelSelected,
                onTabClosed = viewModel::onChannelCloseClick,
                onPlusClick = viewModel::onPlusClick,
            )
        },
        withContentPadding = false,
    ) {
        ChatWindowContent(
            state = state,
            onChannelClick = viewModel::onChannelSelected,
            onChannelOpenClick = viewModel::onChannelOpenClick,
            onChannelCloseClick = viewModel::onChannelCloseClick,
        )
    }
}

@Composable
private fun ToolbarRow(
    state: UiState,
    fixedHeight: Dp,
    onTabSelected: (Channel) -> Unit,
    onTabClosed: (Channel) -> Unit,
    onPlusClick: () -> Unit,
) {
    val channels = state.openChannels
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val unreadChannels = channels.filter {
            val viewed = state.lastViewedTimestamp[it] ?: Instant.EPOCH
            val lastMessage = state.lastMessageTimestamp[it] ?: Instant.EPOCH
            viewed < lastMessage
        }

        val tabs = remember(channels, unreadChannels) {
            val emptyChannel = if (channels.isEmpty()) {
                listOf(Tab(id = 0, title = "No open channels", isCloseable = false))
            } else {
                emptyList()
            }
            channels.mapIndexed { index, channel ->
                Tab(id = index, title = channel.name, isCloseable = true, isNotified = channel in unreadChannels)
            } + emptyChannel
        }
        val selectedTab = remember(state.selectedChannel, channels) {
            channels.indexOfFirst { it == state.selectedChannel }
        }
        RiftTabBar(
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                if (channels.isNotEmpty()) {
                    onTabSelected(channels[tab])
                }
            },
            onTabClosed = { tab ->
                if (channels.isNotEmpty()) {
                    onTabClosed(channels[tab])
                }
            },
            withUnderline = false,
            fixedHeight = fixedHeight,
            modifier = Modifier.weight(1f),
        )

        RiftImageButton(
            resource = Res.drawable.buttoniconplus,
            size = 20.dp,
            onClick = onPlusClick,
        )
    }
}

@Composable
private fun ChatWindowContent(
    state: UiState,
    onChannelClick: (Channel) -> Unit,
    onChannelOpenClick: (Channel) -> Unit = {},
    onChannelCloseClick: (Channel) -> Unit = {},
) {
    Column {
        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .offset(y = (-1).dp)
                .background(RiftTheme.colors.windowBorder),
        )

        if (state.selectedChannel != null) {
            key(state.selectedChannel) {
                ChatChannel(
                    channel = state.selectedChannel,
                    messages = state.messages,
                    displayTimezone = state.displayTimezone,
                )
            }
        } else {
            ChannelList(
                characters = state.characters,
                channels = state.channels,
                openChannels = state.openChannels,
                lastMessageTimestamp = state.lastMessageTimestamp,
                onChannelClick = onChannelClick,
                onChannelOpenClick = onChannelOpenClick,
                onChannelCloseClick = onChannelCloseClick,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ChatChannel(
    channel: Channel,
    messages: List<RichChatMessage>,
    displayTimezone: ZoneId,
) {
    Row {
        val listState = rememberLazyListState()
        // TODO: Empty state
        ScrollbarLazyColumn(
            listState = listState,
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            reverseLayout = true,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(Spacing.medium),
        ) {
            keepScrolledToBottomItem()
            items(
                items = messages.reversed(),
                key = { "${channel.name}-${channel.characterId}-${it.timestamp}-${it.author}-${it.message}" },
            ) { message ->
                ChatMessageItem(message, displayTimezone)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(RiftTheme.colors.windowBorder),
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(150.dp)
                .background(Color(0xFFFFFFFF).copy(alpha = 0.05f))
                .padding(Spacing.medium),
        ) {
            val characters = messages
                .mapNotNull { (it.author as? Author.Character)?.characterDetails }
                .distinct()
                .sortedBy { it.name }
            RiftTooltipArea(
                text = "This counter and list shows characters that have recently sent a message, and not all characters in the channel. Similar to wormhole local in-game.",
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.map_marker_pilot_person),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(RiftTheme.colors.textPrimary),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = characters.size.toString(),
                        style = RiftTheme.typography.detailPrimary,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.small))
            // TODO: Empty state
            ScrollbarLazyColumn(
                scrollbarModifier = Modifier.fillMaxHeight(),
                scrollbarBackground = Color(0xFF000000).copy(alpha = 0.25f),
            ) {
                items(characters) { character ->
                    Box(
                        modifier = Modifier
                            .animateItem()
                            .hoverBackground()
                            .pointerHoverIcon(PointerIcon(Cursors.pointerDropdown)),
                    ) {
                        val interactionProvider: EntityInteractionProvider = remember { koin.get() }
                        val interaction = interactionProvider.getCharacter(character.characterId)
                        RiftContextMenuArea(
                            items = interaction.contextMenuItems,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            ) {
                                FlagIcon(
                                    standing = character.standingLevel,
                                )
                                Text(
                                    text = character.name,
                                    style = RiftTheme.typography.bodyPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible,
                                    softWrap = false,
                                    modifier = Modifier
                                        .padding(end = Spacing.small)
                                        .fadingRightEdge()
                                        .fillMaxWidth(),
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
private fun LazyItemScope.ChatMessageItem(
    message: RichChatMessage,
    displayTimezone: ZoneId,
) {
    Box(
        modifier = Modifier
            .animateItem()
            .hoverBackground(),
    ) {
        RiftContextMenuArea(
            items = listOf(
                ContextMenuItem.TextItem("Copy") {
                    println("Copy")
                },
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (message.author) {
                    is Author.Character -> if (message.author.characterId != null) {
                        DynamicCharacterPortraitStandings(
                            characterId = message.author.characterId,
                            size = 32.dp,
                            standingLevel = message.author.characterDetails?.standingLevel ?: Standing.Neutral,
                            isAnimated = false,
                        )
                    } else {
                        Image(
                            painter = painterResource(Res.drawable.default_character),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Author.EveSystem -> {
                        Image(
                            painter = painterResource(Res.drawable.chat_eve_system),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.small))

                val text = buildFormattedText {
                    append("[${formatTime(message.timestamp, displayTimezone)}]")
                    append(" ")
                    append(message.authorText)
                    append(" > ")
                    append(message.message)
                }
                LinkedText(
                    text = text,
                    style = RiftTheme.typography.bodyPrimary,
                )
            }
        }
    }
}

@Composable
private fun ChannelList(
    characters: List<LocalCharacter>,
    channels: List<Channel>,
    openChannels: List<Channel>,
    lastMessageTimestamp: Map<Channel, Instant>,
    onChannelClick: (Channel) -> Unit,
    onChannelOpenClick: (Channel) -> Unit,
    onChannelCloseClick: (Channel) -> Unit,
) {
    // TODO: Empty state
    Column(
        modifier = Modifier.padding(top = Spacing.large, bottom = Spacing.large, start = Spacing.medium, end = Spacing.large),
    ) {
        ScrollbarLazyColumn {
            item(key = "header") {
                Column {
                    Text(
                        text = "Choose channels to open",
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.padding(start = Spacing.medium, bottom = Spacing.large),
                    )
                    Divider(color = RiftTheme.colors.divider)
                    Spacer(Modifier.padding(top = Spacing.medium))
                }
            }

            val sortedChannels = channels.sortedByDescending { lastMessageTimestamp[it] ?: Instant.EPOCH }
            items(sortedChannels, key = { "channel-$it" }) { channel ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .animateItem()
                        .hoverBackground()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                ) {
                    Column {
                        Text(
                            text = channel.name,
                            style = RiftTheme.typography.bodyPrimary,
                        )

                        if (channel.characterId != null) {
                            val character = characters.firstOrNull { it.characterId == channel.characterId }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val name = character?.info?.name ?: "Character ${channel.characterId}"
                                RiftCircularCharacterPortrait(
                                    characterId = channel.characterId,
                                    name = name,
                                    hasPadding = false,
                                    16.dp,
                                )
                                Text(
                                    text = name,
                                    style = RiftTheme.typography.detailSecondary,
                                )
                            }
                        }
                    }

                    val timestamp = lastMessageTimestamp[channel]
                    if (timestamp != null) {
                        val now = getNow()
                        val age = key(now) { getRelativeTime(timestamp, ZoneId.systemDefault(), now) }
                        Text(
                            text = age,
                            style = RiftTheme.typography.bodySecondary,
                            modifier = Modifier.padding(start = Spacing.medium),
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    if (channel in openChannels) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            RiftButton(
                                text = "Close",
                                type = ButtonType.Secondary,
                                cornerCut = ButtonCornerCut.BottomLeft,
                                onClick = { onChannelCloseClick(channel) },
                            )
                            RiftButton(
                                text = "View",
                                onClick = { onChannelClick(channel) },
                            )
                        }
                    } else {
                        RiftButton(
                            text = "Open",
                            onClick = { onChannelOpenClick(channel) },
                        )
                    }
                }
            }
        }
    }
}
