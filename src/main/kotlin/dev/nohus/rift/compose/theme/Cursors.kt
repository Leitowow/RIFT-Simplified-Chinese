package dev.nohus.rift.compose.theme

import dev.nohus.rift.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.skiko.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Cursors {
    private val toolkit = Toolkit.getDefaultToolkit()

    val pointer = createCursor("files/window_cursor.png")
    val pointerInteractive = createCursor("files/window_cursor_interactive.png")
    val pointerDropdown = createCursor("files/window_cursor_dropdown.png")
    val hand = createCursor("files/window_cursor_hand.png")
    val drag = createCursor("files/window_cursor_drag.png")
    val dragHorizontal = createCursor("files/window_cursor_drag_horizontal.png")

    private fun createCursor(resource: String): Cursor {
        val originalImage = runBlocking { ImageIO.read(Res.readBytes(resource).inputStream()) }
        val preferredSize = 32
        val size = toolkit.getBestCursorSize(preferredSize, preferredSize)
        val widthScale = size.width / preferredSize.toDouble()
        val heightScale = size.height / preferredSize.toDouble()
        val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.drawImage(originalImage, 0, 0, size.width, size.height, null)
        graphics.dispose()
        val hotSpot = Point((15 * widthScale).roundToInt(), (14 * heightScale).roundToInt())
        return toolkit.createCustomCursor(image, hotSpot, "normal")
    }
}
