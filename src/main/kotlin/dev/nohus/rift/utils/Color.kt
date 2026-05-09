package dev.nohus.rift.utils

import androidx.compose.ui.graphics.Color

data class HsbColor(val hue: Float, val saturation: Float, val brightness: Float)

fun Color.multiplyBrightness(multiplier: Float): Color {
    val hsbColor = toHsb()
    val newBrightness = hsbColor.brightness * multiplier
    return hsbColor.copy(brightness = newBrightness).toColor()
}

fun Color.toHsb(): HsbColor {
    val r = this.red * 255
    val g = this.green * 255
    val b = this.blue * 255
    val minValue = minOf(r, g, b)
    val maxValue = maxOf(r, g, b)
    val delta = maxValue - minValue
    val brightness = maxValue / 255
    val h = when {
        delta == 0f -> 0f
        r == maxValue -> ((g - b) / delta % 6) / 6
        g == maxValue -> ((b - r) / delta + 2) / 6
        else -> ((r - g) / delta + 4) / 6
    }
    val hue = when {
        h < 0 -> h + 1
        h > 1 -> h - 1
        else -> h
    }
    val saturation = if (delta == 0f) 0f else delta / maxValue
    return HsbColor(hue, saturation, brightness)
}

fun HsbColor.toColor(): Color {
    val hue = hue.coerceIn(0f..1f)
    val saturation = saturation.coerceIn(0f..1f)
    val brightness = brightness.coerceIn(0f..1f)

    if (saturation == 0f) {
        return Color(brightness, brightness, brightness)
    }

    var varH = hue * 6f
    if (varH == 6f) {
        varH = 0f
    }

    val varI = varH.toInt()
    val var1 = brightness * (1 - saturation)
    val var2 = brightness * (1 - saturation * (varH - varI))
    val var3 = brightness * (1 - saturation * (1 - (varH - varI)))

    val (varR, varG, varB) = when (varI) {
        0 -> Triple(brightness, var3, var1)
        1 -> Triple(var2, brightness, var1)
        2 -> Triple(var1, brightness, var3)
        3 -> Triple(var1, var2, brightness)
        4 -> Triple(var3, var1, brightness)
        else -> Triple(brightness, var1, var2)
    }

    return Color(varR, varG, varB)
}
