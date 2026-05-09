package dev.nohus.rift.utils.sound.jlayer

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.BitstreamException
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.decoder.SampleBuffer
import java.io.InputStream

class Player(stream: InputStream?) {

    private val bitstream = Bitstream(stream)

    private val decoder = Decoder()

    private var audio: JavaSoundAudioDevice? = null

    private var closed = false

    @get:Synchronized
    var isComplete: Boolean = false
        private set

    private var lastPosition = 0

    init {
        audio = JavaSoundAudioDevice()
        audio!!.open(decoder)
    }

    @Throws(JavaLayerException::class)
    fun play() {
        play(Int.MAX_VALUE)
    }

    @Throws(JavaLayerException::class)
    fun play(frames: Int): Boolean {
        var frames = frames
        var ret = true

        while (frames-- > 0 && ret) {
            ret = decodeFrame()
        }

        if (!ret) {
            val out = audio
            if (out != null) {
                out.flush()
                synchronized(this) {
                    isComplete = (!closed)
                    close()
                }
            }
        }
        return ret
    }

    @Synchronized
    fun close() {
        val out = audio
        if (out != null) {
            closed = true
            audio = null
            out.close()
            lastPosition = out.getPosition()
            try {
                bitstream.close()
            } catch (ex: BitstreamException) {
            }
        }
    }

    val position: Int
        get() {
            var position = lastPosition

            val out = audio
            if (out != null) {
                position = out.getPosition()
            }
            return position
        }

    @Throws(JavaLayerException::class)
    private fun decodeFrame(): Boolean {
        try {
            var out: JavaSoundAudioDevice? = audio ?: return false

            val h = bitstream.readFrame() ?: return false

            // sample buffer set when decoder constructed
            val output = decoder.decodeFrame(h, bitstream) as SampleBuffer

            synchronized(this) {
                out = audio
                if (out != null) {
                    out!!.write(output.buffer, 0, output.bufferLength)
                }
            }

            bitstream.closeFrame()
        } catch (ex: RuntimeException) {
            throw JavaLayerException("Exception decoding audio frame", ex)
        }
        return true
    }

    fun setVolume(volume: Double) {
        val gain = if (volume == 0.0) Float.NEGATIVE_INFINITY else (volume.toFloat() - 1) * 20
        audio?.masterGainControl?.value = gain
    }
}
