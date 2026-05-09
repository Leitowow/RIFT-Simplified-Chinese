package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.SolarSystemsRepository
import org.koin.core.annotation.Single

@Single
class AsteroidBeltsSystemColorStrategy(
    private val solarSystemsRepository: SolarSystemsRepository,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        val count = solarSystemsRepository.getSystem(system)?.asteroidBeltCount ?: 0
        return count > 0
    }

    override fun getColor(system: Int): Color {
        val count = solarSystemsRepository.getSystem(system)?.asteroidBeltCount ?: 0
        return if (count > 0) Color(0xFF70E552) else Color(0xFFBC1113)
    }
}
