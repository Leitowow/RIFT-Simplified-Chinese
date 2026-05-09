package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import dev.nohus.rift.utils.desaturate

class SovereigntyUpgradesSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.sovereigntyUpgrades?.takeIf { it.isNotEmpty() } != null
    }

    override fun getColor(system: Int): Color {
        val upgrades = systemStatus[system]?.sovereigntyUpgrades ?: return Color.Unspecified
        val upgradesWithLevels = upgrades.associateWith { upgrade ->
            when {
                upgrade.type.name.endsWith("3") -> 3
                upgrade.type.name.endsWith("2") -> 2
                upgrade.type.name.endsWith("1") -> 1
                else -> -1
            }
        }
        val (upgrade, level) = upgradesWithLevels.maxBy { it.value }
        val color = when (level) {
            1 -> Color(0xFFEEFF83)
            2 -> Color(0xFFDC6C08)
            3 -> Color(0xFFBC1113)
            else -> Color(0xFF70E552)
        }
        return if (upgrade.isEffect) color.desaturate(0.5f) else color
    }
}
