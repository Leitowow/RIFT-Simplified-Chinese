package dev.nohus.rift.charactersettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
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
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWarningBanner
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.getRelativeTime
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.copy_16px
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.recall_drones_16px
import dev.nohus.rift.generated.resources.window_character_settings
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
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
        title = "Character Settings Copy",
        icon = Res.drawable.window_character_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        CharacterSettingsWindowContent(
            state = state,
            onCancelClick = viewModel::onCancelClick,
            onCopySourceClick = viewModel::onCopySourceClick,
            onCopySourceProfileClick = viewModel::onCopySourceProfileClick,
            onCopyTargetProfileClick = viewModel::onCopyTargetProfileClick,
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
    onCopySourceProfileClick: (String) -> Unit,
    onCopyTargetProfileClick: (String) -> Unit,
    onCopyDestinationClick: (Int) -> Unit,
    onCopySettingsConfirmClick: () -> Unit,
    onAssignAccount: (characterId: Int, accountId: Int) -> Unit,
) {
    if (state.characters.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            AnimatedVisibility(state.isOnline) {
                RiftWarningBanner(
                    text = "You are online. Settings can't be copied while the game is open. Make sure to close EVE first.",
                )
            }
            var selectedSourceProfile: String? by remember { mutableStateOf(null) }
            var selectedTargetProfile: String? by remember { mutableStateOf(null) }
            AnimatedContent(state.copying, contentKey = { it::class }) { copying ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (copying) {
                        is CopyingState.SelectingSource -> {
                            Text(
                                text = "Select the character to copy EVE settings from.",
                                style = RiftTheme.typography.bodyPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        is CopyingState.SelectingSourceLauncherProfile -> {
                            LaunchedEffect(copying) {
                                selectedSourceProfile = copying.profiles.first()
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        append("Copying EVE settings from ")
                                        withColor(RiftTheme.colors.textHighlighted) {
                                            append(copying.source.name)
                                        }
                                        append(".\n\nThis character has settings in multiple launcher profiles. From which profile would you like to copy?")
                                    },
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                                Row {
                                    RiftDropdownWithLabel(
                                        label = "Source profile:",
                                        items = copying.profiles,
                                        selectedItem = selectedSourceProfile,
                                        onItemSelected = { selectedSourceProfile = it },
                                        getItemName = { it ?: "" },
                                    )
                                }
                            }
                        }

                        is CopyingState.SelectingDestination -> {
                            Text(
                                text = "Select characters to paste EVE settings to.",
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
                                        append("Copying EVE settings from ")
                                        withColor(RiftTheme.colors.textHighlighted) {
                                            append(copying.source.name)
                                        }
                                        append(" to ")
                                        copying.destination.forEachIndexed { index, character ->
                                            if (index != 0) append(", ")
                                            withColor(RiftTheme.colors.textHighlighted) {
                                                append(character.name)
                                            }
                                        }
                                        append(".")

                                        if (unselectedAffectedCharacters.isNotEmpty()) {
                                            appendLine()
                                            appendLine()
                                            append("Some settings are account-wide, so these will also affect ")
                                            unselectedAffectedCharacters.map { it.info?.name ?: "${it.characterId}" }.forEachIndexed { index, character ->
                                                if (index != 0) append(", ")
                                                withColor(RiftTheme.colors.textHighlighted) {
                                                    append(character)
                                                }
                                            }
                                            append(".")
                                        }
                                    },
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                            }
                        }

                        is CopyingState.SelectingTargetLauncherProfile -> Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        ) {
                            LaunchedEffect(copying) {
                                selectedTargetProfile = copying.profiles.first()
                            }
                            Text(
                                text = buildAnnotatedString {
                                    append("Copying EVE settings from ")
                                    withColor(RiftTheme.colors.textHighlighted) {
                                        append(copying.source.name)
                                    }
                                    append(" to ")
                                    copying.destination.forEachIndexed { index, character ->
                                        if (index != 0) append(", ")
                                        withColor(RiftTheme.colors.textHighlighted) {
                                            append(character.name)
                                        }
                                    }
                                    append(".\n\nYou have multiple launcher profiles. To which profile would you like to paste settings?")
                                },
                                style = RiftTheme.typography.bodyPrimary,
                            )
                            Row {
                                RiftDropdownWithLabel(
                                    label = "Target profile:",
                                    items = copying.profiles,
                                    selectedItem = selectedTargetProfile,
                                    onItemSelected = { selectedTargetProfile = it },
                                    getItemName = { it ?: "" },
                                )
                            }
                        }
                    }
                }
            }

            var accountEditingCharacter by remember { mutableStateOf<Int?>(null) }
            val accounts = state.accounts
                .sortedByDescending { it.lastModified }
            ScrollbarLazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                (accounts + listOf(null)).forEachIndexed { index, account ->
                    val characters = state.characters.filter { it.accountId == account?.id }
                    item(key = account) {
                        if (account == null && characters.isEmpty()) return@item
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            modifier = Modifier
                                .modifyIf(index != 0) { padding(top = Spacing.medium) }
                                .background(RiftTheme.colors.windowBackgroundSecondary)
                                .padding(Spacing.medium)
                                .fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(bottom = Spacing.small),
                            ) {
                                val accountName = account?.let { "Account ${account.id}" } ?: "Unassigned characters"
                                val tooltip = account?.let { "Settings files: ${account.paths.values.joinToString()}" } ?: "RIFT doesn't know which account these characters belong to"
                                RiftTooltipArea(tooltip) {
                                    Text(
                                        text = accountName.uppercase(),
                                        style = RiftTheme.typography.headerPrimary,
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
                                    text = "No characters assigned",
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            }

                            if (characters.size > 3 && account != null) {
                                Text(
                                    text = "Warning: This account has more than 3 characters, which is not possible. Correct the assignment.",
                                    style = RiftTheme.typography.bodySecondary,
                                    modifier = Modifier.padding(bottom = Spacing.small),
                                )
                            }

                            if (account == null) {
                                val text = when (state.copying) {
                                    CopyingState.SelectingSource -> "Assign these characters to accounts to be able to copy settings from them. Log in to assign automatically."
                                    is CopyingState.SelectingSourceLauncherProfile -> null
                                    is CopyingState.SelectingDestination -> "Assign these characters to accounts to be able to copy settings to them. Log in to assign automatically."
                                    is CopyingState.DestinationSelected -> "Assign these characters to accounts to be able to copy settings to them. Log in to assign automatically."
                                    is CopyingState.SelectingTargetLauncherProfile -> null
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
                        }
                    }
                    characters.sortedBy { it.characterId }.forEachIndexed { index, character ->
                        item(key = character) {
                            Column(
                                modifier = Modifier
                                    .background(RiftTheme.colors.windowBackgroundSecondary)
                                    .padding(horizontal = Spacing.medium, vertical = Spacing.verySmall)
                                    .fillMaxWidth(),
                            ) {
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
            }

            AnimatedContent(state.copying, contentKey = { it::class }) { copying ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    when (copying) {
                        CopyingState.SelectingSource -> {
                            RiftButton(
                                text = "Cancel",
                                type = ButtonType.Secondary,
                                cornerCut = ButtonCornerCut.Both,
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        is CopyingState.SelectingSourceLauncherProfile -> {
                            RiftButton(
                                text = "Cancel",
                                type = ButtonType.Secondary,
                                cornerCut = ButtonCornerCut.BottomLeft,
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f),
                            )
                            RiftButton(
                                text = "Confirm profile",
                                cornerCut = ButtonCornerCut.BottomRight,
                                onClick = { selectedSourceProfile?.let(onCopySourceProfileClick) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        is CopyingState.SelectingDestination -> {
                            RiftButton(
                                text = "Cancel",
                                type = ButtonType.Secondary,
                                cornerCut = ButtonCornerCut.Both,
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        is CopyingState.DestinationSelected -> {
                            RiftButton(
                                text = "Cancel",
                                type = ButtonType.Secondary,
                                cornerCut = ButtonCornerCut.BottomLeft,
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f),
                            )
                            RiftButton(
                                text = "Confirm characters",
                                cornerCut = ButtonCornerCut.BottomRight,
                                onClick = onCopySettingsConfirmClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        is CopyingState.SelectingTargetLauncherProfile -> {
                            RiftButton(
                                text = "Cancel",
                                type = ButtonType.Secondary,
                                cornerCut = ButtonCornerCut.BottomLeft,
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f),
                            )
                            RiftButton(
                                text = "Confirm profile",
                                cornerCut = ButtonCornerCut.BottomRight,
                                onClick = { selectedTargetProfile?.let(onCopyTargetProfileClick) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    } else {
        Text(
            text = "No characters found.\n\nMake sure the game directory is selected in settings, and that you have logged in to at least one character on this computer before.",
            style = RiftTheme.typography.headerPrimary,
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.medium),
        )
    }
}

@Composable
private fun CharacterRow(
    character: CharacterItem,
    copying: CopyingState,
    account: Account?,
    accounts: List<Account>,
    isEditingAccount: Boolean,
    onUpdateAccountClick: () -> Unit,
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
            DynamicCharacterPortraitParallax(
                characterId = character.characterId,
                size = 48.dp,
                enterTimestamp = null,
                pointerInteractionStateHolder = null,
            )

            when (character.info) {
                null -> {
                    Text(
                        text = "Could not load",
                        style = RiftTheme.typography.bodySecondary.copy(color = RiftTheme.colors.borderError),
                        modifier = Modifier.padding(horizontal = Spacing.medium),
                    )
                }

                else -> {
                    Text(
                        text = character.info.name,
                        style = RiftTheme.typography.headerHighlighted,
                    )
                }
            }

            if (account != null && accounts.size > 1) {
                RiftTooltipArea("Update account") {
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
                        if (character.settingsFiles.isNotEmpty()) {
                            RiftButton(
                                text = "Copy",
                                icon = Res.drawable.copy_16px,
                                onClick = onCopyClick,
                            )
                        } else {
                            NoSettingsFileIcon()
                        }
                    }
                    is CopyingState.SelectingSourceLauncherProfile -> {
                        if (copying.source.id == character.characterId) {
                            CopyingIcon()
                        }
                    }
                    is CopyingState.SelectingDestination -> {
                        RiftButton(
                            text = "Paste",
                            icon = Res.drawable.recall_drones_16px,
                            onClick = onPasteClick,
                        )
                    }
                    is CopyingState.DestinationSelected -> {
                        if (copying.destination.any { it.id == character.characterId }) {
                            PastingIcon()
                        } else {
                            RiftButton(
                                text = "Paste",
                                icon = Res.drawable.recall_drones_16px,
                                onClick = onPasteClick,
                            )
                        }
                    }
                    is CopyingState.SelectingTargetLauncherProfile -> {
                        if (copying.source.id == character.characterId) {
                            CopyingIcon()
                        } else if (copying.destination.any { it.id == character.characterId }) {
                            PastingIcon()
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
                        text = "Set account:",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
                accounts.filter { it != account }.forEach { account ->
                    RiftButton(
                        text = account.id.toString(),
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
            text = "Copying",
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
            text = "Pasting",
            style = RiftTheme.typography.bodyPrimary,
        )
    }
}

@Composable
private fun NoSettingsFileIcon() {
    RequirementIcon(
        isFulfilled = false,
        fulfilledTooltip = "",
        notFulfilledTooltip = "EVE settings file for this character is missing.\nMake sure you have logged in to the game at least once.",
        modifier = Modifier.padding(start = Spacing.small),
    )
}
