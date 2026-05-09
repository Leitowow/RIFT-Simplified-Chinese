package dev.nohus.rift.dynamicportraits

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import dev.nohus.rift.utils.directories.AppDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bytedeco.opencv.global.opencv_imgcodecs.imwrite
import org.bytedeco.opencv.opencv_core.Mat
import org.jetbrains.skia.Image
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes

private val logger = KotlinLogging.logger {}

@Single
class DynamicPortraitDiskRepository(
    appDirectories: AppDirectories,
) {

    private val portraitsDirectory = appDirectories.getAppCacheDirectory().resolve("portraits").apply {
        createDirectories()
    }

    suspend fun save(key: DynamicPortraitKey, output: DynamicPortraitImageProcessor.Output) {
        try {
            val directory = portraitsDirectory.resolve(key.characterId.toString()).apply { createDirectories() }
            withContext(Dispatchers.IO) {
                save(directory, "portrait-${key.portraitSize}.png", output.portrait)
                save(directory, "background-${key.backgroundSize}.png", output.background)
            }
        } catch (e: IOException) {
            logger.error { "Failed saving portrait for character ${key.characterId}: ${e.message}" }
        }
    }

    suspend fun load(key: DynamicPortraitKey): DynamicPortrait? {
        return try {
            val directory = portraitsDirectory.resolve(key.characterId.toString())
            val portraitPath = directory.resolve("portrait-${key.portraitSize}.png")
            if (!portraitPath.exists()) return null
            val backgroundPath = directory.resolve("background-${key.backgroundSize}.png")
            if (!backgroundPath.exists()) return null

            withContext(Dispatchers.IO) {
                DynamicPortrait(
                    portrait = loadImageBitmap(portraitPath).await(),
                    background = loadImageBitmap(backgroundPath).await(),
                )
            }
        } catch (e: IOException) {
            logger.error { "Failed loading portrait for character ${key.characterId}: ${e.message}" }
            null
        }
    }

    private fun CoroutineScope.save(directory: Path, name: String, mat: Mat) {
        launch {
            val path = directory.resolve(name).toString()
            imwrite(path, mat)
        }
    }

    private fun CoroutineScope.loadImageBitmap(path: Path): Deferred<ImageBitmap> {
        return async {
            val bytes = path.readBytes()
            val skiaImage = Image.makeFromEncoded(bytes)
            skiaImage.toComposeImageBitmap()
        }
    }
}
