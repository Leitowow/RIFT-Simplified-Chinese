package dev.nohus.rift.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RiftColors(
    val windowBackground: Color,
    val windowBackgroundSecondary: Color,
    val windowBackgroundSecondaryHovered: Color,
    val windowBackgroundActive: Color,
    val windowBorder: Color,
    val windowBorderActive: Color,

    val textSpecialHighlighted: Color,
    val textHighlighted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val textLink: Color,
    val textLinkHovered: Color,
    val textExternalLink: Color,
    val textExternalLinkHovered: Color,
    val textInviteLink: Color,
    val textInviteLinkHovered: Color,
    val textHelpLink: Color,
    val textHelpLinkHovered: Color,
    val textGreen: Color,
    val textRed: Color,
    val inactiveGray: Color,
    val primary: Color,
    val primaryDark: Color,

    val backgroundPrimary: Color,
    val backgroundPrimaryDark: Color,
    val backgroundPrimaryLight: Color,
    val backgroundSelected: Color,
    val backgroundHovered: Color,
    val backgroundWhite: Color,
    val backgroundError: Color,
    val backgroundErrorDark: Color,

    val borderPrimary: Color,
    val borderPrimaryDark: Color,
    val borderPrimaryLight: Color,
    val borderError: Color,
    val borderGreyLight: Color,
    val borderGrey: Color,
    val borderGreyDropdown: Color,

    val divider: Color,

    val selectionHandle: Color,
    val selectionBackground: Color,

    val dropdownHovered: Color,
    val dropdownSelected: Color,
    val dropdownHighlighted: Color,

    val sliderThumb: Color,
    val sliderThumbSelected: Color,
    val sliderThumbHighlighted: Color,

    val warningBackground: Color,
    val warningColor: Color,

    val awayYellow: Color,
    val extendedAwayOrange: Color,

    val successGreen: Color,
    val hotRed: Color,

    val mapBackground: Color,

    val progressBarBackground: Color,
    val progressBarProgress: Color,

    // 1f for normal, 0f for transparent theme
    val transparentWindowAlpha: Float,
)

val LocalRiftColors = staticCompositionLocalOf {
    RiftColors(
        windowBackground = Color.Unspecified,
        windowBackgroundSecondary = Color.Unspecified,
        windowBackgroundSecondaryHovered = Color.Unspecified,
        windowBackgroundActive = Color.Unspecified,
        windowBorder = Color.Unspecified,
        windowBorderActive = Color.Unspecified,

        textSpecialHighlighted = Color.Unspecified,
        textHighlighted = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        textDisabled = Color.Unspecified,
        textLink = Color.Unspecified,
        textLinkHovered = Color.Unspecified,
        textExternalLink = Color.Unspecified,
        textExternalLinkHovered = Color.Unspecified,
        textInviteLink = Color.Unspecified,
        textInviteLinkHovered = Color.Unspecified,
        textHelpLink = Color.Unspecified,
        textHelpLinkHovered = Color.Unspecified,
        textGreen = Color.Unspecified,
        textRed = Color.Unspecified,

        inactiveGray = Color.Unspecified,
        primary = Color.Unspecified,
        primaryDark = Color.Unspecified,

        backgroundPrimary = Color.Unspecified,
        backgroundPrimaryDark = Color.Unspecified,
        backgroundPrimaryLight = Color.Unspecified,
        backgroundSelected = Color.Unspecified,
        backgroundHovered = Color.Unspecified,
        backgroundWhite = Color.Unspecified,
        backgroundError = Color.Unspecified,
        backgroundErrorDark = Color.Unspecified,

        borderPrimary = Color.Unspecified,
        borderPrimaryDark = Color.Unspecified,
        borderPrimaryLight = Color.Unspecified,
        borderError = Color.Unspecified,
        borderGreyLight = Color.Unspecified,
        borderGrey = Color.Unspecified,
        borderGreyDropdown = Color.Unspecified,

        divider = Color.Unspecified,

        selectionHandle = Color.Unspecified,
        selectionBackground = Color.Unspecified,

        dropdownHovered = Color.Unspecified,
        dropdownSelected = Color.Unspecified,
        dropdownHighlighted = Color.Unspecified,

        sliderThumb = Color.Unspecified,
        sliderThumbSelected = Color.Unspecified,
        sliderThumbHighlighted = Color.Unspecified,

        warningBackground = Color.Unspecified,
        warningColor = Color.Unspecified,

        awayYellow = Color.Unspecified,
        extendedAwayOrange = Color.Unspecified,

        successGreen = Color.Unspecified,
        hotRed = Color.Unspecified,

        mapBackground = Color.Unspecified,

        progressBarBackground = Color.Unspecified,
        progressBarProgress = Color.Unspecified,

        transparentWindowAlpha = 1f,
    )
}

fun getRiftColors(isTransparent: Boolean): RiftColors {
    return if (isTransparent) {
        getRiftColors().run {
            copy(
                windowBackground = windowBackground.copy(alpha = 0.4f),
                windowBackgroundSecondary = Color(0xFFFFFFFF).copy(alpha = 0.05f),
                windowBackgroundActive = windowBackgroundActive.copy(alpha = 0.6f),
                windowBorder = Color(0xFFFFFFFF).copy(alpha = 0.1f),
                windowBorderActive = Color(0xFFFFFFFF).copy(alpha = 0.1f),

                inactiveGray = Color(0xFFFFFFFF).copy(alpha = 0.3f),

                backgroundPrimary = backgroundPrimary.copy(alpha = 0.5f),
                backgroundPrimaryDark = backgroundPrimaryDark.copy(alpha = 0.5f),
                backgroundSelected = backgroundSelected.copy(alpha = 0.5f),
                backgroundHovered = backgroundHovered.copy(alpha = 0.5f),

                borderPrimary = Color(0xFF5B878E),
                borderGreyLight = Color(0xFFFFFFFF).copy(alpha = 0.1f),

                mapBackground = Color.Transparent,

                transparentWindowAlpha = 0f,
            )
        }
    } else {
        getRiftColors()
    }
}

private fun getRiftColors() = RiftColors(
    windowBackground = Color(0xFF070707),
    windowBackgroundSecondary = Color(0xFF141414),
    windowBackgroundSecondaryHovered = Color(0xFF1F272B),
    windowBackgroundActive = Color(0xFF05080A),
    windowBorder = Color(0xFF1F1F1F),
    windowBorderActive = Color(0xFF1E2022),

    textSpecialHighlighted = Color(0xFFC3E9FF),
    textHighlighted = Color(0xE5FFFFFF),
    textPrimary = Color(0xBFFFFFFF),
    textSecondary = Color(0x7FFFFFFF),
    textDisabled = Color(0x4CFFFFFF),
    textLink = Color(0xFFd98d00),
    textLinkHovered = Color(0xFFffb732),
    textExternalLink = Color(0xFFffe400),
    textExternalLinkHovered = Color(0xE5FFFFFF),
    textInviteLink = Color(0xFF6868e1),
    textInviteLinkHovered = Color(0xFF8e8eff),
    textHelpLink = Color(0xFF94ccff),
    textHelpLinkHovered = Color(0xFFaed9ff),
    textGreen = Color(0xFF029C02),
    textRed = Color(0xFFFB0101),

    inactiveGray = Color(0xFF595555),
    primary = EveColors.cryoBlue,
    primaryDark = Color(0xFF41707D),

    backgroundPrimary = Color(0xFF172327),
    backgroundPrimaryDark = Color(0xFF0A1215),
    backgroundPrimaryLight = Color(0xFF36525E),
    backgroundSelected = Color(0xFF355866),
    backgroundHovered = Color(0xFF131C1F),
    backgroundWhite = Color(0xFFEAEAEA),
    backgroundError = Color(0xFF7F2628),
    backgroundErrorDark = Color(0xFF60171A),

    borderPrimary = Color(0xFF41707F),
    borderPrimaryDark = Color(0xFF213841),
    borderPrimaryLight = Color(0xFF71BED3),
    borderError = Color(0xFFFE5B61),
    borderGreyLight = Color(0xFF303030),
    borderGrey = Color(0xFF1E1E1E),
    borderGreyDropdown = Color(0xFF1E2022),

    divider = Color(0xFF1E2022),

    selectionHandle = Color(0xFF5CADC4),
    selectionBackground = Color(0xFF424344),

    dropdownHovered = Color(0xFF18262D),
    dropdownSelected = Color(0xFF16272E),
    dropdownHighlighted = Color(0xFF273E47),

    sliderThumb = Color(0xFF58A7BF),
    sliderThumbSelected = Color(0xFF99E5EE),
    sliderThumbHighlighted = Color(0xFFFFFFFF),

    warningBackground = Color(0xFF180F09),
    warningColor = Color(0xFFF39058),

    awayYellow = EveColors.sandYellow,
    extendedAwayOrange = EveColors.warningOrange,

    successGreen = EveColors.successGreen,
    hotRed = EveColors.hotRed,

    mapBackground = Color(0xFF0A0E15),

    progressBarBackground = Color(0xFF1A1E1F),
    progressBarProgress = Color(0xFF0D557E),

    transparentWindowAlpha = 1f,
)

object EveColors {
    val burnishedGold = Color(0xFF996A1F)
    val cherryRed = Color(0xFF991F24)
    val copperOxideGreen = Color(0xFF415931)
    val cryoBlue = Color(0xFF58A7BF)
    val dangerRed = Color(0xFFFF454B)
    val destinationYellow = Color(0xFFF0FF45)
    val duskyOrange = Color(0xFF9F583A)
    val focusBlue = Color(0xFF58A7BF)
    val hotRed = Color(0xFFFF454B)
    val iceWhite = Color(0xFFC2E5F2)
    val leafyGreen = Color(0xFF8DC169)
    val limeGreen = Color(0xFFB2F84D)
    val omegaYellow = Color(0xFFFFC64A)
    val plexYellow = Color(0xFFFFCC00)
    val primaryBlue = Color(0xFF407196)
    val smokeBlue = Color(0xFF305665)
    val warningOrange = Color(0xFFF39058)
    val sandYellow = Color(0xFFFFB845)
    val successGreen = Color(0xFF8DC169)
    val auraPurple = Color(0xFF956BEC)
    val airTurquoise = Color(0xFF70F0E3)
    val ultramarineBlue = Color(0xFF2E4EBE)
    val paragonBlue = Color(0xFF97D3CB)
    val evermarkGreen = Color(0xFFCEFF01)
    val black = Color(0xFF000000)
    val coalBlack = Color(0xFF1A1A1A)
    val matteBlack = Color(0xFF303030)
    val gunmetalGrey = Color(0xFF4D4D4D)
    val ledGrey = Color(0xFF8A8A8A)
    val silverGrey = Color(0xFFB0B0B0)
    val tungstenGrey = Color(0xFFD9D9D9)
    val platinumGrey = Color(0xFFF2F2F2)
    val white = Color(0xFFFFFFFF)
    val omegaAccent = Color(0xFFFFC64A)
    val omegaAccentStroke = Color(0xFF96794A)
    val omegaSecondary = Color(0xCC221D18)
    val omegaTertiary = Color(0xCC4A3E2C)
    val omegaTagTextRed = Color(0xFFFFD2D2)
    val omegaTagTextGreen = Color(0xFFB6F28C)
    val omegaTagTextBlue = Color(0xFF8CDEF8)
}
