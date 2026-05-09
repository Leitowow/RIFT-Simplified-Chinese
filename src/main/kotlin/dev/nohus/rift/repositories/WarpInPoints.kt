package dev.nohus.rift.repositories

import dev.nohus.rift.repositories.CelestialsRepository.Celestial
import org.apache.commons.math3.random.MersenneTwister
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object WarpInPoints {

    private const val GROUP_ID_SUN = 6
    private const val GROUP_ID_PLANET = 7
    private const val GROUP_ID_STATION = 15

    fun getWarpInPoint(celestial: Celestial): Position? {
        val position = celestial.position
        val radius = celestial.radius ?: 0.0
        return if (hasWarpInPoint(radius)) {
            when (celestial.type.groupId) {
                GROUP_ID_SUN -> getSunWarpInPoint(position, radius)
                GROUP_ID_PLANET -> getPlanetWarpInPoint(celestial.id, position, radius)
                GROUP_ID_STATION -> null
                else -> getLargeObjectWarpInPoint(position, radius)
            }
        } else {
            null
        }
    }

    private fun hasWarpInPoint(radius: Double): Boolean {
        return radius >= 90_000
    }

    private fun getLargeObjectWarpInPoint(position: Position, radius: Double): Position {
        val offset = 5_000_000
        val x = (radius + offset) * cos(radius)
        val y = 1.3 * radius - 7500
        val z = -(radius + offset) * sin(radius)
        return position + Position(x, y, z)
    }

    private fun getSunWarpInPoint(position: Position, radius: Double): Position {
        val offset = 100_000
        val x = (radius + offset) * cos(radius)
        val y = 0.2 * radius
        val z = -(radius + offset) * sin(radius)
        return position + Position(x, y, z)
    }

    private fun getPlanetWarpInPoint(planetId: Int, position: Position, radius: Double): Position {
        val j = (mersenneTwister(planetId) - 1) / 3
        val theta = asin((position.x / abs(position.x)) * (position.z / (sqrt(position.x.pow(2) + position.z.pow(2))))) + j
        val s = (20 * ((10 * log10(radius / 1_000_000) - 39) / 40).pow(20) + 0.5).coerceIn(0.5, 10.5)
        val d = radius * (s + 1) + 1_000_000
        val x = sin(theta) * d
        val y = radius * sin(j) / 2
        val z = -cos(theta) * d
        return position + Position(x, y, z)
    }

    /**
     * Equivalent to Python's `random.Random(seed).random()`
     */
    private fun mersenneTwister(seed: Int): Double {
        return MersenneTwister(intArrayOf(seed)).nextDouble()
    }
}
