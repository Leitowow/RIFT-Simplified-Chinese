package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository

class WormholesSystemColorStrategy(
    private val systemStatus: Map<Int, MapStatusRepository.SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.wormholes?.takeIf { it.isNotEmpty() } != null
    }

    override fun getColor(system: Int): Color {
        val wormholes = systemStatus[system]?.wormholes ?: emptyList()
        return if (wormholes.size == 1) {
            val wormhole = wormholes.single()
            when (wormhole.outSystemName) {
                "Thera" -> Color(0xFF2E74DF)
                "Turnur" -> Color(0xFF70E552)
                else -> Color.White
            }
        } else if (wormholes.size > 1) {
            Color.White
        } else {
            Color.White
        }
    }
}
