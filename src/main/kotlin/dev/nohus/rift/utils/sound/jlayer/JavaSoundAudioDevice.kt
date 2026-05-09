package dev.nohus.rift.utils.sound.jlayer

import io.github.oshai.kotlinlogging.KotlinLogging
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.JavaLayerException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine

private val logger = KotlinLogging.logger {}

class JavaSoundAudioDevice {

    private var open = false
    private var decoder: Decoder? = null
    private var source: SourceDataLine? = null
    private var fmt: AudioFormat? = null
    private var byteBuf = ByteArray(4096)
    var masterGainControl: FloatControl? = null
        private set

    private var audioFormat: AudioFormat?
        get() {
            if (fmt == null) {
                val decoder = decoder
                fmt = AudioFormat(
                    decoder!!.outputFrequency.toFloat(),
                    16,
                    decoder.outputChannels,
                    true,
                    false,
                )
            }
            return fmt
        }
        private set(fmt0) {
            fmt = fmt0
        }

    private val sourceLineInfo: DataLine.Info
        get() {
            val fmt = audioFormat!!
            val info = DataLine.Info(SourceDataLine::class.java, fmt)
            return info
        }

    @Synchronized
    fun isOpen(): Boolean {
        return open
    }

    private fun setOpen(open: Boolean) {
        this.open = open
    }

    @Synchronized
    @Throws(JavaLayerException::class)
    fun open(decoder: Decoder) {
        if (!isOpen()) {
            this.decoder = decoder
            setOpen(true)
        }
    }

    @Synchronized
    fun close() {
        if (isOpen()) {
            closeImpl()
            setOpen(false)
            decoder = null
        }
    }

    @Throws(JavaLayerException::class)
    fun write(samples: ShortArray, offs: Int, len: Int) {
        if (isOpen()) {
            writeImpl(samples, offs, len)
        }
    }

    fun flush() {
        if (isOpen()) {
            flushImpl()
        }
    }

    @Throws(JavaLayerException::class)
    fun open(fmt: AudioFormat?) {
        if (!isOpen()) {
            audioFormat = fmt
            setOpen(true)
        }
    }

    @Throws(JavaLayerException::class)
    private fun createSource() {
        var t: Throwable? = null
        try {
            val line = AudioSystem.getLine(sourceLineInfo)
            if (line is SourceDataLine) {
                source = line
                line.open(fmt)
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val control = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    masterGainControl = control
                } else {
                    logger.error { "MASTER_GAIN not supported" }
                }
                line.start()
            }
        } catch (ex: RuntimeException) {
            t = ex
        } catch (ex: LinkageError) {
            t = ex
        } catch (ex: LineUnavailableException) {
            t = ex
        }
        if (source == null) throw JavaLayerException("cannot obtain source audio line", t)
    }

    private fun closeImpl() {
        source?.close()
    }

    @Throws(JavaLayerException::class)
    fun writeImpl(samples: ShortArray, offs: Int, len: Int) {
        if (source == null) createSource()

        val b = toByteArray(samples, offs, len)
        source?.write(b, 0, len * 2)
    }

    private fun getByteArray(length: Int): ByteArray {
        if (byteBuf.size < length) {
            byteBuf = ByteArray(length + 1024)
        }
        return byteBuf
    }

    private fun toByteArray(samples: ShortArray, offs: Int, len: Int): ByteArray {
        var offs = offs
        var len = len
        val b = getByteArray(len * 2)
        var idx = 0
        var s: Short
        while (len-- > 0) {
            s = samples[offs++]
            b[idx++] = s.toByte()
            b[idx++] = (s.toInt() ushr 8).toByte()
        }
        return b
    }

    private fun flushImpl() {
        source?.drain()
    }

    fun getPosition(): Int {
        return source?.microsecondPosition?.let { it / 1000 }?.toInt() ?: 0
    }
}
