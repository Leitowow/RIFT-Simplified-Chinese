package dev.nohus.rift.standings

import androidx.compose.ui.graphics.Color

fun Standing.getColor(): Color? {
    return when (this) {
        Standing.Terrible -> Color(0xFFFF494F)
        Standing.Bad -> Color(0xFFFF945A)
        Standing.Neutral -> null
        Standing.Good -> Color(0xFF316BCA)
        Standing.Excellent -> Color(0xFF0062FF)
    }
}

fun Standing.getFlagColor(): Color {
    return when (this) {
        Standing.Terrible -> Color(0xFFBF0000)
        Standing.Bad -> Color(0xFFFF5900)
        Standing.Neutral -> Color(0xFFB2B2B2)
        Standing.Good -> Color(0xFF337FFF)
        Standing.Excellent -> Color(0xFF002699)
    }
}

fun Standing.getSystemColor(): Color {
    return when (this) {
        Standing.Terrible -> Color(0xFFBB1116)
        Standing.Bad -> Color(0xFFCE440F)
        Standing.Neutral -> Color(0xFF8D3163)
        Standing.Good -> Color(0xFF4ECEF8)
        Standing.Excellent -> Color(0xFF2C75E1)
    }
}

val Standing.isFriendly: Boolean get() {
    return when (this) {
        Standing.Terrible -> false
        Standing.Bad -> false
        Standing.Neutral -> false
        Standing.Good -> true
        Standing.Excellent -> true
    }
}

object StandingUtils {
    fun getStandingLevel(standing: Float): Standing {
        return when {
            standing > 5.0f -> Standing.Excellent
            standing > 0f -> Standing.Good
            standing == 0f -> Standing.Neutral
            standing >= -5.0f -> Standing.Bad
            else -> Standing.Terrible
        }
    }

    fun formatStanding(standing: Float): String {
        return if (standing >= 9.995) {
            "10.0"
        } else {
            String.format("%.1f", standing)
        }
    }
}
