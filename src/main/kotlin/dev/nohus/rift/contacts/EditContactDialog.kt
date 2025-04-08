package dev.nohus.rift.contacts

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.contacts.ContactsRepository.EntityType
import dev.nohus.rift.contacts.ContactsViewModel.DeleteContactRequest
import dev.nohus.rift.contacts.ContactsViewModel.EditContactDialog
import dev.nohus.rift.contacts.ContactsViewModel.UpdateContactRequest
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.flag_negative
import dev.nohus.rift.generated.resources.flag_neutral
import dev.nohus.rift.generated.resources.flag_positive
import dev.nohus.rift.generated.resources.window_editcontact
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.getFlagColor
import dev.nohus.rift.utils.toggle
import dev.nohus.rift.windowing.WindowManager
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun WindowScope.EditContactDialog(
    dialog: EditContactDialog,
    parentWindowState: WindowManager.RiftWindowState,
    onConfirmClick: (UpdateContactRequest) -> Unit,
    onDeleteClick: (DeleteContactRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var isExisting by remember { mutableStateOf(false) }
    RiftDialog(
        title = if (isExisting) "Edit Contact" else "Add Contact",
        icon = Res.drawable.window_editcontact,
        parentState = parentWindowState,
        state = rememberWindowState(width = 300.dp, height = Dp.Unspecified),
        onCloseClick = onDismiss,
    ) {
        key(dialog) {
            EditContactDialogContent(
                dialog = dialog,
                isExisting = isExisting,
                onIsExistingChange = { isExisting = it },
                onConfirmClick = onConfirmClick,
                onDeleteClick = onDeleteClick,
                onCancelClick = onDismiss,
            )
        }
    }
}

@Composable
private fun EditContactDialogContent(
    dialog: EditContactDialog,
    isExisting: Boolean,
    onIsExistingChange: (Boolean) -> Unit,
    onConfirmClick: (UpdateContactRequest) -> Unit,
    onDeleteClick: (DeleteContactRequest) -> Unit,
    onCancelClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        val defaultCharacter = dialog.characters.firstOrNull {
            it.characterId in dialog.ownerCharacters.map { it.id }
        } ?: dialog.characters.first()
        val defaultStanding = dialog.ownerStandings[defaultCharacter.characterId] ?: Standing.Neutral

        var selectedStanding by remember { mutableStateOf(defaultStanding) }
        var selectedCharacter: LocalCharacter by remember { mutableStateOf(defaultCharacter) }
        var selectedLabels: List<String> by remember { mutableStateOf(emptyList()) }
        var selectedWatched: Boolean? by remember { mutableStateOf(null) }

        LaunchedEffect(selectedCharacter) {
            onIsExistingChange(selectedCharacter.characterId in dialog.ownerCharacters.map { it.id })
            selectedStanding = dialog.ownerStandings[selectedCharacter.characterId] ?: Standing.Neutral
            selectedLabels = dialog.ownerLabels[selectedCharacter.characterId] ?: emptyList()
            selectedWatched = if (dialog.entity.type == EntityType.Character) {
                dialog.ownerWatched[selectedCharacter.characterId] ?: false
            } else {
                null
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            EntityImage(
                entity = dialog.entity,
                size = 64,
            )
            Column {
                Text(
                    text = dialog.entity.name,
                    style = RiftTheme.typography.titlePrimary.copy(fontWeight = FontWeight.Bold),
                )

                Text(
                    text = selectedStanding.getName(),
                    style = RiftTheme.typography.titlePrimary,
                    modifier = Modifier.padding(top = Spacing.medium),
                )
                StandingLevelSelector(
                    selectedStanding = selectedStanding,
                    onStandingSelected = { selectedStanding = it },
                    modifier = Modifier.padding(top = Spacing.small),
                )

                val selectedWatchedCopy = selectedWatched
                if (selectedWatchedCopy != null) {
                    RiftCheckboxWithLabel(
                        label = "Add contact to buddy list",
                        isChecked = selectedWatchedCopy,
                        onCheckedChange = { selectedWatched = it },
                        modifier = Modifier.padding(top = Spacing.mediumLarge),
                    )
                }

                Text(
                    text = "Choose character",
                    style = RiftTheme.typography.titlePrimary,
                    modifier = Modifier.padding(top = Spacing.mediumLarge),
                )
                RiftDropdown(
                    items = dialog.characters,
                    selectedItem = selectedCharacter,
                    onItemSelected = { selectedCharacter = it },
                    getItemName = { it.info.success?.name ?: "${it.characterId}" },
                    modifier = Modifier.padding(top = Spacing.small),
                )

                val labels = dialog.labels[selectedCharacter.characterId] ?: emptyList()
                AnimatedContent(
                    targetState = labels,
                    modifier = Modifier.padding(top = Spacing.mediumLarge),
                ) { labels ->
                    Column {
                        RiftTooltipArea("Labels can only be created in-game") {
                            Text(
                                text = if (labels.isNotEmpty()) "Assign labels" else "No labels available",
                                style = RiftTheme.typography.titlePrimary,
                            )
                        }
                        ScrollbarColumn(
                            contentPadding = PaddingValues(vertical = Spacing.small),
                            isScrollbarConditional = true,
                            modifier = Modifier.heightIn(max = 100.dp),
                        ) {
                            for (label in labels) {
                                RiftCheckboxWithLabel(
                                    label = label,
                                    isChecked = label in selectedLabels,
                                    onCheckedChange = { selectedLabels = selectedLabels.toggle(label) },
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            if (isExisting) {
                RiftButton(
                    text = "Delete",
                    cornerCut = ButtonCornerCut.None,
                    type = ButtonType.Negative,
                    onClick = {
                        onDeleteClick(
                            DeleteContactRequest(
                                characterId = selectedCharacter.characterId,
                                entity = dialog.entity,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            RiftButton(
                text = if (isExisting) "Edit" else "Add",
                cornerCut = ButtonCornerCut.None,
                onClick = {
                    onConfirmClick(
                        UpdateContactRequest(
                            characterId = selectedCharacter.characterId,
                            entity = dialog.entity,
                            standing = selectedStanding,
                            labels = selectedLabels,
                            isWatched = selectedWatched,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
            )
            RiftButton(
                text = "Cancel",
                cornerCut = ButtonCornerCut.BottomRight,
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StandingLevelSelector(
    selectedStanding: Standing,
    onStandingSelected: (Standing) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Standing.entries.forEach { standing ->
            val color = standing.getFlagColor()
            val icon = when (standing) {
                Standing.Terrible, Standing.Bad -> Res.drawable.flag_negative
                Standing.Neutral -> Res.drawable.flag_neutral
                Standing.Good, Standing.Excellent -> Res.drawable.flag_positive
            }
            StandingBox(
                color = color,
                icon = icon,
                tooltip = standing.getName(),
                isSelected = selectedStanding == standing,
                onClick = { onStandingSelected(standing) },
            )
        }
    }
}

private fun Standing.getName(): String {
    return when (this) {
        Standing.Terrible -> "Terrible Standing"
        Standing.Bad -> "Bad Standing"
        Standing.Neutral -> "Neutral Standing"
        Standing.Good -> "Good Standing"
        Standing.Excellent -> "Excellent Standing"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StandingBox(
    color: Color,
    icon: DrawableResource,
    tooltip: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val pointerInteractionState = rememberPointerInteractionStateHolder()
    val isHighlighted = isSelected || pointerInteractionState.isHovered
    RiftTooltipArea(tooltip) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .onClick { onClick() }
                .pointerInteraction(pointerInteractionState)
                .border(1.dp, Color.White.copy(alpha = if (isHighlighted) 0.8f else 0.2f))
                .border(1.dp, color.copy(alpha = 0.75f))
                .background(color.copy(alpha = 0.75f))
                .size(20.dp),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(8.dp),
            )
        }
    }
}
