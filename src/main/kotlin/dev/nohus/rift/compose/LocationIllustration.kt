package dev.nohus.rift.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.point
import dev.nohus.rift.repositories.CelestialsRepository
import dev.nohus.rift.repositories.CelestialsRepository.Celestial
import dev.nohus.rift.repositories.MapGateConnectionsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import org.jetbrains.compose.resources.imageResource
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val red = Color(0xFFB83333)
private val green = Color(0xFF3A803C)
private val blue = Color(0xFF3535A0)
private val white = Color(0xFFB2B2B2)
private val grey = Color(0xFF878787)

private val connectionRegion = Color(0xFF00B200)
private val connectionConstellation = Color(0xFF0000FF)
private val connectionSystem = Color(0xFFFF0000)

@Composable
fun ConstellationIllustrationIconSmall(constellationId: Int) {
    ConstellationIllustrationIcon(
        constellationId = constellationId,
        iconSize = 32.dp,
        pointSize = 6,
        connectionSystem = Color(0xFFB83333),
        connectionRegion = Color(0xFF3A803C),
        connectionConstellation = Color(0xFF3535A0),
        pointColor = Color(0xFFB2B2B2),
    )
}

@Composable
fun ConstellationIllustrationIconBig(constellationId: Int) {
    ConstellationIllustrationIcon(
        constellationId = constellationId,
        iconSize = 128.dp,
        pointSize = 8,
        connectionSystem = connectionSystem,
        connectionRegion = connectionRegion,
        connectionConstellation = connectionConstellation,
        pointColor = Color(0xFFFFFFFF),
    )
}

@Composable
private fun ConstellationIllustrationIcon(
    constellationId: Int,
    iconSize: Dp,
    pointSize: Int,
    connectionSystem: Color,
    connectionRegion: Color,
    connectionConstellation: Color,
    pointColor: Color,
) {
    val animation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animation.animateTo(1f, animationSpec = tween(2500, easing = FastOutSlowInEasing))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(iconSize),
    ) {
        val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
        val mapGateConnectionsRepository: MapGateConnectionsRepository = remember { koin.get() }
        val systemsIdsInConstellation = solarSystemsRepository.getSystemsInConstellation(constellationId)
        val systemsInConstellation = systemsIdsInConstellation.mapNotNull {
            solarSystemsRepository.getSystem(it)
        }
        val systemsOutsideConstellation = systemsIdsInConstellation.flatMap { systemId ->
            mapGateConnectionsRepository.systemNeighbors[systemId] ?: emptyList()
        }.filter { it !in systemsIdsInConstellation }.distinct().mapNotNull {
            solarSystemsRepository.getSystem(it)
        }
        val systems = systemsInConstellation + systemsOutsideConstellation
        val regionIds = systems.associate { it.id to it.regionId }

        val pointImage = imageResource(Res.drawable.point)
        Canvas(
            modifier = Modifier
                .clipToBounds()
                .fillMaxSize(),
        ) {
            val minX = systemsInConstellation.minOf { it.x }
            val maxX = systemsInConstellation.maxOf { it.x }
            val minZ = systemsInConstellation.minOf { it.z }
            val maxZ = systemsInConstellation.maxOf { it.z }
            val width = (maxX - minX).coerceAtLeast(1e13)
            val height = (maxZ - minZ).coerceAtLeast(1e13)
            val sizeScale = 0.7
            val side = maxOf(width, height) / sizeScale
            val yOffset = if (width > height) (width - height) / 2 else 0.0
            val xOffset = if (height > width) (height - width) / 2 else 0.0

            translate(size.width * (1 - sizeScale.toFloat()) / 2, size.height * (1 - sizeScale.toFloat()) / 2) {
                val bigPointSize = pointSize
                val smallPointSize = bigPointSize / 2
                val systemPositions = systems.associate { system ->
                    val targetX = (((system.x - minX) + xOffset) / side * size.width).toFloat()
                    val targetY = (((system.z - minZ) + yOffset) / side * size.height).toFloat()
                    system.id to (targetX to targetY)
                }

                systemsInConstellation.forEach { system ->
                    val neighbors = (mapGateConnectionsRepository.systemNeighbors[system.id] ?: emptyList())
                        .filter { it > system.id || it !in systemsIdsInConstellation }
                    val (fromX, fromY) = systemPositions[system.id] ?: return@forEach
                    neighbors.forEach { neighborId ->
                        val (toX, toY) = systemPositions[neighborId] ?: return@forEach
                        val color = when (neighborId) {
                            in systemsIdsInConstellation -> connectionSystem
                            else -> if (regionIds[system.id] == regionIds[neighborId]) connectionConstellation else connectionRegion
                        }
                        val diff = toX - fromX to toY - fromY
                        drawLine(
                            color = color.copy(alpha = 0.75f),
                            start = Offset(fromX, fromY),
                            end = Offset(fromX + diff.first * animation.value, fromY + diff.second * animation.value),
                            strokeWidth = 2f,
                        )
                    }
                }

                systems.forEach { system ->
                    val pointSize = if (system.id in systemsIdsInConstellation) bigPointSize else smallPointSize
                    val (targetX, targetY) = systemPositions[system.id] ?: return@forEach
                    translate(targetX - pointSize / 2, targetY - pointSize / 2) {
                        drawImage(
                            image = pointImage,
                            colorFilter = ColorFilter.tint(pointColor),
                            dstSize = IntSize(pointSize, pointSize),
                            alpha = (animation.value - 0.5f).coerceAtLeast(0f) * 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RegionIllustrationIconSmall(regionId: Int) {
    RegionIllustrationIcon(
        regionId = regionId,
        iconSize = 32.dp,
        pointSize = 6,
        connectionRegion = Color(0xFF3A803C),
        connectionConstellation = Color(0xFF3535A0),
        pointColor = Color(0xFFB2B2B2),
    )
}

@Composable
fun RegionIllustrationIconBig(regionId: Int) {
    RegionIllustrationIcon(
        regionId = regionId,
        iconSize = 128.dp,
        pointSize = 8,
        connectionRegion = connectionRegion,
        connectionConstellation = connectionConstellation,
        pointColor = Color(0xFFFFFFFF),
    )
}

@Composable
private fun RegionIllustrationIcon(
    regionId: Int,
    iconSize: Dp,
    pointSize: Int,
    connectionRegion: Color,
    connectionConstellation: Color,
    pointColor: Color,
) {
    val animation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animation.animateTo(1f, animationSpec = tween(2500, easing = FastOutSlowInEasing))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(iconSize),
    ) {
        val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
        val mapGateConnectionsRepository: MapGateConnectionsRepository = remember { koin.get() }
        val constellationIdsInRegion = solarSystemsRepository.getSystemsInRegion(regionId)
            .mapNotNull { solarSystemsRepository.getSystem(it)?.constellationId }
            .distinct()
        val constellationsInRegion = constellationIdsInRegion.mapNotNull {
            solarSystemsRepository.getConstellation(it)
        }

        val constellationNeighbors = solarSystemsRepository.getSystemsInRegion(regionId)
            .flatMap { listOf(it) + (mapGateConnectionsRepository.systemNeighbors[it] ?: emptyList()) }
            .distinct()
            .mapNotNull { solarSystemsRepository.getSystem(it) }
            .map {
                val neighborIds = mapGateConnectionsRepository.systemNeighbors[it.id] ?: emptyList()
                val neighbors = neighborIds.mapNotNull { solarSystemsRepository.getSystem(it) }
                it.constellationId to neighbors.map { it.constellationId }.distinct()
            }
            .groupBy { it.first }
            .entries
            .associate { (from, to) ->
                from to to.flatMap { it.second }.distinct()
            }

        val constellationsOutsideRegion = solarSystemsRepository.getSystemsInRegion(regionId)
            .flatMap { mapGateConnectionsRepository.systemNeighbors[it] ?: emptyList() }
            .distinct()
            .mapNotNull { solarSystemsRepository.getSystem(it) }
            .filter { it.constellationId !in constellationIdsInRegion }
            .mapNotNull { solarSystemsRepository.getConstellation(it.constellationId) }
        val constellations = constellationsInRegion + constellationsOutsideRegion

        val pointImage = imageResource(Res.drawable.point)
        Canvas(
            modifier = Modifier
                .clipToBounds()
                .fillMaxSize(),
        ) {
            val minX = constellationsInRegion.minOf { it.x }
            val maxX = constellationsInRegion.maxOf { it.x }
            val minZ = constellationsInRegion.minOf { it.z }
            val maxZ = constellationsInRegion.maxOf { it.z }
            val width = (maxX - minX).coerceAtLeast(1e13)
            val height = (maxZ - minZ).coerceAtLeast(1e13)
            val sizeScale = 0.7
            val side = maxOf(width, height) / sizeScale
            val yOffset = if (width > height) (width - height) / 2 else 0.0
            val xOffset = if (height > width) (height - width) / 2 else 0.0

            translate(size.width * (1 - sizeScale.toFloat()) / 2, size.height * (1 - sizeScale.toFloat()) / 2) {
                val bigPointSize = pointSize
                val smallPointSize = bigPointSize / 2
                val constellationPositions = constellations.associate { constellation ->
                    val targetX = (((constellation.x - minX) + xOffset) / side * size.width).toFloat()
                    val targetY = (((constellation.z - minZ) + yOffset) / side * size.height).toFloat()
                    constellation.id to (targetX to targetY)
                }

                constellationsInRegion.forEach { constellation ->
                    val neighbors = (constellationNeighbors[constellation.id] ?: emptyList())
                        .filter { it > constellation.id || it !in constellationIdsInRegion }
                    val (fromX, fromY) = constellationPositions[constellation.id] ?: return@forEach
                    neighbors.forEach { neighborId ->
                        val (toX, toY) = constellationPositions[neighborId] ?: return@forEach
                        val color = when (neighborId) {
                            in constellationIdsInRegion -> connectionConstellation
                            else -> connectionRegion
                        }
                        val diff = toX - fromX to toY - fromY
                        drawLine(
                            color = color.copy(alpha = 0.75f),
                            start = Offset(fromX, fromY),
                            end = Offset(fromX + diff.first * animation.value, fromY + diff.second * animation.value),
                            strokeWidth = 2f,
                        )
                    }
                }

                constellations.forEach { constellation ->
                    val pointSize = if (constellation.id in constellationIdsInRegion) bigPointSize else smallPointSize
                    val (targetX, targetY) = constellationPositions[constellation.id] ?: return@forEach
                    translate(targetX - pointSize / 2, targetY - pointSize / 2) {
                        drawImage(
                            image = pointImage,
                            colorFilter = ColorFilter.tint(pointColor),
                            dstSize = IntSize(pointSize, pointSize),
                            alpha = (animation.value - 0.5f).coerceAtLeast(0f) * 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemIllustrationIconSmall(
    solarSystemId: Int,
    size: Dp = 32.dp,
    animation: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        if (animation.value == 0f) {
            animation.animateTo(1f, animationSpec = tween(2500, easing = LinearOutSlowInEasing))
        } else {
            animation.snapTo(1f)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size),
    ) {
        val celestialsRepository: CelestialsRepository = remember { koin.get() }
        val celestials = celestialsRepository.getCelestials(solarSystemId)
            .filter { it.type.groupId in listOf(7, 10) } // Planets, Stargates
        if (celestials.isNotEmpty()) {
            val maxRadius = celestials
                .maxOf { it.position.x * it.position.x + it.position.z * it.position.z }
                .let { sqrt(it) }

            val pointImage = imageResource(Res.drawable.point)
            val orbits = celestials.filter { it.type.groupId == 7 }.map { planet ->
                val x = planet.position.x
                val z = planet.position.z
                val distance = sqrt(x * x + z * z)
                distance
            }
            Canvas(
                modifier = Modifier.size(size * 0.75f),
            ) {
                orbits.forEach { orbit ->
                    drawCircle(
                        color = Color.White,
                        radius = (orbit / maxRadius * this.size.width / 2).toFloat(),
                        alpha = 0.2f,
                        style = Stroke(width = 2.0f),
                    )
                }
                celestials.forEach { celestial ->
                    val xOffset = if (celestial.type.groupId == 10) 6f else 0f
                    val x = -celestial.position.x / maxRadius * this.size.width / 2 + xOffset
                    val y = -celestial.position.z / maxRadius * this.size.height / 2
                    val (color, diameter) = when (celestial.type.groupId) {
                        6 -> Color.Transparent to 0f // Sun
                        7 -> grey to 6f // Planet
                        8 -> grey to 2f // Moon
                        9 -> blue to 2f // Asteroid Belt
                        10 -> green to 6f // Stargate
                        15 -> red to 2f // Station
                        else -> Color.Transparent to 0f
                    }

                    val angle = (atan2(x, y) + 2 * Math.PI) % (2 * Math.PI)
                    val distance = sqrt(x * x + y * y)
                    val animatedAngle = angle * animation.value
                    val animatedX = sin(animatedAngle) * distance - diameter / 2
                    val animatedY = cos(animatedAngle) * distance - diameter / 2

                    translate(this.size.width / 2 + animatedX.toFloat(), this.size.height / 2 + animatedY.toFloat()) {
                        drawImage(
                            image = pointImage,
                            colorFilter = ColorFilter.tint(color),
                            dstSize = IntSize(diameter.toInt(), diameter.toInt()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemIllustrationIconBig(solarSystemId: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(128.dp),
    ) {
        val celestialsRepository: CelestialsRepository = remember { koin.get() }
        val celestials = celestialsRepository.getCelestials(solarSystemId)
            .filter { it.type.groupId in listOf(7, 8, 9, 10, 15) }
        if (celestials.isNotEmpty()) {
            val maxRadius = celestials
                .maxOf { it.position.x * it.position.x + it.position.z * it.position.z }
                .let { sqrt(it) }

            val pointImage = imageResource(Res.drawable.point)
            val orbits = celestials.filter { it.type.groupId == 7 }.map { planet ->
                val x = planet.position.x
                val z = planet.position.z
                val distance = sqrt(x * x + z * z)
                distance
            }
            Canvas(
                modifier = Modifier.size((128 * 0.8).dp),
            ) {
                orbits.forEach { orbit ->
                    drawCircle(
                        color = Color.White,
                        radius = (orbit / maxRadius * this.size.width / 2).toFloat(),
                        alpha = 0.2f,
                        style = Stroke(width = 2.0f),
                    )
                }

                val celestialPositions = celestials.associate { celestial ->
                    val x = -celestial.position.x / maxRadius * this.size.width / 2
                    val y = -celestial.position.z / maxRadius * this.size.height / 2
                    celestial.id to (x to y)
                }

                fun getChildren(parent: Int): List<Celestial> {
                    return celestials.filter { it.orbitId == parent }.flatMap { child ->
                        listOf(child) + getChildren(child.id)
                    }
                }

                fun draw(
                    groupId: Int,
                    x: Double,
                    y: Double,
                ) {
                    val xOffset = if (groupId == 10) 6f else 0f
                    val (color, diameter) = when (groupId) {
                        6 -> Color.Transparent to 0f // Sun
                        7 -> grey to 8f // Planet
                        8 -> grey to 4f // Moon
                        9 -> Color(0xFF0000FF) to 4f // Asteroid Belt
                        10 -> Color(0xFF008700) to 8f // Stargate
                        15 -> Color(0xFFFF3333) to 4f // Station
                        else -> Color.Transparent to 0f
                    }

                    val targetX = x + xOffset - diameter / 2
                    val targetY = y - diameter / 2
                    translate(size.width / 2 + targetX.toFloat(), size.height / 2 + targetY.toFloat()) {
                        drawImage(
                            image = pointImage,
                            colorFilter = ColorFilter.tint(color),
                            dstSize = IntSize(diameter.toInt(), diameter.toInt()),
                        )
                    }
                }

                celestials.filter { it.orbitId !in celestialPositions }.forEach { celestial ->
                    val (celestialX, celestialY) = celestialPositions[celestial.id] ?: return@forEach

                    val children = getChildren(celestial.id)
                    if (children.isNotEmpty()) {
                        val multiplicationNeeded = 4096 / 128

                        children.sortedByDescending { child ->
                            val (childX, childY) = celestialPositions[child.id] ?: return@sortedByDescending 0.0
                            val diffX = childX - celestialX
                            val diffY = childY - celestialY
                            val distanceFromParent = sqrt(diffX * diffX + diffY * diffY)
                            distanceFromParent
                        }.forEach { child ->
                            val (childX, childY) = celestialPositions[child.id] ?: return@forEach
                            val diffX = childX - celestialX
                            val diffY = childY - celestialY
                            val x = celestialX + diffX * multiplicationNeeded
                            val y = celestialY + diffY * multiplicationNeeded
                            draw(child.type.groupId, x, y)
                        }
                    }

                    draw(celestial.type.groupId, celestialX, celestialY)
                }
            }
        }
    }
}
