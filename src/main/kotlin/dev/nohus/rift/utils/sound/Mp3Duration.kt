package dev.nohus.rift.utils.sound

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.BitstreamException
import java.io.IOException
import java.lang.NullPointerException
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.math.roundToLong

fun getMp3FileDuration(path: Path): Duration {
    try {
        val header = Bitstream(path.inputStream()).readFrame()
        val size = path.fileSize().toInt()
        return Duration.ofMillis(header.total_ms(size).roundToLong())
    } catch (e: IOException) {
        return Duration.ZERO
    } catch (ex: BitstreamException) {
        return Duration.ZERO
    } catch (e: NullPointerException) {
        return Duration.ZERO
    }
}
