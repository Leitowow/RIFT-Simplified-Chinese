package dev.nohus.rift.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitStandings
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.standings.getColor
import java.time.Instant

/**
 * Character with full interactive details
 */
@Suppress("UnusedReceiverParameter")
@Composable
fun RowScope.CharacterDetails(
    character: CharacterDetailsRepository.CharacterDetails,
    rowHeight: Dp,
    isAnimated: Boolean,
) {
    ClickableCharacter(character.characterId) {
        RiftTooltipArea(
            text = character.name,
        ) {
            val now = remember(character.characterId) { Instant.now() }
            DynamicCharacterPortraitStandings(
                characterId = character.characterId,
                size = rowHeight,
                standingLevel = character.standingLevel,
                isAnimated = isAnimated,
                enterTimestamp = now,
            )
        }
    }
    ClickableCorporation(character.corporationId) {
        RiftTooltipArea(
            text = character.corporationName ?: "",
        ) {
            AsyncCorporationLogo(
                corporationId = character.corporationId,
                size = 32,
                modifier = Modifier.size(rowHeight),
            )
        }
    }
    if (character.allianceId != null) {
        ClickableAlliance(character.allianceId) {
            RiftTooltipArea(
                text = character.allianceName ?: "",
            ) {
                AsyncAllianceLogo(
                    allianceId = character.allianceId,
                    size = 32,
                    modifier = Modifier.size(rowHeight),
                )
            }
        }
    }

    ClickableCharacter(character.characterId) {
        val ticker = buildString {
            character.corporationTicker?.let { append("$it ") }
            character.allianceTicker?.let { append(it) }
        }
        var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
        character.standingLevel.getColor()?.let { nameStyle = nameStyle.copy(color = it) }
        if (rowHeight < 32.dp) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.padding(horizontal = Spacing.small),
            ) {
                Text(
                    text = ticker,
                    style = RiftTheme.typography.bodySecondary,
                )
                Text(
                    text = character.name,
                    style = nameStyle,
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.small),
            ) {
                Text(
                    text = character.name,
                    style = nameStyle,
                )
                Text(
                    text = ticker,
                    style = RiftTheme.typography.detailSecondary,
                )
            }
        }
    }

    ContactLabelTag(
        details = character,
        modifier = Modifier.padding(end = Spacing.small),
    )
}

@Suppress("UnusedReceiverParameter")
@Composable
fun RowScope.CharacterDetails(
    corporation: CharacterDetailsRepository.CorporationDetails,
    rowHeight: Dp,
    isShowingName: Boolean = true,
    isShowingContactLabel: Boolean = true,
) {
    ClickableCorporation(corporation.corporationId) {
        RiftTooltipArea(
            text = corporation.corporationName,
        ) {
            AsyncCorporationLogo(
                corporationId = corporation.corporationId,
                size = 32,
                modifier = Modifier.size(rowHeight),
            )
        }
    }
    if (corporation.allianceId != null && corporation.allianceName != null) {
        ClickableAlliance(corporation.allianceId) {
            RiftTooltipArea(
                text = corporation.allianceName,
            ) {
                AsyncAllianceLogo(
                    allianceId = corporation.allianceId,
                    size = 32,
                    modifier = Modifier.size(rowHeight),
                )
            }
        }
    }

    if (isShowingName) {
        ClickableCorporation(corporation.corporationId) {
            val ticker = buildString {
                corporation.corporationTicker.let { append("$it ") }
                corporation.allianceTicker?.let { append(it) }
            }
            var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
            corporation.standingLevel.getColor()?.let { nameStyle = nameStyle.copy(color = it) }
            Column(
                modifier = Modifier.padding(horizontal = Spacing.small),
            ) {
                Text(
                    text = corporation.corporationName,
                    style = nameStyle,
                )
                Text(
                    text = ticker,
                    style = RiftTheme.typography.detailSecondary,
                )
            }
        }
    }

    if (isShowingContactLabel) {
        ContactLabelTag(
            details = corporation,
            modifier = Modifier.padding(end = Spacing.small),
        )
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
fun RowScope.CharacterDetails(
    alliance: CharacterDetailsRepository.AllianceDetails,
    rowHeight: Dp,
) {
    ClickableAlliance(alliance.allianceId) {
        RiftTooltipArea(
            text = alliance.allianceName,
        ) {
            AsyncAllianceLogo(
                allianceId = alliance.allianceId,
                size = 32,
                modifier = Modifier.size(rowHeight),
            )
        }
    }

    ClickableAlliance(alliance.allianceId) {
        val ticker = buildString {
            alliance.allianceTicker.let { append(it) }
        }
        var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
        alliance.standingLevel.getColor()?.let { nameStyle = nameStyle.copy(color = it) }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.small),
        ) {
            Text(
                text = alliance.allianceName,
                style = nameStyle,
            )
            Text(
                text = ticker,
                style = RiftTheme.typography.detailSecondary,
            )
        }
    }

    ContactLabelTag(
        details = alliance,
        modifier = Modifier.padding(end = Spacing.small),
    )
}
