package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import dev.nohus.rift.repositories.RatsRepository.RatType

class RatsTypeSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return getRatsType(system) != null
    }

    override fun getColor(system: Int): Color {
        return when (getRatsType(system)) {
            RatType.BloodRaiders -> Color(0xFF5F0000)
            RatType.Guristas -> Color(0xFF9B761E)
            RatType.SanshasNation -> Color(0xFF621A9F)
            RatType.Serpentis -> Color(0xFFB5DB1E)
            RatType.AngelCartel -> Color(0xFFBCB099)
            RatType.RogueDrones -> Color(0xFFD55252)
            RatType.TriglavianCollective -> Color(0xFFE31919)
            null -> Color.Unspecified
        }
    }

    private fun getRatsType(system: Int): RatType? {
        return systemStatus[system]?.ratType
    }
}
