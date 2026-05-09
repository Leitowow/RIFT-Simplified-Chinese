package dev.nohus.rift.repositories

import org.koin.core.annotation.Single

@Single
class GetSystemsInRangeBandsUseCase(
    private val mapGateConnectionsRepository: MapGateConnectionsRepository,
) {

    /**
     * Returns sets of system IDs that are within the given range of the given system, with each set being
     * 1 jump further than the previous set. The first set will always be the given system.
     */
    operator fun invoke(system: Int, range: Int): List<Set<Int>> {
        return getSystemsInRange(system, range)
    }

    private fun getSystemsInRange(system: Int, range: Int): List<Set<Int>> {
        val neighbors = mapGateConnectionsRepository.systemNeighbors
        if (range == 0) return listOf(setOf(system))

        val allResults = mutableSetOf(system)
        val results = mutableListOf(setOf(system))
        (1..range).forEach { range ->
            val neighboringSystems = results.last().flatMap { system ->
                (neighbors[system]?.toSet() ?: emptySet())
            }.toSet() - allResults
            results += neighboringSystems
            allResults += neighboringSystems
        }

        return results
    }
}
