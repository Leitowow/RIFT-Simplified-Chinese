package dev.nohus.rift.network.ntp

import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import kotlin.time.TimeSource
import kotlin.time.toJavaDuration

private val logger = KotlinLogging.logger {}

/**
 * Simple, single-use SNTP client class for retrieving network time.
 *
 * Adapted from: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/net/SntpClient.java
 */
class NtpClient {

    private class InvalidServerReplyException(message: String?) : Exception(message)

    data class NtpResult(
        val ntpTime: Instant,
        val ntpTimeReference: Instant,
        val clockOffset: Duration,
    )

    /**
     * Sends an SNTP request to the given host and processes the response.
     *
     * @param host host name of the server.
     * @param port port of the server.
     * @param timeout network timeout in milliseconds. the timeout doesn't include the DNS lookup
     * time, and it applies to each query to the resolved addresses of the NTP server.
     * @return true if the transaction was successful.
     */
    fun requestTime(host: String?, port: Int, timeout: Int): NtpResult? {
        try {
            val addresses: Array<InetAddress> = InetAddress.getAllByName(host)
            for (address in addresses) {
                requestTime(address, port, timeout)?.let { return it }
            }
        } catch (_: UnknownHostException) {
            logger.warn { "Unknown host: $host" }
        }
        logger.warn { "Unable to request time" }
        return null
    }

    private fun requestTime(address: InetAddress, port: Int, timeout: Int): NtpResult? {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeout
            val buffer = ByteArray(NTP_PACKET_SIZE)
            val request = DatagramPacket(buffer, buffer.size, address, port)
            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte
            buffer[0] = (NTP_MODE_CLIENT or (NTP_VERSION shl 3)).toByte()
            // get current time and write it to the request packet
            val requestTime = Instant.now()
            val requestTimestamp: Timestamp64 = Timestamp64.fromInstant(requestTime)
            val randomizedRequestTimestamp: Timestamp64 = requestTimestamp.randomizeSubMillis(Random)
            val requestTicks = TimeSource.Monotonic.markNow()
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, randomizedRequestTimestamp)
            socket.send(request)
            // read the response
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            val responseTicks = TimeSource.Monotonic.markNow()
            val responseTicksInstant = Instant.now()
            val responseTime = requestTime + responseTicks.minus(requestTicks).toJavaDuration()
            val responseTimestamp: Timestamp64 = Timestamp64.fromInstant(responseTime)
            // extract the results
            val leap = ((buffer[0].toInt() shr 6) and 0x3).toByte()
            val mode = (buffer[0].toInt() and 0x7).toByte()
            val stratum = (buffer[1].toInt() and 0xff)
            val referenceTimestamp: Timestamp64 = readTimeStamp(buffer, REFERENCE_TIME_OFFSET)
            val originateTimestamp: Timestamp64 = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET)
            val receiveTimestamp: Timestamp64 = readTimeStamp(buffer, RECEIVE_TIME_OFFSET)
            val transmitTimestamp: Timestamp64 = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET)
            /* Do validation according to RFC */
            checkValidServerReply(
                leap,
                mode,
                stratum,
                transmitTimestamp,
                referenceTimestamp,
                randomizedRequestTimestamp,
                originateTimestamp,
            )
            val clockOffsetDuration = calculateClockOffset(
                requestTimestamp,
                receiveTimestamp,
                transmitTimestamp,
                responseTimestamp,
            )
            return NtpResult(
                responseTime.plus(clockOffsetDuration),
                responseTicksInstant,
                clockOffsetDuration.negated(),
            )
        } catch (e: Exception) {
            logger.error { "Requesting time failed: $e" }
            return null
        } finally {
            socket?.close()
        }
    }

    /**
     * Reads an unsigned 32-bit big endian number from the given offset in the buffer.
     */
    private fun readUnsigned32(buffer: ByteArray, offset: Int): Long {
        var offset = offset
        val i0 = buffer[offset++].toInt() and 0xFF
        val i1 = buffer[offset++].toInt() and 0xFF
        val i2 = buffer[offset++].toInt() and 0xFF
        val i3 = buffer[offset].toInt() and 0xFF
        val bits = (i0 shl 24) or (i1 shl 16) or (i2 shl 8) or i3
        return bits.toLong() and 0xFFFFFFFFL
    }

    /**
     * Reads the NTP time stamp from the given offset in the buffer.
     */
    private fun readTimeStamp(buffer: ByteArray, offset: Int): Timestamp64 {
        val seconds = readUnsigned32(buffer, offset)
        val fractionBits = readUnsigned32(buffer, offset + 4).toInt()
        return Timestamp64.fromComponents(seconds, fractionBits)
    }

    /**
     * Writes the NTP time stamp at the given offset in the buffer.
     */
    private fun writeTimeStamp(buffer: ByteArray, offset: Int, timestamp: Timestamp64) {
        var offset = offset
        val seconds: Long = timestamp.eraSeconds
        // write seconds in big endian format
        buffer[offset++] = (seconds ushr 24).toByte()
        buffer[offset++] = (seconds ushr 16).toByte()
        buffer[offset++] = (seconds ushr 8).toByte()
        buffer[offset++] = (seconds).toByte()
        val fractionBits: Int = timestamp.fractionBits
        // write fraction in big endian format
        buffer[offset++] = (fractionBits ushr 24).toByte()
        buffer[offset++] = (fractionBits ushr 16).toByte()
        buffer[offset++] = (fractionBits ushr 8).toByte()
        buffer[offset] = (fractionBits).toByte()
    }

    companion object {
        private const val REFERENCE_TIME_OFFSET = 16
        private const val ORIGINATE_TIME_OFFSET = 24
        private const val RECEIVE_TIME_OFFSET = 32
        private const val TRANSMIT_TIME_OFFSET = 40
        private const val NTP_PACKET_SIZE = 48
        const val STANDARD_NTP_PORT: Int = 123
        private const val NTP_MODE_CLIENT = 3
        private const val NTP_MODE_SERVER = 4
        private const val NTP_MODE_BROADCAST = 5
        private const val NTP_VERSION = 3
        private const val NTP_LEAP_NOSYNC = 3
        private const val NTP_STRATUM_DEATH = 0
        private const val NTP_STRATUM_MAX = 15

        /** Performs the NTP clock offset calculation.  */
        fun calculateClockOffset(
            clientRequestTimestamp: Timestamp64,
            serverReceiveTimestamp: Timestamp64,
            serverTransmitTimestamp: Timestamp64,
            clientResponseTimestamp: Timestamp64,
        ): Duration {
            // According to RFC4330:
            // t is the system clock offset (the adjustment we are trying to find)
            // t = ((T2 - T1) + (T3 - T4)) / 2
            //
            // Which is:
            // t = (([server]receiveTimestamp - [client]requestTimestamp)
            //       + ([server]transmitTimestamp - [client]responseTimestamp)) / 2
            //
            // See the NTP spec and tests: the numeric types used are deliberate:
            // + Duration64.between() uses 64-bit arithmetic (32-bit for the seconds).
            // + plus() / dividedBy() use Duration, which isn't the double precision floating point
            //   used in NTPv4, but is good enough.
            return Duration64.between(clientRequestTimestamp, serverReceiveTimestamp)
                .plus(Duration64.between(clientResponseTimestamp, serverTransmitTimestamp))
                .dividedBy(2)
        }

        private fun checkValidServerReply(
            leap: Byte,
            mode: Byte,
            stratum: Int,
            transmitTimestamp: Timestamp64,
            referenceTimestamp: Timestamp64,
            randomizedRequestTimestamp: Timestamp64,
            originateTimestamp: Timestamp64?,
        ) {
            if (leap.toInt() == NTP_LEAP_NOSYNC) {
                throw InvalidServerReplyException("unsynchronized server")
            }
            if ((mode.toInt() != NTP_MODE_SERVER) && (mode.toInt() != NTP_MODE_BROADCAST)) {
                throw InvalidServerReplyException("untrusted mode: " + mode)
            }
            if ((stratum == NTP_STRATUM_DEATH) || (stratum > NTP_STRATUM_MAX)) {
                throw InvalidServerReplyException("untrusted stratum: " + stratum)
            }
            if (!randomizedRequestTimestamp.equals(originateTimestamp)) {
                throw InvalidServerReplyException(
                    "originateTimestamp != randomizedRequestTimestamp",
                )
            }
            if (transmitTimestamp.equals(Timestamp64.ZERO)) {
                throw InvalidServerReplyException("zero transmitTimestamp")
            }
            if (referenceTimestamp.equals(Timestamp64.ZERO)) {
                throw InvalidServerReplyException("zero referenceTimestamp")
            }
        }
    }
}
