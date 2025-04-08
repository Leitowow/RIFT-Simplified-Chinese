package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class UiScaleController(
    settings: Settings,
) {

    val uiScale = settings.uiScale

    @Composable
    fun withScale(block: @Composable () -> Unit) {
        val originalDensity = LocalDensity.current
        val targetDensity = Density(originalDensity.density * uiScale, originalDensity.fontScale)
        CompositionLocalProvider(LocalDensity provides targetDensity) {
            block()
        }
    }
}
