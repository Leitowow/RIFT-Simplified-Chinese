package dev.nohus.rift.repositories

import kotlin.math.pow
import kotlin.math.sqrt

data class Position(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    operator fun plus(other: Position): Position {
        return Position(x + other.x, y + other.y, z + other.z)
    }

    fun squaredDistanceTo(position: Position): Double {
        return (x - position.x).pow(2) + (y - position.y).pow(2) + (z - position.z).pow(2)
    }

    fun distanceTo(position: Position): Double {
        return sqrt(squaredDistanceTo(position))
    }
}
