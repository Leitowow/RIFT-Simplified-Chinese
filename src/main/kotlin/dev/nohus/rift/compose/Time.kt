package dev.nohus.rift.compose

import dev.nohus.rift.utils.getName
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun getRelativeTime(timestamp: Instant, displayTimezone: ZoneId, now: Instant = Instant.now()): String {
    val duration = Duration.between(timestamp, now)
    return if (duration.toSeconds() < 5) {
        "just now"
    } else if (duration.toSeconds() < 60) {
        "${duration.toSeconds()} seconds ago"
    } else if (duration.toMinutes() < 2) {
        "1 minute ago"
    } else if (duration.toMinutes() < 60) {
        "${duration.toMinutes()} minutes ago"
    } else {
        val time = ZonedDateTime.ofInstant(timestamp, displayTimezone)
        val timezoneName = displayTimezone.getName()
        val formatted = if (duration.toHours() < 12) {
            DateTimeFormatter.ofPattern("HH:mm").format(time)
        } else {
            DateTimeFormatter.ofPattern("d MMM, HH:mm").format(time)
        }
        "$formatted $timezoneName"
    }
}
