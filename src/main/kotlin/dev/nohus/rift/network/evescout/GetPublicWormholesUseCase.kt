package dev.nohus.rift.network.evescout

import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.SolarSystemsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class GetPublicWormholesUseCase(
    private val eveScoutRescueApi: EveScoutRescueApi,
    private val solarSystemsRepository: SolarSystemsRepository,
) {

    data class Wormhole(
        val inType: String,
        val inSignature: String,
        val inSystemId: Int,
        val inSystemName: String,
        val inRegionName: String?,
        val outType: String,
        val outSignature: String,
        val outSystemId: Int,
        val outSystemName: String,
        val outRegionName: String?,
        val maxShipSize: WormholeSize?,
    )

    enum class WormholeSize {
        Small,
        Medium,
        Large,
        XLarge,
        Capital,
    }

    suspend operator fun invoke(originator: Originator): Map<Int, List<Wormhole>> {
        return when (val response = eveScoutRescueApi.getSignatures(originator)) {
            is Success -> {
                response.data
                    .mapNotNull { signature ->
                        val inSystemId = signature.inSystemId ?: return@mapNotNull null
                        val inSystem = solarSystemsRepository.getSystem(inSystemId) ?: return@mapNotNull null
                        val outSystem = solarSystemsRepository.getSystem(signature.outSystemId) ?: return@mapNotNull null
                        val size = when (signature.maxShipSize) {
                            MaxShipSize.Small -> WormholeSize.Small
                            MaxShipSize.Medium -> WormholeSize.Medium
                            MaxShipSize.Large -> WormholeSize.Large
                            MaxShipSize.XLarge -> WormholeSize.XLarge
                            MaxShipSize.Capital -> WormholeSize.Capital
                            MaxShipSize.Unknown -> null
                            null -> null
                        }
                        val ignoredRegionNames = solarSystemsRepository.getWormholeSpaceRegions().map { it.name }
                        val type = signature.whType ?: return@mapNotNull null
                        val isEntrance = signature.whExitsOutward ?: return@mapNotNull null
                        val wormhole = Wormhole(
                            inType = if (!isEntrance) type else "K162",
                            outType = if (isEntrance) type else "K162",
                            inSignature = signature.inSignature ?: return@mapNotNull null,
                            outSignature = signature.outSignature,
                            inSystemId = inSystem.id,
                            outSystemId = outSystem.id,
                            inSystemName = inSystem.name,
                            outSystemName = outSystem.name,
                            inRegionName = solarSystemsRepository.getRegionBySystemId(inSystem.id)?.name.takeIf { it !in ignoredRegionNames },
                            outRegionName = solarSystemsRepository.getRegionBySystemId(outSystem.id)?.name.takeIf { it !in ignoredRegionNames },
                            maxShipSize = size,
                        )
                        listOf(wormhole, wormhole.reverse())
                    }
                    .flatten()
                    .groupBy { wormhole ->
                        wormhole.inSystemId
                    }
            }
            is Failure -> {
                logger.error { "Could not get public wormholes from EvE-Scout: ${response.cause?.message}" }
                emptyMap()
            }
        }
    }

    private fun Wormhole.reverse() = Wormhole(
        inType = outType,
        inSignature = outSignature,
        inSystemId = outSystemId,
        inSystemName = outSystemName,
        inRegionName = outRegionName,
        outType = inType,
        outSignature = inSignature,
        outSystemId = inSystemId,
        outSystemName = inSystemName,
        outRegionName = inRegionName,
        maxShipSize = maxShipSize,
    )
}
