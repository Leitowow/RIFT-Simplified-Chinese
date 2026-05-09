package dev.nohus.rift.repositories

import org.koin.core.annotation.Single
import java.util.ArrayDeque
import java.util.UUID

@Single
class GetRouteUseCase(
    private val starGatesRepository: StarGatesRepository,
    private val jumpBridgesRepository: JumpBridgesRepository,
) {
    @Volatile private var cachedAdjacencyNoJumpBridges: Map<Int, IntArray>? = null

    @Volatile private var cachedAdjacencyWithJumpBridges: Map<Int, IntArray>? = null

    @Volatile private var cachedAdjacencyWithJumpBridgesKey: UUID? = null
    private val cacheLock = Any()

    operator fun invoke(fromSolarSystem: Int, toSolarSystem: Int, withJumpBridges: Boolean): List<Int>? {
        if (fromSolarSystem == toSolarSystem) return listOf(fromSolarSystem)

        val adjacency = getAdjacencyMap(withJumpBridges)
        if (fromSolarSystem !in adjacency && toSolarSystem !in adjacency) {
            // Not possible to have a route
            return null
        }
        return bidirectionalBfs(fromSolarSystem, toSolarSystem, adjacency)
    }

    private fun getAdjacencyMap(withJumpBridges: Boolean): Map<Int, IntArray> {
        return if (withJumpBridges) {
            val currentKey = jumpBridgesRepository.connectionsKey
            val cachedKey = cachedAdjacencyWithJumpBridgesKey
            val cached = cachedAdjacencyWithJumpBridges

            if (cached != null && cachedKey == currentKey) {
                cached
            } else {
                synchronized(cacheLock) {
                    // Double-check after acquiring the lock
                    val cachedAfterLock = cachedAdjacencyWithJumpBridges
                    val keyAfterLock = cachedAdjacencyWithJumpBridgesKey
                    if (cachedAfterLock != null && keyAfterLock == currentKey) {
                        cachedAfterLock
                    } else {
                        buildAdjacencyMap(withJumpBridges = true).also {
                            cachedAdjacencyWithJumpBridges = it
                            cachedAdjacencyWithJumpBridgesKey = currentKey
                        }
                    }
                }
            }
        } else {
            cachedAdjacencyNoJumpBridges ?: synchronized(cacheLock) {
                cachedAdjacencyNoJumpBridges ?: buildAdjacencyMap(withJumpBridges = false).also {
                    cachedAdjacencyNoJumpBridges = it
                }
            }
        }
    }

    private fun buildAdjacencyMap(withJumpBridges: Boolean): Map<Int, IntArray> {
        val connections = starGatesRepository.connections.toMutableList()
        if (withJumpBridges) {
            val bridgeConnections = jumpBridgesRepository.getConnections().map { it.from.id to it.to.id }
            connections += bridgeConnections
        }
        return connections
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, neighbors) -> neighbors.toIntArray() }
    }

    /**
     * Bidirectional BFS.
     * Expands the smaller frontier at each step to reduce visited nodes substantially.
     */
    private fun bidirectionalBfs(
        startSolarSystem: Int,
        goalSolarSystem: Int,
        adjacency: Map<Int, IntArray>,
    ): List<Int>? {
        val frontierStart = ArrayDeque<Int>().apply { add(startSolarSystem) }
        val frontierGoal = ArrayDeque<Int>().apply { add(goalSolarSystem) }

        val parentFromStart = HashMap<Int, Int>(128)
        val parentFromGoal = HashMap<Int, Int>(128)

        val visitedStart = HashSet<Int>(128).apply { add(startSolarSystem) }
        val visitedGoal = HashSet<Int>(128).apply { add(goalSolarSystem) }

        fun expandFrontier(
            frontier: ArrayDeque<Int>,
            visitedSelf: MutableSet<Int>,
            visitedOther: Set<Int>,
            parentSelf: MutableMap<Int, Int>,
        ): Int? {
            val levelSize = frontier.size
            repeat(levelSize) {
                val current = frontier.removeFirst()
                val neighbors = adjacency[current] ?: IntArray(0)
                for (neighbor in neighbors) {
                    if (!visitedSelf.add(neighbor)) continue
                    parentSelf[neighbor] = current
                    if (neighbor in visitedOther) return neighbor
                    frontier.addLast(neighbor)
                }
            }
            return null
        }

        while (frontierStart.isNotEmpty() && frontierGoal.isNotEmpty()) {
            val meeting = if (frontierStart.size <= frontierGoal.size) {
                expandFrontier(frontierStart, visitedStart, visitedGoal, parentFromStart)
            } else {
                expandFrontier(frontierGoal, visitedGoal, visitedStart, parentFromGoal)
            }
            if (meeting != null) {
                return reconstructBidirectionalPath(
                    meeting,
                    parentFromStart,
                    parentFromGoal,
                    startSolarSystem,
                    goalSolarSystem,
                )
            }
        }

        return null
    }

    private fun reconstructBidirectionalPath(
        meet: Int,
        parentFromStart: Map<Int, Int>,
        parentFromGoal: Map<Int, Int>,
        startSolarSystem: Int,
        goalSolarSystem: Int,
    ): List<Int> {
        // Build path start -> meet
        val left = ArrayList<Int>()
        var current = meet
        left.add(current)
        while (current != startSolarSystem) {
            val parent = parentFromStart[current] ?: break
            current = parent
            left.add(current)
        }
        left.reverse()

        // Build path meet -> goal (using parentFromGoal, which is child->parent from the goal side)
        val right = ArrayList<Int>()
        current = meet
        while (current != goalSolarSystem) {
            val parent = parentFromGoal[current] ?: break
            current = parent
            right.add(current)
        }

        // Merge, avoid duplicating 'meet'
        if (left.isNotEmpty() && right.isNotEmpty() && left.last() == meet) {
            left.addAll(right)
            return left
        }

        // Fallback if something odd happened
        return left + right
    }
}
