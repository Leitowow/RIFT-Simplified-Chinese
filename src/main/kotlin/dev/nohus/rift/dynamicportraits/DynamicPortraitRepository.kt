package dev.nohus.rift.dynamicportraits

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.network.imageserver.ImageServerApi
import dev.nohus.rift.network.requests.Originator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.opencv.global.opencv_core.CV_8UC1
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_UNCHANGED
import org.bytedeco.opencv.global.opencv_imgcodecs.imdecode
import org.bytedeco.opencv.opencv_core.Mat
import org.koin.core.annotation.Single
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Single
class DynamicPortraitRepository(
    backdropsRepository: BackdropsRepository,
    private val imageServer: ImageServerApi,
    private val dynamicPortraitDiskRepository: DynamicPortraitDiskRepository,
    private val dynamicPortraitImageProcessor: DynamicPortraitImageProcessor,
    private val matToImageBitmapConverter: MatToImageBitmapConverter,
) {

    private val imageSize = 1024
    private val backdrops = backdropsRepository.getBackdrops()
    private val worker = object : IdempotentWorker<DynamicPortraitKey, DynamicPortraitImageProcessor.Output>() {
        override val hasInternalCache: Boolean = false
        override suspend fun process(input: DynamicPortraitKey): DynamicPortraitImageProcessor.Output? {
            return processCharacterPortrait(input)
        }
    }

    suspend fun getCharacterPortrait(characterId: Int, portraitSize: Int, backgroundSize: Int): DynamicPortrait? {
        val key = DynamicPortraitKey(characterId, portraitSize, backgroundSize)
        val portrait = dynamicPortraitDiskRepository.load(key)
        if (portrait != null) return portrait
        val output = worker.getOutput(key) ?: return null
        logger.info { "Generated portrait for character $characterId" }
        return getDynamicPortrait(output)
    }

    /**
     * Converts the output of the image processor to a [DynamicCharacterPortrait]
     */
    private suspend fun getDynamicPortrait(output: DynamicPortraitImageProcessor.Output): DynamicPortrait {
        return withContext(Dispatchers.Default) {
            val portraitBitmap = matToImageBitmapConverter.bgraMatToImageBitmap(output.portrait)
            val backgroundBitmap = matToImageBitmapConverter.bgrMatToImageBitmap(output.background)
            DynamicPortrait(portraitBitmap, backgroundBitmap)
        }
    }

    /**
     * Downloads a portrait image from the image server, processes it, saves the output to disk, and returns it.
     */
    private suspend fun processCharacterPortrait(key: DynamicPortraitKey): DynamicPortraitImageProcessor.Output? {
        val portrait = getPortraitFromImageServer(key.characterId) ?: return null
        val output = dynamicPortraitImageProcessor.process(
            portrait = portrait,
            backdrops = backdrops,
            getBackground = ::getBackground,
            key = key,
        )
        dynamicPortraitDiskRepository.save(key, output)
        return output
    }

    /**
     * Loads a portrait image from the image server to a Mat
     */
    private suspend fun getPortraitFromImageServer(characterId: Int): Mat? {
        val portraitBytes = try {
            imageServer.getCharacterPortraitOpenCv(Originator.DataPreloading, imageSize, characterId) ?: return null
        } catch (e: IOException) {
            logger.error { "Failed to download portrait for character $characterId: ${e.message}" }
            return null
        }
        return withContext(Dispatchers.Default) {
            val bytePointer = BytePointer(*portraitBytes)
            val buf = Mat(1, portraitBytes.size, CV_8UC1, bytePointer)
            imdecode(buf, IMREAD_UNCHANGED).also {
                buf.release()
            }
        }
    }

    /**
     * Loads a background image from assets to a Mat
     */
    private suspend fun getBackground(name: String): Mat {
        val bytes = withContext(Dispatchers.IO) {
            Res.readBytes("files/backdrops/$name.dds.png")
        }
        return withContext(Dispatchers.Default) {
            val buf = Mat(1, bytes.size, CV_8UC1)
            buf.data().put(bytes, 0, bytes.size)
            imdecode(buf, IMREAD_COLOR).also {
                buf.release()
            }
        }
    }
}
