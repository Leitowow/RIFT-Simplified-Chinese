package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.util.lerp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.di.koin
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.OperatingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import java.awt.Component
import java.awt.peer.WindowPeer
import kotlin.math.max

private val logger = KotlinLogging.logger {}

@Single(createdAtStart = true)
class TransparentWindowController(
    private val operatingSystem: OperatingSystem,
    private val settings: Settings,
) {

    /**
     * This is only read once at startup and won't change until a restart
     */
    val isEnabled = settings.isWindowTransparencyEnabled

    /**
     * Compose-level window transparency is disabled on Linux
     */
    fun isComposeWindowTransparent(): Boolean {
        return isEnabled && operatingSystem != OperatingSystem.Linux
    }

    /**
     * System-level window transparency is used on Linux
     */
    @Composable
    fun setTransparency(window: ComposeWindow, enabled: Boolean) {
        if (operatingSystem == OperatingSystem.Linux) {
            val transparencyModifier by settings.updateFlow
                .map { it.windowTransparencyModifier }
                .collectAsState(settings.windowTransparencyModifier)
            val isActive = LocalWindowInfo.current.isWindowFocused
            remember(window, enabled, transparencyModifier, isActive) {
                setLinuxTransparency(window, enabled && isEnabled, transparencyModifier, isActive)
            }
        }
    }

    @Composable
    fun getWindowBackgroundColor(activeTransition: Transition<Boolean>, isTransparent: Boolean): Color {
        if (isTransparent && isComposeWindowTransparent()) {
            val settings: Settings = remember { koin.get() }
            val transparencyModifier by settings.updateFlow
                .map { it.windowTransparencyModifier }
                .collectAsState(settings.windowTransparencyModifier)
            val backgroundColor by activeTransition.animateColor {
                val inactiveAlpha = lerp(0.2f, 0.9f, transparencyModifier)
                val alpha = if (it) {
                    max(0.6f, inactiveAlpha)
                } else {
                    inactiveAlpha
                }
                if (it) {
                    RiftTheme.colors.windowBackgroundActive.copy(alpha = alpha)
                } else {
                    RiftTheme.colors.windowBackground.copy(alpha = alpha)
                }
            }
            return backgroundColor
        } else {
            val backgroundColor by activeTransition.animateColor {
                if (it) {
                    RiftTheme.colors.windowBackgroundActive
                } else {
                    RiftTheme.colors.windowBackground
                }
            }
            return backgroundColor
        }
    }

    @Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
    private fun setLinuxTransparency(window: ComposeWindow, enabled: Boolean, modifier: Float, isActive: Boolean) {
        try {
            val peerField = Component::class.java.getDeclaredField("peer")
            peerField.setAccessible(true)
            val peer = peerField.get(window)
            (peer as WindowPeer).setOpacity(getOpacity(enabled, modifier, isActive))
        } catch (e: ReflectiveOperationException) {
            logger.error { "Could not set window opacity: $e" }
        }
    }

    private fun getOpacity(enabled: Boolean, modifier: Float, isActive: Boolean): Float {
        return when {
            enabled -> if (isActive) {
                0.8f
            } else {
                lerp(0.35f, 0.75f, modifier)
            }
            else -> 1f
        }
    }
}
