package dev.nohus.rift.dynamicportraits

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2BGRA
import org.bytedeco.opencv.global.opencv_imgproc.cvtColor
import org.bytedeco.opencv.opencv_core.Mat
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.koin.core.annotation.Single

/**
 * Converts a JavaCV Mat to a Compose ImageBitmap
 */
@Single
class MatToImageBitmapConverter {

    /**
     * Convert a BGRA Mat (8UC4) to Compose Desktop ImageBitmap.
     * Minimal-copy: only copies Mat bytes once into a ByteArray.
     */
    fun bgraMatToImageBitmap(mat: Mat): ImageBitmap {
        val width = mat.cols()
        val height = mat.rows()
        require(mat.channels() == 4) { "Mat must have 4 channels (BGRA)" }

        // Copy Mat bytes into ByteArray
        val bytes = ByteArray(width * height * 4)
        mat.data().get(bytes)

        // Premultiply alpha in-place
        for (i in bytes.indices step 4) {
            val b = bytes[i].toInt() and 0xFF
            val g = bytes[i + 1].toInt() and 0xFF
            val r = bytes[i + 2].toInt() and 0xFF
            val a = bytes[i + 3].toInt() and 0xFF

            if (a < 255) {
                bytes[i] = ((b * a) / 255).toByte()
                bytes[i + 1] = ((g * a) / 255).toByte()
                bytes[i + 2] = ((r * a) / 255).toByte()
                // alpha unchanged
            }
        }

        // Create Skia Image
        val info = ImageInfo(
            width = width,
            height = height,
            colorType = ColorType.BGRA_8888,
            alphaType = ColorAlphaType.PREMUL,
        )

        val skiaImage = Image.makeRaster(info, bytes, width * 4)

        // Convert to Compose ImageBitmap
        return skiaImage.toComposeImageBitmap()
    }

    fun bgrMatToImageBitmap(mat: Mat): ImageBitmap {
        require(mat.channels() == 3) { "Mat must have 3 channels (BGR)" }

        // Convert BGR -> BGRA
        val bgraMat = Mat()
        cvtColor(mat, bgraMat, CV_BGR2BGRA)

        return bgraMatToImageBitmap(bgraMat).also {
            bgraMat.release()
        }
    }
}
