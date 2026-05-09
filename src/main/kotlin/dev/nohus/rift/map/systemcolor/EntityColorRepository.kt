package dev.nohus.rift.map.systemcolor

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.network.imageserver.ImageServerApi
import dev.nohus.rift.network.requests.Originator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.math3.ml.clustering.DoublePoint
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
import org.koin.core.annotation.Single
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

@Single
class EntityColorRepository(
    private val imageServerApi: ImageServerApi,
) {
    private val scope = CoroutineScope(SupervisorJob())

    sealed interface Status {
        data class Existing(val deferred: Deferred<Color?>) : Status
        data class New(val completableDeferred: CompletableDeferred<Color?>) : Status
    }

    private val mutex = Mutex()
    private val deferredsByIds = mutableMapOf<Int, Deferred<Color?>>()
    private val colorsByIds = ConcurrentHashMap<Int, Color>()

    init {
        val factionColors = mapOf(
            500001 to Color(0xFF9AD2E3), // Caldari State
            500002 to Color(0xFF9D452D), // Minmatar Republic
            500003 to Color(0xFFFFEE93), // Amarr Empire
            500004 to Color(0xFF6DB09E), // Gallente Federation
            500005 to Color(0xFF063C26), // Jove Empire
        )
        colorsByIds += factionColors
    }

    fun getFactionColorOrNull(originator: Originator, factionId: Int): Color? {
        colorsByIds[factionId]?.let { return it }
        scope.launch { getColorFromLogo(originator, factionId, imageServerApi::getCorporationLogo, isWaitingForExisting = false) }
        return null
    }

    fun getAllianceColorOrNull(originator: Originator, allianceId: Int): Color? {
        colorsByIds[allianceId]?.let { return it }
        scope.launch { getColorFromLogo(originator, allianceId, imageServerApi::getAllianceLogo, isWaitingForExisting = false) }
        return null
    }

    fun getCorporationColorOrNull(originator: Originator, corporationId: Int): Color? {
        colorsByIds[corporationId]?.let { return it }
        scope.launch { getColorFromLogo(originator, corporationId, imageServerApi::getCorporationLogo, isWaitingForExisting = false) }
        return null
    }

    suspend fun getFactionColor(originator: Originator, factionId: Int): Color? {
        colorsByIds[factionId]?.let { return it }
        return getColorFromLogo(originator, factionId, imageServerApi::getCorporationLogo)
    }

    suspend fun getAllianceColor(originator: Originator, allianceId: Int): Color? {
        colorsByIds[allianceId]?.let { return it }
        return getColorFromLogo(originator, allianceId, imageServerApi::getAllianceLogo)
    }

    suspend fun getCorporationColor(originator: Originator, corporationId: Int): Color? {
        colorsByIds[corporationId]?.let { return it }
        return getColorFromLogo(originator, corporationId, imageServerApi::getCorporationLogo)
    }

    /**
     * Returns the key color of the logo.
     * Can be called concurrently from multiple threads and will only do the work once.
     */
    private suspend fun getColorFromLogo(
        originator: Originator,
        id: Int,
        getLogo: suspend (originator: Originator, id: Int, size: Int) -> BufferedImage?,
        isWaitingForExisting: Boolean = true,
    ): Color? {
        val status = mutex.withLock {
            val deferred = deferredsByIds[id]
            if (deferred == null) {
                Status.New(CompletableDeferred())
            } else {
                Status.Existing(deferred)
            }
        }
        return when (status) {
            is Status.Existing -> {
                // Another coroutine is already calculating the color
                if (isWaitingForExisting) status.deferred.await() else null
            }
            is Status.New -> {
                try {
                    mutex.withLock {
                        deferredsByIds[id] = status.completableDeferred
                    }
                    calculateColorFromLogo(originator, id, getLogo).also {
                        if (it != null) colorsByIds[id] = it
                        status.completableDeferred.complete(it)
                    }
                } finally {
                    mutex.withLock {
                        deferredsByIds -= id
                    }
                }
            }
        }
    }

    private suspend fun calculateColorFromLogo(
        originator: Originator,
        id: Int,
        getLogo: suspend (originator: Originator, id: Int, size: Int) -> BufferedImage?,
    ): Color? {
        return withContext(Dispatchers.Default) {
            val logo = getLogo(originator, id, 32) ?: return@withContext null
            getDominantColor(logo, id)
        }
    }

    private fun getDominantColor(image: BufferedImage, id: Int): Color {
        val k = 5
        val pixels = getPixels(image)
        if (pixels.size < k) {
            // We have less than k pixels (likely 0), so we can't calculate a color, some corporations have 100% transparent logos
            return Color.White
        }
        return KMeansPlusPlusClusterer<DoublePoint>(k)
            .cluster(pixels)
            .mapNotNull { cluster ->
                cluster?.center?.point?.map { it.toFloat() } // Center of the pixel cluster
            }.map { point ->
                Color(point[0], point[1], point[2])
            }.maxByOrNull {
                it.getIntensity()
            } ?: Color.Unspecified
    }

    private fun getPixels(image: BufferedImage): List<DoublePoint> {
        return mutableListOf<DoublePoint>().also {
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    if (image.isTransparent(x, y)) continue
                    val color = Color(image.getRGB(x, y))
                    it.add(DoublePoint(doubleArrayOf(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())))
                }
            }
        }
    }

    private fun BufferedImage.isTransparent(x: Int, y: Int): Boolean {
        return alphaRaster?.getPixel(x, y, IntArray(1))?.first() == 0
    }

    /**
     * Returns the smaller of HSV saturation (more color) and value (less blackness)
     */
    private fun Color.getIntensity(): Float {
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        val delta = max - min
        val saturation = if (max == 0f) 0f else delta / max
        return minOf(saturation, max)
    }
}
