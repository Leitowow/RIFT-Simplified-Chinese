package dev.nohus.rift.network.ntp

import java.time.Duration
import java.util.Objects

/**
 * A type similar to [Timestamp64] but used when calculating the difference between two
 * timestamps. As such, it is a signed type, but still uses 64-bits in total and so can only
 * represent half the magnitude of [Timestamp64].
 *
 *
 * See [4. Time Difference Calculations](https://www.eecis.udel.edu/~mills/time.html).
 *
 * @hide
 */
class Duration64 private constructor(private val mBits: Long) {
    /**
     * Add two [Duration64] instances together. This performs the calculation in [ ] and returns a [Duration] to increase the magnitude of accepted arguments,
     * since [Duration64] only supports signed 32-bit seconds. The use of [Duration]
     * limits precision to nanoseconds.
     */
    fun plus(other: Duration64): Duration {
        // From https://www.eecis.udel.edu/~mills/time.html:
        // "The offset and delay calculations require sums and differences of these raw timestamp
        // differences that can span no more than from 34 years in the future to 34 years in the
        // past without overflow. This is a fundamental limitation in 64-bit integer calculations.
        //
        // In the NTPv4 reference implementation, all calculations involving offset and delay values
        // use 64-bit floating double arithmetic, with the exception of raw timestamp subtraction,
        // as mentioned above. The raw timestamp differences are then converted to 64-bit floating
        // double format without loss of precision or chance of overflow in subsequent
        // calculations."
        //
        // Here, we use Duration instead, which provides sufficient range, but loses precision below
        // nanos.
        return this.toDuration().plus(other.toDuration())
    }

    /**
     * Returns a [Duration] equivalent of this duration. Because [Duration64] uses a
     * fixed point type for sub-second values it can values smaller than nanosecond precision and so
     * the conversion can be lossy.
     */
    fun toDuration(): Duration {
        val seconds = this.seconds
        val nanos = this.nanos
        return Duration.ofSeconds(seconds.toLong(), nanos.toLong())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as Duration64
        return mBits == that.mBits
    }

    override fun hashCode(): Int {
        return Objects.hash(mBits)
    }

    override fun toString(): String {
        val duration = toDuration()
        return ("(" + duration.getSeconds() + "s " + duration.getNano() + "ns)")
    }

    val seconds: Int
        /**
         * Returns the *signed* seconds in this duration.
         */
        get() = (mBits shr 32).toInt()
    val nanos: Int
        /**
         * Returns the *unsigned* nanoseconds in this duration (truncated).
         */
        get() = Timestamp64.fractionBitsToNanos((mBits and 0xFFFFFFFFL).toInt())

    companion object {
        val ZERO: Duration64 = Duration64(0)

        /**
         * Returns the difference between two 64-bit NTP timestamps as a [Duration64], as
         * described in the NTP spec. The times represented by the timestamps have to be within [ ][Timestamp64.MAX_SECONDS_IN_ERA] (~68 years) of each other for the calculation to produce a
         * correct answer.
         */
        fun between(startInclusive: Timestamp64, endExclusive: Timestamp64): Duration64 {
            val oneBits = (
                (startInclusive.eraSeconds shl 32)
                    or (startInclusive.fractionBits.toLong() and 0xFFFFFFFFL)
                )
            val twoBits = (
                (endExclusive.eraSeconds shl 32)
                    or (endExclusive.fractionBits.toLong() and 0xFFFFFFFFL)
                )
            val resultBits = twoBits - oneBits
            return Duration64(resultBits)
        }
    }
}
