package dev.nohus.rift.loglite

class ByteReader(byteArray: ByteArray) {

    private var list = byteArray.toList()
    var offset: Int = 0
        private set
    val size = list.size

    fun skip(bytes: Int) {
        offset += bytes
    }

    fun peek(bytes: Int): List<Byte> {
        return list.subList(offset, offset + bytes)
    }

    fun read(bytes: Int): List<Byte> {
        return peek(bytes).also { offset += bytes }
    }

    fun readString(length: Int): String {
        val bytes = list.subList(offset, offset + length)
        offset += length
        return bytes.takeWhile { it != 0.toByte() }.map { Char(it.toInt()) }.joinToString("")
    }

    fun read8ByteNumber(): Long = readNByteNumber(8)

    fun read4ByteNumber(): Int = readNByteNumber(4).toInt()

    fun read2ByteNumber(): Int = readNByteNumber(2).toInt()

    fun read1ByteNumber(): Int = readNByteNumber(1).toInt()

    private fun readNByteNumber(n: Int): Long {
        return (0 until n).sumOf {
            val shift = ((n - 1) - it) * 8
            list[offset + (n - 1) - it].toUByte().toLong() shl shift
        }.also { offset += n }
    }
}
