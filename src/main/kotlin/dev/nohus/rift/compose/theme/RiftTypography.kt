package dev.nohus.rift.compose.theme

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.nohus.rift.compose.PreviewContainer
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.evesansneue_bold
import dev.nohus.rift.generated.resources.evesansneue_bolditalic
import dev.nohus.rift.generated.resources.evesansneue_italic
import dev.nohus.rift.generated.resources.evesansneue_regular
import dev.nohus.rift.generated.resources.triglavian
import org.jetbrains.compose.resources.Font

@Immutable
data class RiftTypography(
    val detailBoldPrimary: TextStyle,
    val detailHighlighted: TextStyle,
    val detailPrimary: TextStyle,
    val detailSecondary: TextStyle,
    val detailDisabled: TextStyle,
    val bodySpecialHighlighted: TextStyle,
    val bodyHighlighted: TextStyle,
    val bodyPrimary: TextStyle,
    val bodySecondary: TextStyle,
    val bodyDisabled: TextStyle,
    val bodyLink: TextStyle,
    val bodyTriglavian: TextStyle,
    val headerHighlighted: TextStyle,
    val headerPrimary: TextStyle,
    val headerSecondary: TextStyle,
    val headlineHighlighted: TextStyle,
    val headlinePrimary: TextStyle,
    val headlineSecondary: TextStyle,
    val displayHighlighted: TextStyle,
    val displayPrimary: TextStyle,
    val displaySecondary: TextStyle,
)

val LocalRiftTypography = staticCompositionLocalOf {
    RiftTypography(
        detailBoldPrimary = TextStyle.Default,
        detailHighlighted = TextStyle.Default,
        detailPrimary = TextStyle.Default,
        detailSecondary = TextStyle.Default,
        detailDisabled = TextStyle.Default,
        bodySpecialHighlighted = TextStyle.Default,
        bodyHighlighted = TextStyle.Default,
        bodyPrimary = TextStyle.Default,
        bodySecondary = TextStyle.Default,
        bodyDisabled = TextStyle.Default,
        bodyLink = TextStyle.Default,
        bodyTriglavian = TextStyle.Default,
        headerHighlighted = TextStyle.Default,
        headerPrimary = TextStyle.Default,
        headerSecondary = TextStyle.Default,
        headlineHighlighted = TextStyle.Default,
        headlinePrimary = TextStyle.Default,
        headlineSecondary = TextStyle.Default,
        displayHighlighted = TextStyle.Default,
        displayPrimary = TextStyle.Default,
        displaySecondary = TextStyle.Default,
    )
}

@Composable
fun getRiftTypography(colors: RiftColors): RiftTypography {
    val eveSansNeue = FontFamily(
        Font(
            resource = Res.font.evesansneue_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.evesansneue_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic,
        ),
        Font(
            resource = Res.font.evesansneue_bold,
            weight = FontWeight.Bold,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.evesansneue_bolditalic,
            weight = FontWeight.Bold,
            style = FontStyle.Italic,
        ),
    )
    val triglavian = FontFamily(
        Font(
            resource = Res.font.triglavian,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
    )

    val base = TextStyle(
        fontFamily = eveSansNeue,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        letterSpacing = 0.0.sp,
        shadow = Shadow(
            offset = Offset(1f, 1f),
            blurRadius = 0f,
            color = Color(0xFF000000).copy(alpha = 0.5f),
        ),
    )
    val detail = base.copy(
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    )
    val body = base.copy(
        fontSize = 14.sp,
    )
    val header = base.copy(
        fontSize = 16.sp,
    )
    val headline = base.copy(
        fontSize = 19.sp,
    )
    val display = base.copy(
        fontSize = 24.sp,
    )

    val baseTriglavian = base.copy(
        fontFamily = triglavian,
    )
    val bodyTriglavian = baseTriglavian.copy(
        fontSize = 16.sp,
    )

    return RiftTypography(
        detailBoldPrimary = detail.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold),
        detailHighlighted = detail.copy(color = colors.textHighlighted),
        detailPrimary = detail.copy(color = colors.textPrimary),
        detailSecondary = detail.copy(color = colors.textSecondary),
        detailDisabled = detail.copy(color = colors.textDisabled),
        bodySpecialHighlighted = body.copy(color = colors.textSpecialHighlighted),
        bodyHighlighted = body.copy(color = colors.textHighlighted),
        bodyPrimary = body.copy(color = colors.textPrimary),
        bodySecondary = body.copy(color = colors.textSecondary),
        bodyDisabled = body.copy(color = colors.textDisabled),
        bodyLink = body.copy(color = colors.textLink, fontWeight = FontWeight.Bold),
        bodyTriglavian = bodyTriglavian,
        headerHighlighted = header.copy(color = colors.textHighlighted),
        headerPrimary = header.copy(color = colors.textPrimary),
        headerSecondary = header.copy(color = colors.textSecondary),
        headlineHighlighted = headline.copy(color = colors.textHighlighted),
        headlinePrimary = headline.copy(color = colors.textPrimary),
        headlineSecondary = headline.copy(color = colors.textSecondary),
        displayHighlighted = display.copy(color = colors.textHighlighted),
        displayPrimary = display.copy(color = colors.textPrimary),
        displaySecondary = display.copy(color = colors.textSecondary),
    )
}

@Preview
@Composable
private fun RiftTypographyPreview() {
    PreviewContainer {
        Text(
            text = "Detail Primary",
            style = RiftTheme.typography.detailPrimary,
        )
        Text(
            text = "Detail Secondary",
            style = RiftTheme.typography.detailSecondary,
        )
        Text(
            text = "Detail Bold Primary",
            style = RiftTheme.typography.detailBoldPrimary,
        )

        Text(
            text = "Body Special Highlighted",
            style = RiftTheme.typography.bodySpecialHighlighted,
        )
        Text(
            text = "Body Highlighted",
            style = RiftTheme.typography.bodyHighlighted,
        )
        Text(
            text = "Body Primary",
            style = RiftTheme.typography.bodyPrimary,
        )
        Text(
            text = "Body Secondary",
            style = RiftTheme.typography.bodySecondary,
        )

        Text(
            text = "Header Highlighted",
            style = RiftTheme.typography.headerHighlighted,
        )
        Text(
            text = "Header Primary",
            style = RiftTheme.typography.headerPrimary,
        )
        Text(
            text = "Header Secondary",
            style = RiftTheme.typography.headerSecondary,
        )

        Text(
            text = "Headline Highlighted",
            style = RiftTheme.typography.headlineHighlighted,
        )
        Text(
            text = "Headline Primary",
            style = RiftTheme.typography.headlinePrimary,
        )
        Text(
            text = "Headline Secondary",
            style = RiftTheme.typography.headlineSecondary,
        )

        Text(
            text = "Display Highlighted",
            style = RiftTheme.typography.displayHighlighted,
        )
        Text(
            text = "Display Primary",
            style = RiftTheme.typography.displayPrimary,
        )
        Text(
            text = "Display Secondary",
            style = RiftTheme.typography.displaySecondary,
        )
    }
}
