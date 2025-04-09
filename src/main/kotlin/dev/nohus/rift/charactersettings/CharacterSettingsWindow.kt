package dev.nohus.rift.charactersettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import dev.nohus.rift.charactersettings.CharacterSettingsViewModel.CharacterItem
import dev.nohus.rift.charactersettings.CharacterSettingsViewModel.CopyingState
import dev.nohus.rift.charactersettings.CharacterSettingsViewModel.UiState
import dev.nohus.rift.charactersettings.GetAccountsUseCase.Account
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.getRelativeTime
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.copy_16px
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.recall_drones_16px
import dev.nohus.rift.generated.resources.window_character_settings
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource
import java.time.ZoneId

@Composable
fun CharacterSettingsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: CharacterSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "复制角色设置",
        icon = Res.drawable.window_character_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        CharacterSettingsWindowContent(
            state = state,
            onCancelClick = viewModel::onCancelClick,
            onCopySourceClick = viewModel::onCopySourceClick,
            onCopyDestinationClick = viewModel::onCopyDestinationClick,
            onCopySettingsConfirmClick = viewModel::onCopySettingsConfirmClick,
            onAssignAccount = viewModel::onAssignAccount,
        )

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }
    }
}

@Composable
private fun CharacterSettingsWindowContent(
    state: UiState,
    onCancelClick: () -> Unit,
    onCopySourceClick: (Int) -> Unit,
    onCopyDestinationClick: (Int) -> Unit,
    onCopySettingsConfirmClick: () -> Unit,
    onAssignAccount: (characterId: Int, accountId: Int) -> Unit,
) {
    if (state.characters.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            AnimatedContent(state.copying, contentKey = { it::class }) { copying ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (copying) {
                        is CopyingState.SelectingSource -> {
                            Text(
                                text = "选择要从中复制EVE设置的角色。",
                                style = RiftTheme.typography.bodyPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        is CopyingState.SelectingDestination -> {
                            Text(
                                text = "选择要粘贴EVE设置的角色。",
                                style = RiftTheme.typography.bodyPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        is CopyingState.DestinationSelected -> {
                            AnimatedContent(copying, modifier = Modifier.fillMaxWidth()) { copying ->
                                val destinationIds = copying.destination.map { it.id }
                                val sourceAccountId = state.characters.firstOrNull { it.characterId == copying.source.id }?.accountId
                                val charactersOnSourceAccount = state.characters.filter { it.accountId == sourceAccountId }.map { it.characterId }
                                val charactersOnTargetAccounts = destinationIds.flatMap { destinationId ->
                                    val accountId = state.characters.firstOrNull { it.characterId == destinationId }?.accountId
                                    state.characters.filter { it.accountId == accountId }
                                }.distinct()
                                val unselectedAffectedCharacters = charactersOnTargetAccounts
                                    .filter { it.characterId !in destinationIds }
                                    .filter { it.characterId !in charactersOnSourceAccount }

                                Text(
                                    text = buildAnnotatedString {
                                        append("正在从 ")
                                        withColor(RiftTheme.colors.textHighlighted) {
                                            append(copying.source.name)
                                        }
                                        append(" 复制EVE设置到 ")
                                        copying.destination.forEachIndexed { index, character ->
                                            if (index != 0) append(", ")
                                            withColor(RiftTheme.colors.textHighlighted) {
                                                append(character.name)
                                            }
                                        }
                                        append("。")

                                        if (unselectedAffectedCharacters.isNotEmpty()) {
                                            appendLine()
                                            appendLine()
                                            append("某些设置是账号范围的，因此这些设置也会影响 ")
                                            unselectedAffectedCharacters.map { it.info.success?.name ?: "${it.characterId}" }.forEachIndexed { index, character ->
                                                if (index != 0) append(", ")
                                                withColor(RiftTheme.colors.textHighlighted) {
                                                    append(character)
                                                }
                                            }
                                            append("。")
                                        }
                                    },
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                            }
                        }
                    }
                }
            }

            ScrollbarColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.weight(1f),
            ) {
                var accountEditingCharacter by remember { mutableStateOf<Int?>(null) }
                val accounts = state.accounts
                    .sortedByDescending { it.lastModified }
                val accountOrdinals = accounts
                    .mapIndexed { index, account -> account to index + 1 }
                    .toMap()

                (accounts + listOf(null)).forEach { account ->
                    val characters = state.characters.filter { it.accountId == account?.id }
                    if (account == null && characters.isEmpty()) return@forEach
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier
                            .padding(end = Spacing.small)
                            .border(1.dp, RiftTheme.colors.borderGreyLight)
                            .padding(Spacing.medium)
                            .fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(bottom = Spacing.small),
                        ) {
                            val accountName = account?.let { "账号 ${accountOrdinals[it]}" } ?: "未分配的角色"
                            val tooltip = account?.let { "设置文件: ${account.path}" } ?: "RIFT不知道这些角色属于哪个账号"
                            RiftTooltipArea(tooltip) {
                                Text(
                                    text = accountName.uppercase(),
                                    style = RiftTheme.typography.titlePrimary,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            val lastUsed = account?.lastModified
                            if (lastUsed != null) {
                                val now = getNow()
                                val age = key(now) { getRelativeTime(lastUsed, ZoneId.systemDefault(), now) }
                                Text(
                                    text = "Last used: $age",
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            }
                        }

                        if (characters.isEmpty()) {
                            Text(
                                text = "未分配角色",
                                style = RiftTheme.typography.bodySecondary,
                            )
                        }

                        if (characters.size > 3) {
                            Text(
                                text = "警告：此账号有超过3个角色，这是不可能的。请更正分配。",
                                style = RiftTheme.typography.bodySecondary,
                                modifier = Modifier.padding(bottom = Spacing.small),
                            )
                        }

                        if (account == null) {
                            val text = when (state.copying) {
                                CopyingState.SelectingSource -> "分配这些角色到账号以便能够从中复制设置。登录以自动分配。"
                                is CopyingState.SelectingDestination -> "分配这些角色到账号以便能够复制设置到它们。登录以自动分配。"
                                is CopyingState.DestinationSelected -> null
                            }
                            AnimatedContent(text) {
                                if (it != null) {
                                    Text(
                                        text = it,
                                        style = RiftTheme.typography.bodySecondary,
                                        modifier = Modifier.padding(bottom = Spacing.small),
                                    )
                                }
                            }
                        }

                        characters.sortedBy { it.characterId }.forEach { character ->
                            val isEditingAccount = accountEditingCharacter == character.characterId
                            CharacterRow(
                                character = character,
                                copying = state.copying,
                                account = account,
                                accounts = accounts,
                                isEditingAccount = accountEditingCharacter == character.characterId,
                                onUpdateAccountClick = {
                                    accountEditingCharacter = if (!isEditingAccount) character.characterId else null
                                },
                                accountOrdinals = accountOrdinals,
                                onAssignAccount = { characterId, accountId ->
                                    onAssignAccount(characterId, accountId)
                                    accountEditingCharacter = null
                                },
                                onCopyClick = { onCopySourceClick(character.characterId) },
                                onPasteClick = { onCopyDestinationClick(character.characterId) },
                            )
                        }
                    }
                }
            }

            AnimatedContent(state.copying, contentKey = { it::class }) { copying ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    when (copying) {
                        CopyingState.SelectingSource -> {
                            RiftButton(
                                text = "取消",
                                type = ButtonType.Secondary,
                                cornerCut = ButtonCornerCut.Both,
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        is CopyingState.SelectingDestination -> {
                            RiftButton(
                                text = "取消",
                                type = ButtonType.Secondary,
                                cornerCut = ButtonCornerCut.Both,
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        is CopyingState.DestinationSelected -> {
                            RiftButton(
                                text = "取消",
                                type = ButtonType.Secondary,
                                cornerCut = ButtonCornerCut.BottomLeft,
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f),
                            )
                            RiftButton(
                                text = "确认",
                                cornerCut = ButtonCornerCut.BottomRight,
                                onClick = onCopySettingsConfirmClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    } else {
        Text(
            text = "未找到角色。\n\n请确保在设置中选择了游戏目录，并且您之前在此计算机上至少登录过一个角色。",
            style = RiftTheme.typography.titlePrimary,
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.medium),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterRow(
    character: CharacterItem,
    copying: CopyingState,
    account: Account?,
    accounts: List<Account>,
    isEditingAccount: Boolean,
    onUpdateAccountClick: () -> Unit,
    accountOrdinals: Map<Account, Int>,
    onAssignAccount: (characterId: Int, accountId: Int) -> Unit,
    onCopyClick: () -> Unit,
    onPasteClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncPlayerPortrait(
                characterId = character.characterId,
                size = 32,
                modifier = Modifier.size(32.dp),
            )

            when (character.info) {
                is AsyncResource.Ready -> {
                    Text(
                        text = character.info.value.name,
                        style = RiftTheme.typography.titleHighlighted,
                    )
                }

                is AsyncResource.Error -> {
                    Text(
                        text = "无法加载",
                        style = RiftTheme.typography.bodySecondary.copy(color = RiftTheme.colors.borderError),
                        modifier = Modifier.padding(horizontal = Spacing.medium),
                    )
                }

                AsyncResource.Loading -> {
                    Text(
                        text = "加载中…",
                        style = RiftTheme.typography.bodySecondary,
                        modifier = Modifier.padding(horizontal = Spacing.medium),
                    )
                }
            }

            if (account != null && accounts.size > 1) {
                RiftTooltipArea("更新账号") {
                    RiftImageButton(
                        resource = Res.drawable.editplanicon,
                        size = 20.dp,
                        onClick = onUpdateAccountClick,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (account != null) {
                when (copying) {
                    CopyingState.SelectingSource -> {
                        if (character.settingsFile != null) {
                            RiftButton(
                                text = "复制",
                                icon = Res.drawable.copy_16px,
                                onClick = onCopyClick,
                            )
                        } else {
                            NoSettingsFileIcon()
                        }
                    }
                    is CopyingState.SelectingDestination -> {
                        if (character.settingsFile != null) {
                            if (copying.sourceId == character.characterId) {
                                CopyingIcon()
                            } else {
                                RiftButton(
                                    text = "粘贴",
                                    icon = Res.drawable.recall_drones_16px,
                                    onClick = onPasteClick,
                                )
                            }
                        } else {
                            NoSettingsFileIcon()
                        }
                    }
                    is CopyingState.DestinationSelected -> {
                        if (character.settingsFile != null) {
                            if (copying.source.id == character.characterId) {
                                CopyingIcon()
                            } else if (copying.destination.any { it.id == character.characterId }) {
                                PastingIcon()
                            } else {
                                RiftButton(
                                    text = "粘贴",
                                    icon = Res.drawable.recall_drones_16px,
                                    onClick = onPasteClick,
                                )
                            }
                        } else {
                            NoSettingsFileIcon()
                        }
                    }
                }
            }
        }

        AnimatedVisibility(isEditingAccount || account == null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(24.dp),
                ) {
                    Text(
                        text = "设置账号:",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
                accountOrdinals.filter { it.key != account }.forEach { (account, accountName) ->
                    RiftButton(
                        text = accountName.toString(),
                        isCompact = true,
                        onClick = {
                            onAssignAccount(character.characterId, account.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyingIcon() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.copy_16px),
            contentDescription = null,
            colorFilter = ColorFilter.tint(RiftTheme.colors.textPrimary),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "复制中",
            style = RiftTheme.typography.bodyPrimary,
        )
    }
}

@Composable
private fun PastingIcon() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.recall_drones_16px),
            contentDescription = null,
            colorFilter = ColorFilter.tint(RiftTheme.colors.textPrimary),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "粘贴中",
            style = RiftTheme.typography.bodyPrimary,
        )
    }
}

@Composable
private fun NoSettingsFileIcon() {
    RequirementIcon(
        isFulfilled = false,
        fulfilledTooltip = "",
        notFulfilledTooltip = "此角色的EVE设置文件缺失。\n请确保您至少登录过一次游戏。",
        modifier = Modifier.padding(start = Spacing.small),
    )
}
