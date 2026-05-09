package dev.nohus.rift.logs

import dev.nohus.rift.logs.parse.ChatMessage
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

@Single
class MessageDeduplicator {

    private val maxHistory = 200
    private val duplicateMinInterval = Duration.ofSeconds(4)
    private val bucket = mutableMapOf<Int, Instant>()

    fun isDuplicate(message: ChatMessage): Boolean {
        val now = Instant.now()
        cleanUpBucket(now)
        val hash = (message.author to message.message).hashCode()
        val previouslySent = bucket[hash]
        bucket[hash] = now
        if (previouslySent == null) return false
        return previouslySent.isAfter(now - duplicateMinInterval)
    }

    private fun cleanUpBucket(now: Instant) {
        if (bucket.size > maxHistory) {
            val minTimestamp = now - duplicateMinInterval
            bucket.entries.removeAll { it.value.isBefore(minTimestamp) }
        }
    }
}
