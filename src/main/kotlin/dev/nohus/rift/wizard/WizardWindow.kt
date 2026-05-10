package dev.nohus.rift.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AnimatedImage
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.TypingText
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.configurationpack.displayName
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.partner_400
import dev.nohus.rift.generated.resources.tray_tray_64
import dev.nohus.rift.generated.resources.window_agent
import dev.nohus.rift.get
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import dev.nohus.rift.wizard.WizardViewModel.EveInstallationState
import dev.nohus.rift.wizard.WizardViewModel.UiState
import dev.nohus.rift.wizard.WizardViewModel.WizardStep
import org.jetbrains.compose.resources.painterResource

@Composable
fun WizardWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: WizardViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "设置向导",
        icon = Res.drawable.window_agent,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        WizardWindowContent(
            state = state,
            onSetEveInstallationClick = viewModel::onSetEveInstallationClick,
            onCharactersClick = viewModel::onCharactersClick,
            onConfigurationPackChange = viewModel::onConfigurationPackChange,
            onSetIntelChannelsClick = viewModel::onSetIntelChannelsClick,
            onContinueClick = viewModel::onContinueClick,
            onKeyEvent = viewModel::onKeyEvent,
        )

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }

        if (state.onFinishedEvent.get()) onCloseRequest()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WizardWindowContent(
    state: UiState,
    onSetEveInstallationClick: () -> Unit,
    onCharactersClick: () -> Unit,
    onConfigurationPackChange: (ConfigurationPack?) -> Unit,
    onSetIntelChannelsClick: () -> Unit,
    onContinueClick: () -> Unit,
    onKeyEvent: (KeyEvent) -> Unit,
) {
    val focusRequester = FocusRequester()
    Row(
        modifier = Modifier
            .onKeyEvent {
                onKeyEvent(it)
                false
            }
            .focusRequester(focusRequester)
            .focusable()
            .onClick { focusRequester.requestFocus() },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(end = Spacing.large),
        ) {
            AnimatedImage(
                resource = "aura.gif",
                modifier = Modifier
                    .size(200.dp)
                    .border(2.dp, RiftTheme.colors.borderPrimary),
            )
            Image(
                painter = painterResource(Res.drawable.partner_400),
                contentDescription = null,
                modifier = Modifier
                    .width(200.dp),
            )
        }
        when (val step = state.step) {
            WizardStep.Welcome -> WelcomeStep(
                onContinueClick = onContinueClick,
            )
            is WizardStep.EveInstallation -> EveInstallationStep(
                step = step,
                onSetEveInstallationClick = onSetEveInstallationClick,
                onContinueClick = onContinueClick,
            )
            is WizardStep.Characters -> CharactersStep(
                step = step,
                onCharactersClick = onCharactersClick,
                onContinueClick = onContinueClick,
            )
            is WizardStep.ConfigurationPacks -> ConfigurationPacksStep(
                step = step,
                onConfigurationPackChange = onConfigurationPackChange,
                onContinueClick = onContinueClick,
            )
            is WizardStep.IntelChannels -> IntelChannelsStep(
                step = step,
                onSetIntelChannelsClick = onSetIntelChannelsClick,
                onContinueClick = onContinueClick,
            )
            WizardStep.Finish -> FinishStep(
                onContinueClick = onContinueClick,
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                append("欢迎，克隆飞行员！")
            }
            append(
                "\n\n你已安装 RIFT——一套用于增强态势感知的原型军用系统，可整合额外预警信息。\n\n" +
                    "下面由我引导你完成初始化。",
            )
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
    }
}

@Composable
private fun EveInstallationStep(
    step: WizardStep.EveInstallation,
    onSetEveInstallationClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    val isContinueWarning = when (step.state) {
        EveInstallationState.None -> true
        EveInstallationState.Detected -> false
        EveInstallationState.Set -> false
    }
    var hasFinishedTyping by remember { mutableStateOf(false) }

    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
        isWarning = isContinueWarning,
    ) {
        val text = buildAnnotatedString {
            when (step.state) {
                EveInstallationState.None -> {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("未检测到《星战前夜》客户端")
                    }
                    append(
                        "\n\n要正确配置 RIFT，需要知道本机 EVE 安装路径。",
                    )
                }
                EveInstallationState.Detected -> {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("已检测到《星战前夜》")
                    }
                    append(
                        "\n\n已找到你的 EVE 安装目录。",
                    )
                }
                EveInstallationState.Set -> {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("已设置《星战前夜》路径")
                    }
                    append(
                        "\n\nEVE 安装路径已正确保存。",
                    )
                }
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            val (type, buttonText) = when (step.state) {
                EveInstallationState.None -> ButtonType.Primary to "选择安装目录"
                EveInstallationState.Detected -> ButtonType.Secondary to "检查安装"
                EveInstallationState.Set -> ButtonType.Secondary to "更改安装路径"
            }
            RiftButton(
                text = buttonText,
                type = type,
                cornerCut = ButtonCornerCut.Both,
                onClick = onSetEveInstallationClick,
            )
        }
    }
}

@Composable
private fun CharactersStep(
    step: WizardStep.Characters,
    onCharactersClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
        isWarning = step.authenticatedCharacterCount < step.characterCount,
    ) {
        val text = buildAnnotatedString {
            if (step.characterCount == 0) {
                withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                    append("未检测到角色")
                }
                append(
                    "\n\n未在本机检测到曾登录过的角色记录。",
                )
            } else {
                if (step.authenticatedCharacterCount == 0) {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("已检测到角色")
                    }
                    append(
                        "\n\n在本机检测到 ${step.characterCount} 个角色。\n\n" +
                            "请通过 ESI 完成授权以便在 RIFT 中使用。",
                    )
                } else if (step.authenticatedCharacterCount < step.characterCount) {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("角色授权未完成")
                    }
                    append(
                        "\n\n共检测到 ${step.characterCount} 个角色，但仅有 ${step.authenticatedCharacterCount} 个已完成授权。",
                    )
                } else {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append("角色已就绪")
                    }
                    append(
                        "\n\n所有角色均已完成授权，可在 RIFT 中使用。",
                    )
                }
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            val (type, buttonText) = if (step.authenticatedCharacterCount < step.characterCount) {
                ButtonType.Primary to "授权角色"
            } else {
                ButtonType.Secondary to "检查角色"
            }
            RiftButton(
                text = buttonText,
                type = type,
                cornerCut = ButtonCornerCut.Both,
                onClick = onCharactersClick,
            )
        }
    }
}

@Composable
private fun ConfigurationPacksStep(
    step: WizardStep.ConfigurationPacks,
    onConfigurationPackChange: (ConfigurationPack?) -> Unit,
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    val getPackName: (ConfigurationPack?) -> String = { it.displayName }
    StepContent(
        onContinueClick = onContinueClick,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                append("联盟向功能")
            }
            if (step.pack != null) {
                append(
                    "\n\n是否启用针对 ${getPackName(step.pack)} 的定制功能？",
                )
            } else {
                append(
                    "\n\n你已选择默认配置。",
                )
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            RiftDropdownWithLabel(
                label = "配置方案：",
                items = listOf(null) + ConfigurationPack.entries,
                selectedItem = step.pack,
                onItemSelected = onConfigurationPackChange,
                getItemName = getPackName,
            )
        }
    }
}

@Composable
private fun IntelChannelsStep(
    step: WizardStep.IntelChannels,
    onSetIntelChannelsClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
        isWarning = !step.hasChannels,
        warningButtonText = "跳过",
    ) {
        val text = buildAnnotatedString {
            if (step.hasChannels) {
                withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                    append("预警频道已配置")
                }
                append(
                    "\n\n你已设置预警频道。",
                )
            } else {
                withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                    append("预警频道")
                }
                append(
                    "\n\n若联盟提供预警频道，可让 RIFT 监听其中的预警报告。",
                )
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            val (type, buttonText) = if (!step.hasChannels) {
                ButtonType.Primary to "添加预警频道"
            } else {
                ButtonType.Secondary to "更改预警频道"
            }
            RiftButton(
                text = buttonText,
                type = type,
                cornerCut = ButtonCornerCut.Both,
                onClick = onSetIntelChannelsClick,
            )
        }
    }
}

@Composable
private fun FinishStep(
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                append("全部就绪！")
            }
            append(
                "\n\nRIFT 已就绪。点击托盘图标即可使用全部功能：",
            )
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large),
        ) {
            Image(
                painter = painterResource(Res.drawable.tray_tray_64),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun StepContent(
    onContinueClick: () -> Unit,
    isContinueVisible: Boolean = true,
    isWarning: Boolean = false,
    warningButtonText: String = "仍要继续",
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        content()
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = isContinueVisible,
            enter = fadeIn(),
            modifier = Modifier.align(Alignment.End),
        ) {
            val text = if (isWarning) warningButtonText else "继续"
            val type = if (isWarning) ButtonType.Negative else ButtonType.Primary
            RiftButton(
                text = text,
                type = type,
                onClick = onContinueClick,
            )
        }
    }
}
