package dev.nohus.rift.dynamicportraits

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacpp.indexer.IntIndexer
import org.bytedeco.javacpp.indexer.UByteIndexer
import org.bytedeco.opencv.global.opencv_core.CV_8UC1
import org.bytedeco.opencv.global.opencv_core.absdiff
import org.bytedeco.opencv.global.opencv_core.subtract
import org.bytedeco.opencv.global.opencv_imgproc.CC_STAT_AREA
import org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2BGRA
import org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY
import org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur
import org.bytedeco.opencv.global.opencv_imgproc.INTER_CUBIC
import org.bytedeco.opencv.global.opencv_imgproc.MORPH_DILATE
import org.bytedeco.opencv.global.opencv_imgproc.MORPH_ELLIPSE
import org.bytedeco.opencv.global.opencv_imgproc.MORPH_ERODE
import org.bytedeco.opencv.global.opencv_imgproc.THRESH_BINARY
import org.bytedeco.opencv.global.opencv_imgproc.connectedComponentsWithStats
import org.bytedeco.opencv.global.opencv_imgproc.cvtColor
import org.bytedeco.opencv.global.opencv_imgproc.floodFill
import org.bytedeco.opencv.global.opencv_imgproc.getStructuringElement
import org.bytedeco.opencv.global.opencv_imgproc.morphologyEx
import org.bytedeco.opencv.global.opencv_imgproc.pyrDown
import org.bytedeco.opencv.global.opencv_imgproc.resize
import org.bytedeco.opencv.global.opencv_imgproc.threshold
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Rect
import org.bytedeco.opencv.opencv_core.Scalar
import org.bytedeco.opencv.opencv_core.Size
import org.koin.core.annotation.Single
import kotlin.math.abs

@Single
class DynamicPortraitImageProcessor {

    companion object {
        const val BACKDROP_SIMILARITY_STEP = 32
    }

    private val kernel15 = getStructuringElement(MORPH_ELLIPSE, Size(15, 15))
    private val size5 = Size(5, 5)
    private val size25 = Size(25, 25)

    data class Output(
        val portrait: Mat,
        val background: Mat,
    )

    suspend fun process(
        portrait: Mat,
        backdrops: List<BackdropsRepository.Backdrop>,
        getBackground: suspend (name: String) -> Mat,
        key: DynamicPortraitKey,
    ): Output {
        return withContext(Dispatchers.Default) {
            processCharacterPortrait(
                portrait = portrait,
                backdrops = backdrops,
                getBackground = getBackground,
                key = key,
            )
        }
    }

    private suspend fun processCharacterPortrait(
        portrait: Mat,
        backdrops: List<BackdropsRepository.Backdrop>,
        getBackground: suspend (name: String) -> Mat,
        key: DynamicPortraitKey,
    ): Output {
        val closestBackgroundName = backdrops.maxBy {
            getImageSimilarity(portrait, it.bytes)
        }.name
        val closestBackground = getBackground(closestBackgroundName)

        val mask = Mat()
        absdiff(portrait, closestBackground, mask)

        cvtColor(mask, mask, COLOR_BGR2GRAY)

        // Blur to reduce JPEG noise
        GaussianBlur(mask, mask, size5, 0.0)

        // Threshold
        threshold(mask, mask, 2.0, 255.0, THRESH_BINARY)

        // Morphological cleanup
        morphologyEx(mask, mask, MORPH_ERODE, kernel15)

        // Find and leave only the largest connected component
        leaveOnlyLargestComponent(mask)

        // Morphological cleanup
        morphologyEx(mask, mask, MORPH_DILATE, kernel15)
        morphologyEx(mask, mask, MORPH_DILATE, kernel15)
        morphologyEx(mask, mask, MORPH_DILATE, kernel15)
        morphologyEx(mask, mask, MORPH_DILATE, kernel15)

        floodFillLargestComponent(mask)
        removeSmallBackgroundRegions(mask)

        morphologyEx(mask, mask, MORPH_ERODE, kernel15)
        morphologyEx(mask, mask, MORPH_ERODE, kernel15)
        morphologyEx(mask, mask, MORPH_ERODE, kernel15)
        morphologyEx(mask, mask, MORPH_ERODE, kernel15)

        // Blur the final mask
        GaussianBlur(mask, mask, size25, 0.0)

        // Apply mask to subject
        val portraitRgba = Mat()
        cvtColor(portrait, portraitRgba, COLOR_BGR2BGRA)
        val portraitIndexer = portraitRgba.createIndexer<UByteIndexer>()
        val maskIndexer = mask.createIndexer<UByteIndexer>()
        for (y in 0L until portrait.rows()) {
            for (x in 0L until portrait.cols()) {
                val value = maskIndexer.get(y, x)
                portraitIndexer.put(y, x, 3L, value)
            }
        }

        // Scale image
        val scaledPortrait = downscaleHighQuality(portraitRgba, key.portraitSize, key.portraitSize)

        // Scale background
        val scaledBackground = downscaleHighQuality(closestBackground, key.backgroundSize, key.backgroundSize)

        maskIndexer.release()
        portraitIndexer.release()
        mask.release()
        portraitRgba.release()
        closestBackground.release()

        return Output(scaledPortrait, scaledBackground)
    }

    private fun getImageSimilarity(image: Mat, backdrop: ByteArray): Int {
        val rows = image.rows()
        val columns = image.cols()
        val indexerA = image.createIndexer() as UByteIndexer
        var matchCount = 0
        val stepSize = BACKDROP_SIMILARITY_STEP

        for (y in 0L until rows step stepSize.toLong()) {
            for (x in 0L until columns step stepSize.toLong()) {
                val channels = 3
                val index = ((y.toInt() / stepSize) * (columns / stepSize) * channels) + ((x.toInt() / stepSize) * channels)
                val red = abs(indexerA.get(y, x, 2) - backdrop[index])
                val green = abs(indexerA.get(y, x, 1) - backdrop[index + 1])
                val blue = abs(indexerA.get(y, x, 0) - backdrop[index + 2])
                val diff = red + green + blue
                if (diff <= 8) matchCount++
            }
        }

        indexerA.release()
        return matchCount
    }

    private fun floodFillLargestComponent(mask: Mat) {
        // Create a mask with a 1-pixel background border around the whole image
        val borderedMask = Mat(mask.rows() + 2, mask.cols() + 2, mask.type(), Scalar(0.0))
        mask.copyTo(borderedMask.apply(Rect(1, 1, mask.cols(), mask.rows())))

        // Flood fill the bordered mask from the corner, which thanks to the border will touch all background regions that touch the edges
        val floodMask = Mat.zeros(borderedMask.rows() + 2, borderedMask.cols() + 2, CV_8UC1).asMat()
        floodFill(borderedMask, floodMask, org.bytedeco.opencv.opencv_core.Point(0, 0), Scalar(128.0))

        // Convert the flood-filled marker (128) back to background (0), and holes (0) to foreground (255)
        val maskIndexer: UByteIndexer = mask.createIndexer()
        val borderedMaskIndexer: UByteIndexer = borderedMask.createIndexer()
        for (y in 0 until mask.rows()) {
            for (x in 0 until mask.cols()) {
                val value = borderedMaskIndexer.get(y.toLong() + 1, x.toLong() + 1)
                if (value == 128) {
                    maskIndexer.put(y.toLong(), x.toLong(), 0)
                } else {
                    maskIndexer.put(y.toLong(), x.toLong(), 255)
                }
            }
        }
        borderedMaskIndexer.release()
        maskIndexer.release()

        borderedMask.release()
        floodMask.release()
    }

    private fun leaveOnlyLargestComponent(mask: Mat) {
        val labels = Mat()
        val stats = Mat()
        val centroids = Mat()
        val componentsCount = connectedComponentsWithStats(mask, labels, stats, centroids)

        var maxArea = 0
        var maxLabel = 1
        // Skip background label 0
        for (i in 1 until componentsCount) {
            val area = stats.ptr(i, CC_STAT_AREA).int
            if (area > maxArea) {
                maxArea = area
                maxLabel = i
            }
        }

        // Create a mask of only the largest component
        val labelsIndexer = labels.createIndexer<IntIndexer>()
        val maskIndexer = mask.createIndexer<UByteIndexer>()
        for (y in 0L until mask.rows()) {
            for (x in 0L until mask.cols()) {
                if (labelsIndexer.get(y, x) != maxLabel) {
                    maskIndexer.put(y, x, 0)
                }
            }
        }

        labelsIndexer.release()
        maskIndexer.release()
        labels.release()
        stats.release()
        centroids.release()
    }

    private fun removeSmallBackgroundRegions(mask: Mat) {
        val invertedMask = Mat()
        subtract(Mat(mask.size(), mask.type(), Scalar(255.0)), mask, invertedMask)

        val labels = Mat()
        val stats = Mat()
        val centroids = Mat()
        val componentsCount = connectedComponentsWithStats(invertedMask, labels, stats, centroids)
        val areas = IntArray(componentsCount)
        for (i in 0 until componentsCount) {
            areas[i] = stats.ptr(i, CC_STAT_AREA).int
        }

        val labelsIndexer = labels.createIndexer<IntIndexer>()
        val maskIndexer = mask.createIndexer<UByteIndexer>()
        for (y in 0L until mask.rows()) {
            for (x in 0L until mask.cols()) {
                val label = labelsIndexer.get(y, x)
                val area = areas[label]
                if (area < 2_000) {
                    maskIndexer.put(y, x, 255)
                }
            }
        }

        labelsIndexer.release()
        maskIndexer.release()
        labels.release()
        stats.release()
        centroids.release()
        invertedMask.release()
    }

    fun downscaleHighQuality(src: Mat, targetWidth: Int, targetHeight: Int): Mat {
        val current = src.clone()

        // Pyramid downsampling until the image cannot be downsampled any further without reaching or passing the target size
        while (current.cols() / 2 > targetWidth && current.rows() / 2 > targetHeight) {
            pyrDown(current, current)
        }

        // Final precise resize
        resize(
            current,
            current,
            Size(targetWidth, targetHeight),
            0.0,
            0.0,
            INTER_CUBIC,
        )
        return current
    }
}
