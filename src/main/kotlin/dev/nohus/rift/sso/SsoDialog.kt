package dev.nohus.rift.sso

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.Bullet
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.completedicon
import dev.nohus.rift.generated.resources.doneglow
import dev.nohus.rift.generated.resources.purchase_fail_fg
import dev.nohus.rift.generated.resources.window_browser
import dev.nohus.rift.sso.SsoViewModel.UiState
import dev.nohus.rift.sso.scopes.ScopeGroup
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource

@Composable
fun WindowScope.SsoDialog(
    inputModel: SsoAuthority,
    parentWindowState: RiftWindowState,
    onDismiss: () -> Unit,
) {
    val viewModel: SsoViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onWindowOpened()
    }

    val title = when (inputModel) {
        SsoAuthority.Eve -> "Log in with EVE Online"
    }
    RiftDialog(
        title = title,
        icon = Res.drawable.window_browser,
        parentState = parentWindowState,
        state = rememberWindowState(width = 320.dp, height = Dp.Unspecified),
        onCloseClick = {
            viewModel.onCloseRequest()
            onDismiss()
        },
    ) {
        SsoDialogContent(
            state = state,
            onCheckedChange = viewModel::onCheckedChange,
            onScopesButtonClick = viewModel::onScopesButtonClick,
            onContinueClick = viewModel::onContinueClick,
            onFinishClick = {
                viewModel.onCloseRequest()
                onDismiss()
            },
        )
    }
}

@Composable
private fun SsoDialogContent(
    state: UiState,
    onCheckedChange: (ScopeGroup) -> Unit,
    onScopesButtonClick: () -> Unit,
    onContinueClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    AnimatedContent(state.status) { status ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            when (status) {
                SsoViewModel.SsoStatus.Scopes -> {
                    EsiScopes(
                        groups = state.scopeGroups,
                        selectedGroups = state.selectedScopeGroups,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.weight(1f),
                    )
                    RiftButton(
                        text = "Continue",
                        cornerCut = ButtonCornerCut.Both,
                        onClick = onContinueClick,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }

                SsoViewModel.SsoStatus.Waiting -> {
                    Spacer(Modifier.height(Spacing.large))
                    LoadingSpinner()
                    Text(
                        text = "Please continue in your browser",
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier
                            .padding(vertical = Spacing.large),
                    )
                    RiftButton(
                        text = "Change ESI scopes",
                        type = ButtonType.Secondary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = onScopesButtonClick,
                        modifier = Modifier
                            .padding(bottom = Spacing.medium)
                            .fillMaxWidth(),
                    )
                    RiftButton(
                        text = "Cancel",
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.Both,
                        onClick = onFinishClick,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }

                SsoViewModel.SsoStatus.Complete -> {
                    Spacer(Modifier.height(Spacing.large))
                    SuccessIcon()
                    Text(
                        text = "Authentication successful!",
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier
                            .padding(vertical = Spacing.large),
                    )
                    RiftButton(
                        text = "OK",
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.Both,
                        onClick = onFinishClick,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }

                is SsoViewModel.SsoStatus.Failed -> {
                    Spacer(Modifier.height(Spacing.large))
                    FailIcon()
                    Text(
                        text = "Authentication failed",
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier
                            .padding(vertical = Spacing.large),
                    )
                    if (status.message != null) {
                        Text(
                            text = status.message,
                            style = RiftTheme.typography.bodySecondary,
                            modifier = Modifier
                                .padding(vertical = Spacing.large),
                        )
                    }
                    RiftButton(
                        text = "Cancel",
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.Both,
                        onClick = onFinishClick,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EsiScopes(
    groups: List<ScopeGroup>,
    selectedGroups: List<ScopeGroup>,
    onCheckedChange: (ScopeGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(bottom = Spacing.large)
            .fillMaxWidth(),
    ) {
        Text(
            text = "Allow RIFT to:",
            style = RiftTheme.typography.headerPrimary,
            modifier = Modifier.padding(bottom = Spacing.large),
        )
        ScrollbarColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            for (group in groups) {
                EsiScope(
                    group = group,
                    isChecked = group in selectedGroups,
                    onCheckedChange = { onCheckedChange(group) },
                )
            }
        }
    }
}

@Composable
private fun EsiScope(
    group: ScopeGroup,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column {
        var isRequiredShown by remember { mutableStateOf(false) }
        val ids = group.scopes.joinToString("\n") { it.id }
        RiftCheckboxWithLabel(
            label = group.name,
            labelStyle = RiftTheme.typography.headerPrimary,
            tooltip = ids,
            isChecked = isChecked,
            onCheckedChange = {
                if (group.isRequired) {
                    isRequiredShown = true
                } else {
                    onCheckedChange(it)
                }
            },
        )
        Spacer(Modifier.height(Spacing.medium))
        if (isRequiredShown) {
            Row {
                Bullet(color = RiftTheme.colors.textRed)
                Spacer(Modifier.width(Spacing.medium))
                Text(
                    text = "Required for core functionality",
                    style = RiftTheme.typography.bodySecondary.copy(RiftTheme.colors.textRed),
                )
            }
        }
        group.reasons.forEach { reason ->
            Row {
                Bullet(color = RiftTheme.colors.textSecondary)
                Spacer(Modifier.width(Spacing.medium))
                Text(
                    text = reason,
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }
    }
}

@Composable
private fun FailIcon() {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.doneglow),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Image(
            painter = painterResource(Res.drawable.purchase_fail_fg),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )
    }
}

@Composable
private fun SuccessIcon() {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.doneglow),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Image(
            painter = painterResource(Res.drawable.completedicon),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
    }
}
