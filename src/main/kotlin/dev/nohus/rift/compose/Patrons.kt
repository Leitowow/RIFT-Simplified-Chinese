package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nohus.rift.about.GetPatronsUseCase.Patron
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Patrons(patrons: List<Patron>, modifier: Modifier = Modifier) {
    RiftTooltipArea(
        text = "Become a member on Patreon",
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .onClick { "https://patreon.com/Nohus".toURIOrNull()?.openBrowser() }
                .hoverBackground()
                .border(1.dp, RiftTheme.colors.borderGreyLight)
                .fillMaxWidth()
                .padding(
                    vertical = Spacing.small,
                ),
        ) {
            Text(
                text = "PATRONS",
                style = RiftTheme.typography.headlineHighlighted.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.heightIn(min = 32.dp),
            ) {
                if (patrons.isEmpty()) {
                    Text(
                        text = "Support RIFT",
                        style = RiftTheme.typography.headerPrimary,
                    )
                } else if (patrons.size == 1) {
                    Patron(patrons.single(), modifier = Modifier.padding(horizontal = Spacing.medium))
                } else {
                    val shuffledPatrons = remember(patrons) { patrons.shuffled() }
                    InfiniteScrollingCarousel(shuffledPatrons, delay = 5_000) { patron ->
                        Patron(patron, modifier = Modifier.padding(end = Spacing.large))
                    }
                }
            }
        }
    }
}

@Composable
private fun Patron(patron: Patron, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f)),
        ) {
            AsyncCharacterPortrait(
                characterId = patron.characterId,
                size = 32,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = patron.name,
            style = RiftTheme.typography.headerPrimary,
        )
    }
}
