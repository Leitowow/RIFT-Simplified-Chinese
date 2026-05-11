package dev.nohus.rift.jabber

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.ContextMenuItem.CheckboxItem
import dev.nohus.rift.compose.ContextMenuItem.TextItem
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuArea
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftToggleButton
import dev.nohus.rift.compose.RiftWarningBanner
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.ToggleButtonType
import dev.nohus.rift.compose.annotateLinks
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.bee
import dev.nohus.rift.generated.resources.logout
import dev.nohus.rift.generated.resources.window_chatchannels
import dev.nohus.rift.jabber.JabberAccountRepository.JabberAccountResult.JabberAccount
import dev.nohus.rift.jabber.JabberViewModel.ContactListState
import dev.nohus.rift.jabber.JabberViewModel.TabModel
import dev.nohus.rift.jabber.JabberViewModel.UiState
import dev.nohus.rift.jabber.client.MultiUserChatController.MultiUserMessage
import dev.nohus.rift.jabber.client.RosterUsersController.RosterUser
import dev.nohus.rift.jabber.client.UserChatController.UserChat
import dev.nohus.rift.jabber.client.UserChatController.UserMessage
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smackx.muc.MultiUserChat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private enum class RescueTrapType(val label: String) {
    SmallGang("杂鱼收割队"),
    Bombers("纯隐轰队"),
    BombersKiki("隐轰加奇奇莫拉队"),
    BlackOps("黑隐队"),
}

private enum class RescueEnemyCountType(val label: String) {
    LessThanTen("小于10人"),
    BetweenTwentyAndForty("20-40人"),
    AboveFifty("50人以上"),
}

private enum class RescueLocationType(val label: String) {
    Belt("矿带"),
    Ice("冰矿"),
    Moon("月矿"),
}

private fun buildRescuePingMessage(
    rorqualPilot: String,
    system: String,
    locationType: RescueLocationType,
    enemyCountType: RescueEnemyCountType,
    trapType: RescueTrapType,
    cynoPilot: String,
): String {
    val locationText = when (locationType) {
        RescueLocationType.Belt -> "Ore Depot"
        RescueLocationType.Ice -> "ICE Anomaly"
        RescueLocationType.Moon -> "Moon"
    }
    val enemyCountText = when (enemyCountType) {
        RescueEnemyCountType.LessThanTen -> "<10"
        RescueEnemyCountType.BetweenTwentyAndForty -> "30"
        RescueEnemyCountType.AboveFifty -> "50+"
    }
    val trapTypeText = when (trapType) {
        RescueTrapType.SmallGang -> "Roaming Gang"
        RescueTrapType.Bombers -> "bomber fleet"
        RescueTrapType.BombersKiki -> "bomber+Kiki fleet"
        RescueTrapType.BlackOps -> "Blop fleet"
    }

    return buildString {
        appendLine("!bping all")
        appendLine("RORQUAL TACKLED")
        appendLine("Rorq pilot: $rorqualPilot")
        appendLine("System: $system")
        appendLine("Loc: $locationText")
        appendLine("Dscan: about $enemyCountText+ $trapTypeText")
        append("Cyno 300k Off - YES , $cynoPilot")
    }
}

@Composable
fun JabberWindow(
    inputModel: JabberInputModel,
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: JabberViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Jabber",
        icon = Res.drawable.window_chatchannels,
        state = windowState,
        tuneContextMenuItems = getTuneContextMenuItems(state, viewModel),
        onCloseClick = onCloseRequest,
        withContentPadding = false,
    ) {
        JabberWindowContent(
            state = state,
            onTabSelect = viewModel::onTabSelect,
            onImportClick = viewModel::onImportClick,
            onLoginClick = viewModel::onLoginClick,
            onConnectClick = viewModel::onConnectClick,
            onRosterUserClick = viewModel::onRosterUserClick,
            onRosterUserRemove = viewModel::onRosterUserRemove,
            onChatRoomClick = viewModel::onChatRoomClick,
            onChatRoomRemove = viewModel::onChatRoomRemove,
            onChatClosed = viewModel::onChatClosed,
            onChatRoomClosed = viewModel::onChatRoomClosed,
            onAddContactClick = viewModel::onAddContactClick,
            onAddChatRoomClick = viewModel::onAddChatRoomClick,
            onAddContactSubmitClick = viewModel::onAddContactSubmitClick,
            onAddChatRoomSubmitClick = viewModel::onAddChatRoomSubmitClick,
            onBackClick = viewModel::onBackClick,
            onMessageSend = viewModel::onMessageSend,
            onChatRoomMessageSend = viewModel::onMessageSend,
            onCollapsedGroupToggle = viewModel::onCollapsedGroupToggle,
        )
    }
}

private fun getTuneContextMenuItems(
    state: UiState,
    viewModel: JabberViewModel,
): List<ContextMenuItem>? {
    val canLogout = when (state) {
        is UiState.NoAccount -> false
        is UiState.Login -> false
        UiState.Connecting -> true
        is UiState.LoggedIn -> true
    }
    val isUsingBiggerFontSize = (state as? UiState.LoggedIn)?.isUsingBiggerFontSize
    return buildList {
        if (canLogout) add(TextItem("退出登陆", Res.drawable.logout, onClick = viewModel::onLogoutClick))
        if (isUsingBiggerFontSize != null) add(CheckboxItem("切换至大号字体", isSelected = isUsingBiggerFontSize, onClick = viewModel::onBiggerFontSizeClick))
    }.takeIf { it.isNotEmpty() }
}

@Composable
private fun JabberWindowContent(
    state: UiState,
    onTabSelect: (TabModel) -> Unit,
    onImportClick: () -> Unit,
    onLoginClick: () -> Unit,
    onConnectClick: (jidLocalPart: String, password: String) -> Unit,
    onRosterUserClick: (RosterUser) -> Unit,
    onRosterUserRemove: (RosterUser) -> Unit,
    onChatRoomClick: (MultiUserChat) -> Unit,
    onChatRoomRemove: (MultiUserChat) -> Unit,
    onChatClosed: (Chat) -> Unit,
    onChatRoomClosed: (MultiUserChat) -> Unit,
    onAddContactClick: () -> Unit,
    onAddChatRoomClick: () -> Unit,
    onAddContactSubmitClick: (jidLocalPart: String, name: String, groups: List<String>) -> Unit,
    onAddChatRoomSubmitClick: (jidLocalPart: String) -> Unit,
    onBackClick: () -> Unit,
    onMessageSend: (UserChat, String) -> Unit,
    onChatRoomMessageSend: (MultiUserChat, String) -> Unit,
    onCollapsedGroupToggle: (String) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it.contentKey },
    ) { state ->
        when (state) {
            UiState.Connecting -> ConnectingContent()
            is UiState.NoAccount -> NoAccountContent(
                state = state,
                onImportClick = onImportClick,
                onLoginClick = onLoginClick,
            )
            is UiState.Login -> LoginContent(
                state = state,
                onConnectClick = onConnectClick,
            )
            is UiState.LoggedIn -> LoggedInContent(
                state = state,
                onTabSelect = onTabSelect,
                onRosterUserClick = onRosterUserClick,
                onRosterUserRemove = onRosterUserRemove,
                onChatRoomClick = onChatRoomClick,
                onChatRoomRemove = onChatRoomRemove,
                onChatClosed = onChatClosed,
                onChatRoomClosed = onChatRoomClosed,
                onAddContactClick = onAddContactClick,
                onAddChatRoomClick = onAddChatRoomClick,
                onAddContactSubmitClick = onAddContactSubmitClick,
                onAddChatRoomSubmitClick = onAddChatRoomSubmitClick,
                onBackClick = onBackClick,
                onMessageSend = onMessageSend,
                onChatRoomMessageSend = onChatRoomMessageSend,
                onCollapsedGroupToggle = onCollapsedGroupToggle,
            )
        }
    }
}

@Composable
private fun ConnectingContent() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column {
            LoadingSpinner()
            Text(
                text = "正在连接…",
                style = RiftTheme.typography.headerPrimary,
                modifier = Modifier
                    .padding(top = Spacing.large),
            )
        }
    }
}

@Composable
private fun NoAccountContent(
    state: UiState.NoAccount,
    onImportClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(Spacing.medium),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.bee),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally),
                )
                Text(
                    text = "Goonswarm Jabber",
                    style = RiftTheme.typography.headlineHighlighted,
                    textAlign = TextAlign.Center,
                )
                val isUsingPidgin = state.canImport
                val text = if (isUsingPidgin) {
                    "\nRIFT 是一个为 Goonswarm 提供额外特性的 Jabber 客户端。\n\n" +
                        "无需运行 Pidgin，也能更方便地接收集结通知。"
                } else {
                    "\nRIFT 是一个为 Goonswarm 提供额外特性的 Jabber 客户端。\n\n" +
                        "无需额外运行其他应用，也能更方便地接收集结通知。"
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.headerPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (state.canImport) {
            RiftButton(
                text = "导入 Pidgin 账号",
                cornerCut = ButtonCornerCut.Both,
                onClick = onImportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.medium),
            )
            RiftButton(
                text = "手动登陆",
                type = ButtonType.Secondary,
                cornerCut = ButtonCornerCut.Both,
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.medium),
            )
        } else {
            RiftButton(
                text = "开始使用",
                cornerCut = ButtonCornerCut.Both,
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.medium),
            )
        }
    }
}

@Composable
private fun LoginContent(
    state: UiState.Login,
    onConnectClick: (jidLocalPart: String, password: String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(Spacing.medium),
    ) {
        val account = remember { koin.get<JabberAccountRepository>().getAccount() as? JabberAccount }
        var jidLocalPart by remember { mutableStateOf(account?.jid?.substringBeforeLast("@") ?: "") }
        val savedPassword = remember { account?.password }
        var password by remember { mutableStateOf("") }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.bee),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally),
                )
                Text(
                    text = "Goonswarm Jabber",
                    style = RiftTheme.typography.headlineHighlighted,
                    textAlign = TextAlign.Center,
                )
                if (state.errorMessage != null) {
                    RiftWarningBanner(
                        text = state.errorMessage,
                        modifier = Modifier.padding(top = Spacing.medium),
                    )
                }
                Column(
                    modifier = Modifier.padding(top = Spacing.medium),
                ) {
                    Text(
                        text = "用户名",
                        style = RiftTheme.typography.headerPrimary,
                    )
                    RiftTextField(
                        text = jidLocalPart,
                        placeholder = "请输入用户名",
                        onTextChanged = { jidLocalPart = it },
                        modifier = Modifier
                            .width(200.dp)
                            .padding(top = Spacing.small),
                    )
                    Text(
                        text = "密码",
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier
                            .padding(top = Spacing.small),
                    )
                    RiftTextField(
                        text = password,
                        placeholder = if (savedPassword != null) "（如果密码未更改，直接点击登陆）" else "请输入密码",
                        isPassword = true,
                        onTextChanged = { password = it },
                        modifier = Modifier
                            .width(200.dp)
                            .padding(top = Spacing.small),
                    )
                    LinkText(
                        text = "忘记用户名或密码？",
                        onClick = { "https://goonfleet.com/esa/".toURIOrNull()?.openBrowser() },
                        modifier = Modifier.padding(top = Spacing.small),
                    )
                }
            }
        }
        RiftButton(
            text = "登陆",
            cornerCut = ButtonCornerCut.Both,
            onClick = {
                val effectivePassword = password.takeIf { it.isNotEmpty() } ?: savedPassword ?: password
                onConnectClick(jidLocalPart, effectivePassword)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.medium),
        )
    }
}

@Composable
private fun LoggedInContent(
    state: UiState.LoggedIn,
    onTabSelect: (TabModel) -> Unit,
    onRosterUserClick: (RosterUser) -> Unit,
    onRosterUserRemove: (RosterUser) -> Unit,
    onChatRoomClick: (MultiUserChat) -> Unit,
    onChatRoomRemove: (MultiUserChat) -> Unit,
    onChatClosed: (Chat) -> Unit,
    onChatRoomClosed: (MultiUserChat) -> Unit,
    onAddContactClick: () -> Unit,
    onAddChatRoomClick: () -> Unit,
    onAddContactSubmitClick: (jidLocalPart: String, name: String, groups: List<String>) -> Unit,
    onAddChatRoomSubmitClick: (jidLocalPart: String) -> Unit,
    onBackClick: () -> Unit,
    onMessageSend: (UserChat, String) -> Unit,
    onCollapsedGroupToggle: (String) -> Unit,
    onChatRoomMessageSend: (MultiUserChat, String) -> Unit,
) {
    Column {
        val contactsTab = Tab(id = 0, "联系人", false, payload = TabModel.Contacts)
        val chatRoomTabs = state.jabberState.openMultiUserChats.withIndex().associateWith { (index, chat) ->
            Tab(
                id = index + 1,
                title = chat.localpartOrNull?.toString() ?: "",
                isCloseable = true,
                isNotified = chat in state.unreadChats,
                payload = TabModel.MultiUserChat(chat),
            )
        }
        val chatTabs = state.jabberState.userChats.withIndex().associateWith { (index, chat) ->
            Tab(
                id = index + 1 + chatRoomTabs.size,
                title = chat.name,
                isCloseable = true,
                isNotified = chat.chat.xmppAddressOfChatPartner in state.unreadChats,
                payload = TabModel.UserChat(chat),
            )
        }
        val selectedTab = when (val selectedTab = state.selectedTab) {
            TabModel.Contacts -> 0
            is TabModel.MultiUserChat -> chatRoomTabs.entries.firstOrNull { it.key.value == selectedTab.chat }?.value?.id ?: 0
            is TabModel.UserChat -> chatTabs.entries.firstOrNull { it.key.value.chat == selectedTab.chat.chat }?.value?.id ?: 0
        }
        val tabs = listOf(contactsTab) + chatRoomTabs.values + chatTabs.values

        RiftTabBar(
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = { id ->
                val model = tabs.firstOrNull { it.id == id }?.payload as? TabModel ?: return@RiftTabBar
                onTabSelect(model)
            },
            onTabClosed = { id ->
                val model = tabs.firstOrNull { it.id == id }?.payload as? TabModel ?: return@RiftTabBar
                when (model) {
                    TabModel.Contacts -> {}
                    is TabModel.MultiUserChat -> state.jabberState.multiUserChats.firstOrNull { it.room == model.chat }?.let { onChatRoomClosed(it) }
                    is TabModel.UserChat -> onChatClosed(model.chat.chat)
                }
                onTabSelect(TabModel.Contacts)
            },
            modifier = Modifier
                .fillMaxWidth(),
        )
        if (selectedTab == 0) {
            AnimatedContent(
                state.contactListState,
                modifier = Modifier.weight(1f),
            ) { contactListState ->
                when (contactListState) {
                    ContactListState.Contacts -> {
                        Column {
                            ContactsList(
                                multiUserChats = state.jabberState.multiUserChats,
                                users = state.jabberState.users.values.toList(),
                                collapsedGroups = state.collapsedGroups,
                                onRosterUserClick = onRosterUserClick,
                                onRosterUserRemove = onRosterUserRemove,
                                onChatRoomClick = onChatRoomClick,
                                onChatRoomRemove = onChatRoomRemove,
                                onCollapsedGroupToggle = onCollapsedGroupToggle,
                                modifier = Modifier.weight(1f),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.medium)
                                    .padding(bottom = Spacing.medium),
                            ) {
                                RiftButton(
                                    text = "添加联系人",
                                    cornerCut = ButtonCornerCut.BottomLeft,
                                    onClick = onAddContactClick,
                                    modifier = Modifier.weight(1f),
                                )
                                RiftButton(
                                    text = "添加聊天室",
                                    cornerCut = ButtonCornerCut.BottomRight,
                                    onClick = onAddChatRoomClick,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    is ContactListState.AddContact -> {
                        AddContact(
                            error = contactListState.error,
                            onAddContactSubmitClick = onAddContactSubmitClick,
                            onBackClick = onBackClick,
                        )
                    }
                    is ContactListState.AddChatRoom -> {
                        AddChatRoom(
                            error = contactListState.error,
                            onAddChatRoomSubmitClick = onAddChatRoomSubmitClick,
                            onBackClick = onBackClick,
                        )
                    }
                }
            }
        } else {
            val chatRoom = chatRoomTabs.entries.firstOrNull { it.value.id == selectedTab }?.key?.value
                ?.let { jid -> state.jabberState.multiUserChats.firstOrNull { it.room == jid } }
            if (chatRoom != null) {
                val messages = state.jabberState.multiUserChatMessages[chatRoom] ?: emptyList()
                MultiUserChat(
                    multiUserChat = chatRoom,
                    subject = state.jabberState.multiUserChatSubjects[chatRoom.room],
                    messages = messages,
                    isUsingBiggerFontSize = state.isUsingBiggerFontSize,
                    onMessageSend = { onChatRoomMessageSend(chatRoom, it) },
                )
            } else {
                val userChat = chatTabs.entries.first { it.value.id == selectedTab }.key.value
                val rosterUser = state.jabberState.users[userChat.chat.xmppAddressOfChatPartner]
                val messages = state.jabberState.userChatMessages[userChat.chat] ?: emptyList()
                key(userChat) {
                    UserChat(
                        userChat = userChat,
                        rosterUser = rosterUser,
                        messages = messages,
                        isUsingBiggerFontSize = state.isUsingBiggerFontSize,
                        onMessageSend = { onMessageSend(userChat, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddContact(
    error: String?,
    onAddContactSubmitClick: (jidLocalPart: String, name: String, groups: List<String>) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        Text(
            text = "添加联系人",
            style = RiftTheme.typography.headerPrimary,
        )
        var jidLocalPart by remember { mutableStateOf("") }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RiftTextField(
                text = jidLocalPart,
                placeholder = "用户名",
                onTextChanged = {
                    jidLocalPart = it
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "@goonfleet.com",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
        Text(
            text = "设置昵称（可选）",
            style = RiftTheme.typography.headerPrimary,
        )
        var name by remember { mutableStateOf("") }
        RiftTextField(
            text = name,
            placeholder = "昵称",
            onTextChanged = {
                name = it
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "设置分组（可选）",
            style = RiftTheme.typography.headerPrimary,
        )
        var group by remember { mutableStateOf("") }
        RiftTextField(
            text = group,
            placeholder = "分组",
            onTextChanged = {
                group = it
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Text(
                text = error,
                style = RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.borderError),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            RiftButton(
                text = "返回",
                type = ButtonType.Secondary,
                cornerCut = ButtonCornerCut.BottomLeft,
                onClick = { onBackClick() },
                modifier = Modifier.weight(1f),
            )
            RiftButton(
                text = "添加联系人",
                cornerCut = ButtonCornerCut.BottomRight,
                onClick = {
                    val name = name.takeIf { it.isNotBlank() } ?: jidLocalPart
                    onAddContactSubmitClick(jidLocalPart, name, listOf(group))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AddChatRoom(
    error: String?,
    onAddChatRoomSubmitClick: (jidLocalPart: String) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        Text(
            text = "添加聊天室",
            style = RiftTheme.typography.headerPrimary,
        )
        var jidLocalPart by remember { mutableStateOf("") }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RiftTextField(
                text = jidLocalPart,
                placeholder = "聊天室名称",
                onTextChanged = {
                    jidLocalPart = it
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "@conference.goonfleet.com",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.borderError),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            RiftButton(
                text = "返回",
                type = ButtonType.Secondary,
                cornerCut = ButtonCornerCut.BottomLeft,
                onClick = { onBackClick() },
                modifier = Modifier.weight(1f),
            )
            RiftButton(
                text = "添加聊天室",
                cornerCut = ButtonCornerCut.BottomRight,
                onClick = { onAddChatRoomSubmitClick(jidLocalPart) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun UserChat(
    userChat: UserChat,
    rosterUser: RosterUser?,
    messages: List<UserMessage>,
    isUsingBiggerFontSize: Boolean,
    onMessageSend: (String) -> Unit,
) {
    var isRescueSettingsOpen by remember { mutableStateOf(false) }
    var messageInput by remember { mutableStateOf("") }
    var rescueTrapType by remember { mutableStateOf<RescueTrapType?>(null) }
    var rescueEnemyCountType by remember { mutableStateOf<RescueEnemyCountType?>(null) }
    var rescueSystem by remember { mutableStateOf("") }
    var bigFishPilotName by remember { mutableStateOf("") }
    var cynoPilotName by remember { mutableStateOf("") }
    var rescueLocationType by remember { mutableStateOf<RescueLocationType?>(null) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(Spacing.medium),
        ) {
            rosterUser?.bestPresence?.let {
                PresenceIndicatorDot(
                    presence = it,
                    isSubscriptionPending = rosterUser.isSubscriptionPending,
                )
            }
            Text(
                text = userChat.name,
                style = RiftTheme.typography.headerPrimary,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RiftTheme.colors.borderGreyLight),
        ) {}
        if (isRescueSettingsOpen) {
            RescueQuickCallContent(
                rescueTrapType = rescueTrapType,
                onRescueTrapTypeChange = { rescueTrapType = it },
                rescueEnemyCountType = rescueEnemyCountType,
                onRescueEnemyCountTypeChange = { rescueEnemyCountType = it },
                rescueSystem = rescueSystem,
                onRescueSystemChange = { rescueSystem = it },
                bigFishPilotName = bigFishPilotName,
                onBigFishPilotNameChange = { bigFishPilotName = it },
                cynoPilotName = cynoPilotName,
                onCynoPilotNameChange = { cynoPilotName = it },
                rescueLocationType = rescueLocationType,
                onRescueLocationTypeChange = { rescueLocationType = it },
                onCallClick = {
                    val trapType = rescueTrapType ?: return@RescueQuickCallContent
                    val enemyCount = rescueEnemyCountType ?: return@RescueQuickCallContent
                    val locationType = rescueLocationType ?: return@RescueQuickCallContent
                    messageInput = buildRescuePingMessage(
                        rorqualPilot = bigFishPilotName,
                        system = rescueSystem,
                        locationType = locationType,
                        enemyCountType = enemyCount,
                        trapType = trapType,
                        cynoPilot = cynoPilotName,
                    )
                    isRescueSettingsOpen = false
                },
                onBackClick = { isRescueSettingsOpen = false },
                modifier = Modifier.weight(1f),
            )
        } else {
            val listState = rememberLazyListState()
            var lastMessage by remember { mutableStateOf(messages.lastOrNull()) }
            LaunchedEffect(messages) {
                val newLastMessage = messages.lastOrNull()
                if (newLastMessage != lastMessage) {
                    if (messages.lastIndex >= 0) listState.animateScrollToItem(messages.lastIndex)
                    lastMessage = newLastMessage
                }
            }
            LaunchedEffect(Unit) {
                if (messages.lastIndex >= 0) listState.animateScrollToItem(messages.lastIndex)
            }
            ScrollbarLazyColumn(
                listState = listState,
                scrollbarModifier = Modifier.padding(Spacing.small),
                modifier = Modifier.weight(1f),
            ) {
                items(messages) {
                    ChatMessage(isUsingBiggerFontSize, userChat, it)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier
                    .padding(horizontal = Spacing.medium)
                    .padding(bottom = Spacing.medium),
            ) {
                RiftTextField(
                    text = messageInput,
                    onTextChanged = { messageInput = it },
                    singleLine = false,
                    minLines = 1,
                    maxLines = 6,
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent {
                            if (it.key == Key.Enter) {
                                onMessageSend(messageInput)
                                messageInput = ""
                                true
                            } else {
                                false
                            }
                        },
                )
                RiftButton(
                    text = "发送",
                    onClick = {
                        onMessageSend(messageInput)
                        messageInput = ""
                    },
                )
                RiftButton(
                    text = "一键呼叫救援",
                    type = ButtonType.Secondary,
                    onClick = {
                        rescueTrapType = null
                        rescueEnemyCountType = null
                        rescueLocationType = null
                        isRescueSettingsOpen = true
                    },
                )
            }
        }
    }
}

@Composable
private fun RescueQuickCallContent(
    rescueTrapType: RescueTrapType?,
    onRescueTrapTypeChange: (RescueTrapType?) -> Unit,
    rescueEnemyCountType: RescueEnemyCountType?,
    onRescueEnemyCountTypeChange: (RescueEnemyCountType?) -> Unit,
    rescueSystem: String,
    onRescueSystemChange: (String) -> Unit,
    bigFishPilotName: String,
    onBigFishPilotNameChange: (String) -> Unit,
    cynoPilotName: String,
    onCynoPilotNameChange: (String) -> Unit,
    rescueLocationType: RescueLocationType?,
    onRescueLocationTypeChange: (RescueLocationType?) -> Unit,
    onCallClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMissingInfoWarning by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = modifier.padding(Spacing.medium),
    ) {
        if (showMissingInfoWarning) {
            RiftWarningBanner(
                text = "有关键信息未填写，请检查",
            )
        }
        Text(
            text = "你被什么抓住了",
            style = RiftTheme.typography.headerPrimary,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                RiftToggleButton(
                    text = RescueTrapType.SmallGang.label,
                    isSelected = rescueTrapType == RescueTrapType.SmallGang,
                    type = ToggleButtonType.Left,
                    onClick = {
                        showMissingInfoWarning = false
                        onRescueTrapTypeChange(RescueTrapType.SmallGang)
                    },
                    modifier = Modifier.weight(1f),
                )
                RiftToggleButton(
                    text = RescueTrapType.Bombers.label,
                    isSelected = rescueTrapType == RescueTrapType.Bombers,
                    type = ToggleButtonType.Middle,
                    onClick = {
                        showMissingInfoWarning = false
                        onRescueTrapTypeChange(RescueTrapType.Bombers)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                RiftToggleButton(
                    text = RescueTrapType.BombersKiki.label,
                    isSelected = rescueTrapType == RescueTrapType.BombersKiki,
                    type = ToggleButtonType.Left,
                    onClick = {
                        showMissingInfoWarning = false
                        onRescueTrapTypeChange(RescueTrapType.BombersKiki)
                    },
                    modifier = Modifier.weight(1f),
                )
                RiftToggleButton(
                    text = RescueTrapType.BlackOps.label,
                    isSelected = rescueTrapType == RescueTrapType.BlackOps,
                    type = ToggleButtonType.Right,
                    onClick = {
                        showMissingInfoWarning = false
                        onRescueTrapTypeChange(RescueTrapType.BlackOps)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            text = "敌人的数量",
            style = RiftTheme.typography.headerPrimary,
            modifier = Modifier.padding(top = Spacing.medium),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftToggleButton(
                text = RescueEnemyCountType.LessThanTen.label,
                isSelected = rescueEnemyCountType == RescueEnemyCountType.LessThanTen,
                type = ToggleButtonType.Left,
                onClick = {
                    showMissingInfoWarning = false
                    onRescueEnemyCountTypeChange(RescueEnemyCountType.LessThanTen)
                },
                modifier = Modifier.weight(1f),
            )
            RiftToggleButton(
                text = RescueEnemyCountType.BetweenTwentyAndForty.label,
                isSelected = rescueEnemyCountType == RescueEnemyCountType.BetweenTwentyAndForty,
                type = ToggleButtonType.Middle,
                onClick = {
                    showMissingInfoWarning = false
                    onRescueEnemyCountTypeChange(RescueEnemyCountType.BetweenTwentyAndForty)
                },
                modifier = Modifier.weight(1f),
            )
            RiftToggleButton(
                text = RescueEnemyCountType.AboveFifty.label,
                isSelected = rescueEnemyCountType == RescueEnemyCountType.AboveFifty,
                type = ToggleButtonType.Right,
                onClick = {
                    showMissingInfoWarning = false
                    onRescueEnemyCountTypeChange(RescueEnemyCountType.AboveFifty)
                },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "所在星系",
            style = RiftTheme.typography.headerPrimary,
            modifier = Modifier.padding(top = Spacing.medium),
        )
        RiftTextField(
            text = rescueSystem,
            placeholder = "请输入所在星系",
            onTextChanged = {
                showMissingInfoWarning = false
                onRescueSystemChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "大鱼驾驶员角色名",
            style = RiftTheme.typography.headerPrimary,
            modifier = Modifier.padding(top = Spacing.small),
        )
        RiftTextField(
            text = bigFishPilotName,
            placeholder = "请输入大鱼驾驶员角色名",
            onTextChanged = {
                showMissingInfoWarning = false
                onBigFishPilotNameChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "诱导驾驶员角色名",
            style = RiftTheme.typography.headerPrimary,
            modifier = Modifier.padding(top = Spacing.small),
        )
        RiftTextField(
            text = cynoPilotName,
            placeholder = "请输入诱导驾驶员角色名",
            onTextChanged = {
                showMissingInfoWarning = false
                onCynoPilotNameChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "地点类型",
            style = RiftTheme.typography.headerPrimary,
            modifier = Modifier.padding(top = Spacing.medium),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftToggleButton(
                text = RescueLocationType.Belt.label,
                isSelected = rescueLocationType == RescueLocationType.Belt,
                type = ToggleButtonType.Left,
                onClick = {
                    showMissingInfoWarning = false
                    onRescueLocationTypeChange(RescueLocationType.Belt)
                },
                modifier = Modifier.weight(1f),
            )
            RiftToggleButton(
                text = RescueLocationType.Ice.label,
                isSelected = rescueLocationType == RescueLocationType.Ice,
                type = ToggleButtonType.Middle,
                onClick = {
                    showMissingInfoWarning = false
                    onRescueLocationTypeChange(RescueLocationType.Ice)
                },
                modifier = Modifier.weight(1f),
            )
            RiftToggleButton(
                text = RescueLocationType.Moon.label,
                isSelected = rescueLocationType == RescueLocationType.Moon,
                type = ToggleButtonType.Right,
                onClick = {
                    showMissingInfoWarning = false
                    onRescueLocationTypeChange(RescueLocationType.Moon)
                },
                modifier = Modifier.weight(1f),
            )
        }

        Box(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftButton(
                text = "返回聊天",
                type = ButtonType.Secondary,
                cornerCut = ButtonCornerCut.BottomLeft,
                onClick = onBackClick,
                modifier = Modifier.weight(1f),
            )
            RiftButton(
                text = "一键呼救",
                cornerCut = ButtonCornerCut.BottomRight,
                onClick = {
                    val hasMissingRequiredInfo = rescueTrapType == null ||
                        rescueEnemyCountType == null ||
                        rescueLocationType == null ||
                        rescueSystem.isBlank() ||
                        bigFishPilotName.isBlank() ||
                        cynoPilotName.isBlank()
                    if (hasMissingRequiredInfo) {
                        showMissingInfoWarning = true
                    } else {
                        onCallClick()
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MultiUserChat(
    multiUserChat: MultiUserChat,
    subject: String?,
    messages: List<MultiUserMessage>,
    isUsingBiggerFontSize: Boolean,
    onMessageSend: (String) -> Unit,
) {
    var isRescueSettingsOpen by remember { mutableStateOf(false) }
    var messageInput by remember { mutableStateOf("") }
    var rescueTrapType by remember { mutableStateOf<RescueTrapType?>(null) }
    var rescueEnemyCountType by remember { mutableStateOf<RescueEnemyCountType?>(null) }
    var rescueSystem by remember { mutableStateOf("") }
    var bigFishPilotName by remember { mutableStateOf("") }
    var cynoPilotName by remember { mutableStateOf("") }
    var rescueLocationType by remember { mutableStateOf<RescueLocationType?>(null) }

    Column {
        ScrollbarColumn(
            modifier = Modifier
                .heightIn(max = 85.dp)
                .padding(Spacing.medium),
        ) {
            if (subject != null) {
                val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
                val linkifiedSubject = remember(subject) { annotateLinks(subject.trim(), linkStyle) }
                Text(
                    text = linkifiedSubject,
                    style = RiftTheme.typography.bodyPrimary,
                )
            } else {
                Text(
                    text = multiUserChat.room.localpartOrNull?.toString() ?: "",
                    style = RiftTheme.typography.headerPrimary,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RiftTheme.colors.borderGreyLight),
        ) {}
        if (isRescueSettingsOpen) {
            RescueQuickCallContent(
                rescueTrapType = rescueTrapType,
                onRescueTrapTypeChange = { rescueTrapType = it },
                rescueEnemyCountType = rescueEnemyCountType,
                onRescueEnemyCountTypeChange = { rescueEnemyCountType = it },
                rescueSystem = rescueSystem,
                onRescueSystemChange = { rescueSystem = it },
                bigFishPilotName = bigFishPilotName,
                onBigFishPilotNameChange = { bigFishPilotName = it },
                cynoPilotName = cynoPilotName,
                onCynoPilotNameChange = { cynoPilotName = it },
                rescueLocationType = rescueLocationType,
                onRescueLocationTypeChange = { rescueLocationType = it },
                onCallClick = {
                    val trapType = rescueTrapType ?: return@RescueQuickCallContent
                    val enemyCount = rescueEnemyCountType ?: return@RescueQuickCallContent
                    val locationType = rescueLocationType ?: return@RescueQuickCallContent
                    messageInput = buildRescuePingMessage(
                        rorqualPilot = bigFishPilotName,
                        system = rescueSystem,
                        locationType = locationType,
                        enemyCountType = enemyCount,
                        trapType = trapType,
                        cynoPilot = cynoPilotName,
                    )
                    isRescueSettingsOpen = false
                },
                onBackClick = { isRescueSettingsOpen = false },
                modifier = Modifier.weight(1f),
            )
        } else {
            val listState = rememberLazyListState()
            var lastMessage by remember { mutableStateOf(messages.lastOrNull()) }
            LaunchedEffect(messages) {
                val newLastMessage = messages.lastOrNull()
                if (newLastMessage != lastMessage) {
                    if (messages.lastIndex >= 0) listState.animateScrollToItem(messages.lastIndex)
                    lastMessage = newLastMessage
                }
            }
            LaunchedEffect(Unit) {
                if (messages.lastIndex >= 0) listState.animateScrollToItem(messages.lastIndex)
            }
            ScrollbarLazyColumn(
                listState = listState,
                contentPadding = PaddingValues(vertical = Spacing.medium),
                scrollbarModifier = Modifier.padding(Spacing.small),
                modifier = Modifier.weight(1f),
            ) {
                items(messages) {
                    ChatMessage(isUsingBiggerFontSize, multiUserChat, it)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier
                    .padding(horizontal = Spacing.medium)
                    .padding(bottom = Spacing.medium),
            ) {
                RiftTextField(
                    text = messageInput,
                    onTextChanged = { messageInput = it },
                    singleLine = false,
                    minLines = 1,
                    maxLines = 6,
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent {
                            if (it.key == Key.Enter) {
                                onMessageSend(messageInput)
                                messageInput = ""
                                true
                            } else {
                                false
                            }
                        },
                )
                RiftButton(
                    text = "发送",
                    onClick = {
                        onMessageSend(messageInput)
                        messageInput = ""
                    },
                )
                RiftButton(
                    text = "一键呼叫救援",
                    type = ButtonType.Secondary,
                    onClick = {
                        rescueTrapType = null
                        rescueEnemyCountType = null
                        rescueLocationType = null
                        isRescueSettingsOpen = true
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatMessage(
    isUsingBiggerFontSize: Boolean,
    userChat: UserChat,
    message: UserMessage,
) {
    val from = if (message.isOutgoing) "你" else userChat.name
    ChatMessage(isUsingBiggerFontSize, message.timestamp, from, message.text)
}

@Composable
private fun ChatMessage(
    isUsingBiggerFontSize: Boolean,
    userChat: MultiUserChat,
    message: MultiUserMessage,
) {
    val from = message.sender ?: "你"
    ChatMessage(isUsingBiggerFontSize, message.timestamp, from, message.text)
}

@Composable
private fun ChatMessage(
    isUsingBiggerFontSize: Boolean,
    timestamp: Instant,
    sender: String,
    message: String,
) {
    val time: ZonedDateTime = ZonedDateTime.ofInstant(timestamp, ZoneId.systemDefault())
    val formatter = if (time.isBefore(ZonedDateTime.of(LocalDate.now().atTime(0, 0), ZoneId.systemDefault()))) {
        DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")
    } else {
        DateTimeFormatter.ofPattern("HH:mm:ss")
    }
    val formattedTime = formatter.format(time)
    val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
    val linkifiedMessage = remember(message) { annotateLinks(message, linkStyle) }
    val text = buildAnnotatedString {
        append("[$formattedTime] ")
        withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
            append(sender)
        }
        append(" > ")
        append(linkifiedMessage)
    }

    RiftContextMenuArea(
        items = listOf(
            TextItem("复制整行", onClick = { Clipboard.copy(text.toString()) }),
            TextItem("复制消息", onClick = { Clipboard.copy(message) }),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hoverBackground()
                .padding(horizontal = Spacing.medium, vertical = Spacing.verySmall),
        ) {
            val style = if (isUsingBiggerFontSize) RiftTheme.typography.headerPrimary else RiftTheme.typography.bodyPrimary
            Text(
                text = text,
                style = style,
            )
        }
    }
}
