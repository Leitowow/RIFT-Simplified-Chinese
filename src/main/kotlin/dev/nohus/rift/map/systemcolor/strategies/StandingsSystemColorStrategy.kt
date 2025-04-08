package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.standings.StandingsRepository
import dev.nohus.rift.standings.getSystemColor
import dev.nohus.rift.utils.roundSecurity
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class StandingsSystemColorStrategy(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val standingsRepository: StandingsRepository,
    @InjectedParam private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return true
    }

    override fun getColor(system: Int): Color {
        val security = solarSystemsRepository.getSystem(system)?.security?.roundSecurity() ?: return Color.Unspecified
        if (security >= 0.5) {
            return Color(0xFF71E754)
        } else if (security > 0.0) return Color(0xFFF5FF83)
        val allianceId = systemStatus[system]?.sovereignty?.allianceId ?: return Color(0xFF7D7E7E)
        val standing = standingsRepository.getStandingLevel(allianceId = allianceId, null, null)
        return standing.getSystemColor()
    }
}
