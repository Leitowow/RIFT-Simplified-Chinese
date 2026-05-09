package dev.nohus.rift.network.ntp

import java.time.Instant
import java.util.Objects
import kotlin.random.Random

/**
 * The 64-bit type ("timestamp") that NTP uses to represent a point in time. It only holds the
 * lowest 32-bits of the number of seconds since 1900-01-01 00:00:00. Consequently, to turn an
 * instance into an unambiguous point in time the era number must be known. Era zero runs from
 * 1900-01-01 00:00:00 to a date in 2036.
 *
 * It stores sub-second values using a 32-bit fixed point type, so it can resolve values smaller
 * than a nanosecond, but is imprecise (i.e. it truncates).
 *
 * See also [NTP docs](https://www.eecis.udel.edu/~mills/y2k.html).
 *
 * @hide
 */
class Timestamp64 private constructor(eraSeconds: Long, fractionBits: Int) {
    /** Returns the number of seconds in the NTP era.  */
    val eraSeconds: Long

    /** Returns the fraction of a second as 32-bit, unsigned fixed-point bits.  */
    val fractionBits: Int

    init {
        require(!(eraSeconds < 0 || eraSeconds > MAX_SECONDS_IN_ERA)) { "Invalid parameters. seconds=" + eraSeconds + ", fraction=" + fractionBits }
        this.eraSeconds = eraSeconds
        this.fractionBits = fractionBits
    }

    override fun toString(): String {
        return String.format("%08x.%08x", this.eraSeconds, this.fractionBits)
    }

    /** Returns the instant represented by this value in the specified NTP era.  */
    fun toInstant(ntpEra: Int): Instant? {
        var secondsSinceEpoch = this.eraSeconds - OFFSET_1900_TO_1970
        secondsSinceEpoch += ntpEra * SECONDS_IN_ERA
        val nanos = fractionBitsToNanos(this.fractionBits)
        return Instant.ofEpochSecond(secondsSinceEpoch, nanos.toLong())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as Timestamp64
        return this.eraSeconds == that.eraSeconds && this.fractionBits == that.fractionBits
    }

    override fun hashCode(): Int {
        return Objects.hash(this.eraSeconds, this.fractionBits)
    }

    /**
     * Randomizes the fraction bits that represent sub-millisecond values. i.e. the randomization
     * won't change the number of milliseconds represented after truncation. This is used to
     * implement the part of the NTP spec that calls for clients with millisecond accuracy clocks
     * to send randomized LSB values rather than zeros.
     */
    fun randomizeSubMillis(random: Random): Timestamp64 {
        val randomizedFractionBits =
            randomizeLowestBits(random, this.fractionBits, SUB_MILLIS_BITS_TO_RANDOMIZE)
        return Timestamp64(this.eraSeconds, randomizedFractionBits)
    }

    companion object {
        val ZERO: Timestamp64 = fromComponents(0, 0)
        const val SUB_MILLIS_BITS_TO_RANDOMIZE: Int = 32 - 10

        // Number of seconds between Jan 1, 1900 and Jan 1, 1970
        // 70 years plus 17 leap days
        const val OFFSET_1900_TO_1970: Long = ((365L * 70L) + 17L) * 24L * 60L * 60L
        const val MAX_SECONDS_IN_ERA: Long = 0xFFFFFFFFL
        const val SECONDS_IN_ERA: Long = MAX_SECONDS_IN_ERA + 1
        const val NANOS_PER_SECOND: Int = 1000000000

        /** Creates a [Timestamp64] from the seconds and fraction components.  */
        fun fromComponents(eraSeconds: Long, fractionBits: Int): Timestamp64 {
            return Timestamp64(eraSeconds, fractionBits)
        }

        /**
         * Converts an [Instant] into a [Timestamp64]. This is lossy: Timestamp64 only
         * contains the number of seconds in a given era, but the era is not stored. Also, sub-second
         * values are not stored precisely.
         */
        fun fromInstant(instant: Instant): Timestamp64 {
            var ntpEraSeconds = instant.epochSecond + OFFSET_1900_TO_1970
            if (ntpEraSeconds < 0) {
                ntpEraSeconds = SECONDS_IN_ERA - (-ntpEraSeconds % SECONDS_IN_ERA)
            }
            ntpEraSeconds %= SECONDS_IN_ERA
            val nanos = instant.nano.toLong()
            val fractionBits = nanosToFractionBits(nanos)
            return Timestamp64(ntpEraSeconds, fractionBits)
        }

        fun fractionBitsToNanos(fractionBits: Int): Int {
            val fractionBitsLong = fractionBits.toLong() and 0xFFFFFFFFL
            return ((fractionBitsLong * NANOS_PER_SECOND) ushr 32).toInt()
        }

        fun nanosToFractionBits(nanos: Long): Int {
            require(nanos <= NANOS_PER_SECOND)
            return ((nanos shl 32) / NANOS_PER_SECOND).toInt()
        }

        /**
         * Randomizes the specified number of LSBs in `value` by using replacement bits from
         * `Random.getNextInt()`.
         */
        fun randomizeLowestBits(random: Random, value: Int, bitsToRandomize: Int): Int {
            require(!(bitsToRandomize < 1 || bitsToRandomize >= Integer.SIZE)) { bitsToRandomize.toString() }
            val upperBitMask = -0x1 shl bitsToRandomize
            val lowerBitMask = upperBitMask.inv()
            val randomValue = random.nextInt()
            return (value and upperBitMask) or (randomValue and lowerBitMask)
        }
    }
}
