package dev.nohus.rift.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.contact_tag
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.utils.plural
import org.jetbrains.compose.resources.painterResource
import kotlin.text.appendLine

@Composable
fun ContactLabelTag(details: CharacterDetailsRepository.CharacterDetails, modifier: Modifier = Modifier) {
    if (details.characterLabels.isNotEmpty() || details.corporationLabels.isNotEmpty() || details.allianceLabels.isNotEmpty()) {
        RiftTooltipArea(
            text = buildAnnotatedString {
                if (details.characterLabels.isNotEmpty()) {
                    withStyle(RiftTheme.typography.detailSecondary.toSpanStyle()) {
                        appendLine("Character label${details.characterLabels.size.plural}")
                    }
                    appendLine(details.characterLabels.joinToString(", "))
                }
                if (details.corporationLabels.isNotEmpty()) {
                    withStyle(RiftTheme.typography.detailSecondary.toSpanStyle()) {
                        appendLine("Corporation label${details.corporationLabels.size.plural}")
                    }
                    appendLine(details.corporationLabels.joinToString(", "))
                }
                if (details.allianceLabels.isNotEmpty()) {
                    withStyle(RiftTheme.typography.detailSecondary.toSpanStyle()) {
                        appendLine("Alliance label${details.allianceLabels.size.plural}")
                    }
                    appendLine(details.allianceLabels.joinToString(", "))
                }
            }.trim() as AnnotatedString,
        ) {
            Image(
                painter = painterResource(Res.drawable.contact_tag),
                contentDescription = null,
                modifier = modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun ContactLabelTag(details: CharacterDetailsRepository.CorporationDetails, modifier: Modifier = Modifier) {
    if (details.corporationLabels.isNotEmpty() || details.allianceLabels.isNotEmpty()) {
        RiftTooltipArea(
            text = buildAnnotatedString {
                if (details.corporationLabels.isNotEmpty()) {
                    withStyle(RiftTheme.typography.detailSecondary.toSpanStyle()) {
                        appendLine("Corporation label${details.corporationLabels.size.plural}")
                    }
                    appendLine(details.corporationLabels.joinToString(", "))
                }
                if (details.allianceLabels.isNotEmpty()) {
                    withStyle(RiftTheme.typography.detailSecondary.toSpanStyle()) {
                        appendLine("Alliance label${details.allianceLabels.size.plural}")
                    }
                    appendLine(details.allianceLabels.joinToString(", "))
                }
            }.trim() as AnnotatedString,
        ) {
            Image(
                painter = painterResource(Res.drawable.contact_tag),
                contentDescription = null,
                modifier = modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun ContactLabelTag(details: CharacterDetailsRepository.AllianceDetails, modifier: Modifier = Modifier) {
    if (details.allianceLabels.isNotEmpty()) {
        RiftTooltipArea(
            text = buildAnnotatedString {
                if (details.allianceLabels.isNotEmpty()) {
                    withStyle(RiftTheme.typography.detailSecondary.toSpanStyle()) {
                        appendLine("Alliance label${details.allianceLabels.size.plural}")
                    }
                    appendLine(details.allianceLabels.joinToString(", "))
                }
            }.trim() as AnnotatedString,
        ) {
            Image(
                painter = painterResource(Res.drawable.contact_tag),
                contentDescription = null,
                modifier = modifier.size(16.dp),
            )
        }
    }
}
