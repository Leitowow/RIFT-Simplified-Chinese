package dev.nohus.rift.network.evescout

import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormStrength.Strong
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.StormStrength.Weak
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.GetSystemsInRangeUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class GetMetaliminalStormsUseCase(
    private val eveScoutRescueApi: EveScoutRescueApi,
    private val getSystemsInRangeUseCase: GetSystemsInRangeUseCase,
) {

    enum class StormType {
        Gamma,
        Electric,
        Plasma,
        Exotic,
    }

    enum class StormStrength {
        Strong,
        Weak,
    }

    data class Storm(
        val type: StormType,
        val strength: StormStrength,
    )

    suspend operator fun invoke(originator: Originator): Map<Int, List<Storm>> {
        val storms = getStormCenters(originator)
        return storms.flatMap { storm ->
            val core = getSystemsInRangeUseCase(storm.key, 1)
            val periphery = getSystemsInRangeUseCase(storm.key, 3) - core
            core.map { it to Storm(storm.value, Strong) } + periphery.map { it to Storm(storm.value, Weak) }
        }.groupBy({ it.first }, { it.second })
    }

    private suspend fun getStormCenters(originator: Originator): Map<Int, StormType> {
        return when (val response = eveScoutRescueApi.getObservations(originator)) {
            is Success -> {
                response.data.mapNotNull { observation ->
                    val type = when (observation.observationType) {
                        ObservationType.ElectricA, ObservationType.ElectricB -> StormType.Electric
                        ObservationType.ExoticA, ObservationType.ExoticB -> StormType.Exotic
                        ObservationType.GammaA, ObservationType.GammaB -> StormType.Gamma
                        ObservationType.PlasmaA, ObservationType.PlasmaB -> StormType.Plasma
                        ObservationType.TomsShuttle -> return@mapNotNull null
                    }
                    observation.systemId to type
                }.toMap()
            }
            is Failure -> {
                logger.error { "Could not get storms from EvE-Scout: ${response.cause?.message}" }
                emptyMap()
            }
        }
    }
}
