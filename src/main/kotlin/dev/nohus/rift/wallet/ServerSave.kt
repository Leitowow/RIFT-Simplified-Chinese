package dev.nohus.rift.wallet

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun Instant.toEveLocalDate(): LocalDate {
    val utc = ZoneId.of("UTC")
    return if (atZone(utc).hour < 11) {
        LocalDate.ofInstant(this, utc).minusDays(1)
    } else {
        LocalDate.ofInstant(this, utc)
    }
}
