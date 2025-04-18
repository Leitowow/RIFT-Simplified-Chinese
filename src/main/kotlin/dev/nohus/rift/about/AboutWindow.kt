package dev.nohus.rift.about

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.about.AboutViewModel.UiState
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.CreatorCode
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.Patrons
import dev.nohus.rift.compose.RiftAppName
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.partner_400
import dev.nohus.rift.generated.resources.window_achievements
import dev.nohus.rift.generated.resources.window_concord
import dev.nohus.rift.generated.resources.window_rift_64
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.OperatingSystem.Linux
import dev.nohus.rift.utils.OperatingSystem.MacOs
import dev.nohus.rift.utils.OperatingSystem.Windows
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.windowing.WindowManager
import org.jetbrains.compose.resources.painterResource

@Composable
fun AboutWindow(
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: AboutViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "About RIFT",
        icon = Res.drawable.window_rift_64,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        AboutWindowContent(
            state = state,
            onUpdateClick = viewModel::onUpdateClick,
            onDebugClick = viewModel::onDebugClick,
            onAppDataClick = viewModel::onAppDataClick,
            onLegalClick = viewModel::onLegalClick,
            onCreditsClick = viewModel::onCreditsClick,
            onWhatsNewClick = viewModel::onWhatsNewClick,
        )

        if (state.isLegalDialogShown) {
            RiftDialog(
                title = "Legal & info",
                icon = Res.drawable.window_concord,
                parentState = windowState,
                state = rememberWindowState(width = 400.dp, height = 250.dp),
                onCloseClick = viewModel::onDialogDismissed,
            ) {
                ScrollbarColumn {
                    Text(
                        text = getLegalText(),
                        style = RiftTheme.typography.titlePrimary,
                    )
                }
            }
        }

        if (state.isCreditsDialogShown) {
            RiftDialog(
                title = "Credits",
                icon = Res.drawable.window_achievements,
                parentState = windowState,
                state = rememberWindowState(width = 400.dp, height = Dp.Unspecified),
                onCloseClick = viewModel::onDialogDismissed,
            ) {
                Text(
                    text = getCreditsText(),
                    style = RiftTheme.typography.titlePrimary,
                )
            }
        }
    }
}

@Composable
private fun AboutWindowContent(
    state: UiState,
    onUpdateClick: () -> Unit,
    onDebugClick: () -> Unit,
    onAppDataClick: () -> Unit,
    onLegalClick: () -> Unit,
    onCreditsClick: () -> Unit,
    onWhatsNewClick: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.width(200.dp).padding(bottom = Spacing.large),
            ) {
                Image(
                    painter = painterResource(Res.drawable.partner_400),
                    contentDescription = null,
                    modifier = Modifier
                        .width(200.dp),
                )
                CreatorCode()
                Patrons(state.patrons)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = Spacing.large).weight(1f),
            ) {
                Column {
                    RiftAppName(RiftTheme.typography.headlineHighlighted)
                    RiftTooltipArea(
                        text = state.buildTime,
                        modifier = Modifier.padding(top = Spacing.medium),
                    ) {
                        Text(
                            text = state.version,
                            style = RiftTheme.typography.titlePrimary,
                        )
                    }
                    AnimatedContent(state.updateAvailability) { isUpdateAvailable ->
                        when (isUpdateAvailable) {
                            is AsyncResource.Error -> {
                                Text(
                                    text = "Could not check for updates",
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            }

                            AsyncResource.Loading -> {
                                Text(
                                    text = "Checking for updates…",
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            }

                            is AsyncResource.Ready -> {
                                Text(
                                    text = "汉化开发版",
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            }
                        }
                    }

                    Text(
                        text = "原版作者：Nohus",
                        style = RiftTheme.typography.titlePrimary,
                        modifier = Modifier.padding(top = Spacing.medium),
                    )

                    LinkText(
                        text = "riftforeve.online",
                        onClick = { "https://riftforeve.online/".toURIOrNull()?.openBrowser() },
                    )
                    Text(
                        text = "汉化版作者：Leito 元元 暗枪 路飞",
                        style = RiftTheme.typography.titlePrimary,
                        modifier = Modifier.padding(top = Spacing.medium),
                    )
                    LinkText(
                        text = "下载地址",
                        onClick = { "https://github.com/Leitowow/RIFT-Simplified-Chinese".toURIOrNull()?.openBrowser() },
                    )
                    Text(
                        text = "加入RIFT汉化版Q群",
                        style = RiftTheme.typography.titlePrimary,
                        modifier = Modifier.padding(top = Spacing.medium),
                    )
                    LinkText(
                        text = "1037664416",
                        onClick = { "http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=4UnAEl8FxsYnSpH_mIju6QeVipLDVR1f&authKey=NgBxKAeRnM29Og%2BNWrDF3VXMqUh08mAwdE7MeMeosJw%2Bna%2FbdMEgqXfT1kPADrIs&noverify=0&group_code=1037664416".toURIOrNull()?.openBrowser() },
                    )

                    Text(
                        text = "© 2023–2024 Nohus",
                        style = RiftTheme.typography.bodySecondary,
                        modifier = Modifier.padding(top = Spacing.medium),
                    )
                    LinkText(
                        text = "Legal & info",
                        onClick = onLegalClick,
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.align(Alignment.End),
        ) {
            RiftButton(
                text = "Debug",
                type = ButtonType.Secondary,
                cornerCut = ButtonCornerCut.None,
                onClick = onDebugClick,
            )
            RiftButton(
                text = "App data",
                type = ButtonType.Secondary,
                cornerCut = ButtonCornerCut.None,
                onClick = onAppDataClick,
            )
            RiftButton(
                text = "Credits",
                type = ButtonType.Secondary,
                cornerCut = ButtonCornerCut.None,
                onClick = onCreditsClick,
            )
            RiftButton(
                text = "What's new",
                type = ButtonType.Primary,
                onClick = onWhatsNewClick,
            )
        }
    }
}

@Composable
private fun getUpdateDialogText(
    operatingSystem: OperatingSystem,
    executablePath: String,
): AnnotatedString {
    return when (operatingSystem) {
        Linux -> {
            buildAnnotatedString {
                append(
                    "If you installed the DEB package, you can update the app with your package manager as normal. " +
                        "For example you can run ",
                )
                withColor(RiftTheme.colors.textHighlighted) {
                    append("sudo apt update && sudo apt upgrade")
                }
                append(".")
                appendLine()
                appendLine()
                append("If you downloaded the ")
                withColor(RiftTheme.colors.textHighlighted) {
                    append(".tar.gz")
                }
                append(" package, then you have to redownload it manually.")
            }
        }

        Windows -> {
            buildAnnotatedString {
                append(
                    "Updates to the app are managed by Windows, which will update it from time to time. " +
                        "If you want to force update to the new version, " +
                        "you can rerun the downloaded installer manually, or check the Microsoft Store " +
                        "if you installed from there.",
                )
            }
        }

        MacOs -> {
            buildAnnotatedString {
                if ("/AppTranslocation/" in executablePath) {
                    append(
                        "Cannot update when ran from the download location. " +
                            "Please move the app to Applications.",
                    )
                } else {
                    append(
                        "Restart the app to apply the latest update.",
                    )
                }
            }
        }
    }
}

@Composable
private fun getLegalText(): AnnotatedString {
    return buildAnnotatedString {
        appendLine(
            "EVE related materials © 2014 CCP hf. All rights reserved. \"EVE\", \"EVE Online\", \"CCP\", " +
                "and all related logos and images are trademarks or registered trademarks of CCP hf.",
        )
        appendLine()
        append("RIFT collects anonymous statistics, like the number of people using it and popularity of features. This ")
        withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
            append("does not")
        }
        appendLine(" include any personal data.")
        appendLine()
        appendLine(
            "These metrics are required by CCP for the EVE Online Partnership Program, as well as helping " +
                "improve RIFT by focusing work on features used the most.",
        )
    }
}

@Composable
private fun getCreditsText(): AnnotatedString {
    return buildAnnotatedString {
        append("Thanks to ")
        withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
            append("smultar")
        }
        append(" for designing the app icon.")
        appendLine()
        append("Thanks to ")
        withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
            append("Steve Ronuken")
        }
        append(" for the SDE conversions.")
        appendLine()
        append("Thanks to ")
        withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
            append("Wollari")
        }
        append(" for the region map layouts.")
        appendLine()
        append("Thanks to ")
        withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted)) {
            append("CCP")
        }
        append(" for creating EVE Online.")
    }
}
