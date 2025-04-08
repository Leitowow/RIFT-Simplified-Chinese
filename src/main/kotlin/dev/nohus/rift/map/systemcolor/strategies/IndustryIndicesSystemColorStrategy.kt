package dev.nohus.rift.map.systemcolor.strategies

import dev.nohus.rift.map.systemcolor.PercentageSystemColorStrategy
import dev.nohus.rift.network.esi.IndustryActivity
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus

class IndustryIndicesSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
    private val activity: IndustryActivity,
) : PercentageSystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.industryIndices?.contains(activity) == true
    }

    override fun getPercentage(system: Int): Float {
        val index = systemStatus[system]?.industryIndices?.get(activity) ?: return 0f
        return (index * 10).coerceIn(0.05f, 1f) // Multiplied by 10 for 10% to be the 100% intensity value
    }
}
