package dev.nohus.rift.whatsnew

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AffiliateCode
import dev.nohus.rift.compose.Bullet
import dev.nohus.rift.compose.CreatorCode
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.Patrons
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.creator_awards
import dev.nohus.rift.generated.resources.window_redeem
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.viewModel
import dev.nohus.rift.whatsnew.WhatsNewViewModel.UiState
import dev.nohus.rift.whatsnew.WhatsNewViewModel.Version
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource

@Composable
fun WhatsNewWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: WhatsNewViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "What's New",
        icon = Res.drawable.window_redeem,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        WhatsNewWindowContent(
            state = state,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhatsNewWindowContent(
    state: UiState,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        ScrollbarLazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(state.versions) {
                VersionItem(it)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            EveCreatorAwards()
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Box(Modifier.weight(1f)) {
                    AffiliateCode()
                }
                Box(Modifier.weight(1f)) {
                    CreatorCode()
                }
            }
            Patrons(state.patrons, Modifier.fillMaxWidth())
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EveCreatorAwards() {
    Column {
        Image(
            painter = painterResource(Res.drawable.creator_awards),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier
                .onClick {
                    "https://www.eveonline.com/news/view/eve-creator-awards".toURIOrNull()?.openBrowser()
                }
                .hoverBackground()
                .border(1.dp, RiftTheme.colors.borderGreyLight)
                .fillMaxWidth()
                .padding(Spacing.medium),
        ) {
            Text(
                text = "EVE Creator Awards",
                style = RiftTheme.typography.headlineHighlighted.copy(fontWeight = FontWeight.Bold),
            )
            LinkText(
                text = "Nominate RIFT for Third Party App of the Year",
                onClick = {
                    "https://www.eveonline.com/news/view/eve-creator-awards".toURIOrNull()?.openBrowser()
                },
            )
            Text(
                text = "Thank you!",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
    }
}

@Composable
private fun VersionItem(version: Version) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Spacing.medium),
        ) {
            Divider(
                color = RiftTheme.colors.divider,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Version ${version.version}",
                style = RiftTheme.typography.headlineHighlighted.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(horizontal = Spacing.medium),
            )
            Divider(
                color = RiftTheme.colors.divider,
                modifier = Modifier.weight(1f),
            )
        }
        version.points.forEach { point ->
            Row {
                if (!point.isHighlighted) {
                    Bullet()
                    Spacer(Modifier.width(Spacing.medium))
                }
                val style = if (point.isHighlighted) {
                    RiftTheme.typography.headerPrimary.copy(
                        color = RiftTheme.colors.textSpecialHighlighted,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    RiftTheme.typography.headerPrimary
                }
                Text(
                    text = point.text,
                    style = style,
                )
            }
        }
    }
}
