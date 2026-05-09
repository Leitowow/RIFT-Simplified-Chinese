package dev.nohus.rift.utils

import org.apache.commons.math3.util.ArithmeticUtils.pow
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val eveTime = ZoneId.of("UTC")
private val formatterWithoutDecimals = NumberFormat.getInstance(Locale.ENGLISH).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 0
}
private val formatterWithDecimals = NumberFormat.getInstance(Locale.ENGLISH).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}
private val dateFormatter = DateTimeFormatter
    .ISO_LOCAL_DATE
    .withLocale(Locale.ENGLISH)
private val dateFormatterWithDateTime = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss", Locale.ENGLISH)
private val dateFormatterWithDateTime2 = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.ENGLISH)
private val dateFormatterWithTime = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH)

fun formatIskCompact(number: Long): String = "${formatNumberCompact(number)} ISK"
fun formatIskCompact(number: Double): String = "${formatNumberCompact(number)} ISK"
fun formatIsk(number: Long, withCents: Boolean): String = "${formatNumber(number, withCents)} ISK"
fun formatIsk(number: Double, withCents: Boolean): String = "${formatNumber(number, withCents)} ISK"
fun formatIskReadable(number: Long): String = "${formatNumberReadable(number.toDouble(), isCompact = false)} ISK"
fun formatIskReadable(number: Double): String = "${formatNumberReadable(number, isCompact = false)} ISK"

fun formatNumberCompact(number: Int): String = formatNumberReadable(number.toDouble(), isCompact = true)
fun formatNumberCompact(number: Long): String = formatNumberReadable(number.toDouble(), isCompact = true)
fun formatNumberCompact(number: Double): String = formatNumberReadable(number, isCompact = true)
fun formatNumber(number: Int, withDecimals: Boolean = false): String = getFormatter(withDecimals).format(number)
fun formatNumber(number: Long, withDecimals: Boolean = false): String = getFormatter(withDecimals).format(number)
fun formatNumber(number: Double, withDecimals: Boolean = false): String = getFormatter(withDecimals).format(number)

private fun getFormatter(withDecimals: Boolean) = if (withDecimals) formatterWithDecimals else formatterWithoutDecimals

private fun formatNumberReadable(number: Double, significantDigits: Int = 3, isCompact: Boolean): String {
    var rounded = roundToSignificant(number, significantDigits)
    val logThousand = log1k(rounded).coerceIn(0, 4)
    rounded /= 1000.0.pow(logThousand.toDouble())
    var decimalPlaces = 0
    for (i in 0 until (significantDigits - 1)) {
        if ((rounded.absoluteValue * pow(10, significantDigits - 1 - i)).roundToInt() % 10 > 0) {
            decimalPlaces = significantDigits - 1 - i
            break
        }
    }
    val formatted = NumberFormat.getInstance(Locale.ENGLISH).apply {
        minimumFractionDigits = decimalPlaces
        maximumFractionDigits = decimalPlaces
    }.format(rounded)
    val suffix = if (isCompact) {
        getNumberSuffixCompact(logThousand)
    } else {
        getNumberSuffix(logThousand)
    }
    return if (suffix != null) {
        "$formatted$suffix"
    } else {
        formatted
    }
}

private fun getNumberSuffix(logThousand: Int) = when (logThousand) {
    0 -> null
    1 -> " thousand"
    2 -> " million"
    3 -> " billion"
    else -> " trillion"
}

private fun getNumberSuffixCompact(logThousand: Int) = when (logThousand) {
    0 -> null
    1 -> "k"
    2 -> "M"
    3 -> "B"
    else -> "T"
}

/**
 * Rounds a number to a certain number of most significant digits
 */
fun roundToSignificant(number: Double, significantDigits: Int): Double {
    var numberOfDigits = 0
    if (!isClose(number, 0.0)) {
        numberOfDigits = significantDigits - 1 - floor(log10(abs(number))).toInt()
    }
    val factor = 10.0.pow(numberOfDigits.toDouble())
    return (number * factor).roundToInt() / factor
}

private fun isClose(a: Double, b: Double, epsilon: Double = 1e-09): Boolean = abs(a - b) < epsilon

private fun log1k(number: Double): Int {
    if (isClose(number, 0.0)) return 0
    return floor(log(abs(number), 1000.0)).toInt()
}

fun formatDurationNumeric(duration: Duration): String {
    return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart())
}
fun formatDurationCompact(duration: Duration): String {
    return buildString {
        if (duration.toDays() >= 1) append("${duration.toDays()}d ")
        if (duration.toHoursPart() >= 1) append("${duration.toHoursPart()}h ")
        if (duration.toMinutesPart() >= 1) append("${duration.toMinutesPart()}m")
        if (duration.toMinutes() == 0L) append("${duration.toSeconds()}s")
    }.trimEnd()
}
fun formatDurationLong(duration: Duration): String {
    return buildList {
        if (duration.toDays() >= 1) add("${duration.toDays()} day${duration.toDays().plural}")
        if (duration.toHoursPart() >= 1) add("${duration.toHoursPart()} hour${duration.toHoursPart().plural}")
        if (duration.toMinutesPart() >= 1) add("${duration.toMinutesPart()} minute${duration.toMinutesPart().plural}")
    }.take(2).joinToString(" and ")
}

fun formatDateTime(instant: Instant, timezone: ZoneId = eveTime): String {
    val time: ZonedDateTime = ZonedDateTime.ofInstant(instant, timezone)
    val previousMidnight = ZonedDateTime.of(LocalDate.now().atTime(0, 0), timezone)
    val nextMidnight = previousMidnight.plusDays(1)
    val isToday = time.isAfter(previousMidnight) && time.isBefore(nextMidnight)
    val formatter = if (isToday) dateFormatterWithTime else dateFormatterWithDateTime
    return formatter.format(ZonedDateTime.ofInstant(instant, timezone)) + " ${timezone.getName()}"
}

fun formatDateTime2(instant: Instant): String = dateFormatterWithDateTime2.format(ZonedDateTime.ofInstant(instant, eveTime))

fun formatDate(instant: Instant): String {
    return dateFormatter.format(ZonedDateTime.ofInstant(instant, eveTime))
}

fun formatDate(date: LocalDate): String {
    return dateFormatter.format(date)
}

fun formatTime(instant: Instant, timezone: ZoneId): String {
    val time = ZonedDateTime.ofInstant(instant, timezone)
    return dateFormatterWithTime.format(time)
}

fun formatDuration(duration: Duration): String {
    return buildString {
        duration.toDays().takeIf { it > 0 }?.let { append("${it}d ") }
        duration.toHoursPart().takeIf { it > 0 }?.let { append("${it}h ") }
        duration.toMinutesPart().takeIf { it > 0 }?.let { append("${it}m ") }
        append("${duration.toSecondsPart()}s")
    }
}

val Int.plural: String get() {
    return if (this != 1) "s" else ""
}

val Int.invertedPlural: String get() {
    return if (this == 1) "s" else ""
}

val Long.plural: String get() {
    return if (this != 1L) "s" else ""
}

val String.article: String get() {
    return when {
        isEmpty() -> ""
        lowercase().firstOrNull { it.isLetter() } in listOf('e', 'u', 'i', 'o', 'a') -> "an"
        else -> "a"
    }
}

fun ZoneId.getName(): String {
    return if (this == ZoneId.of("UTC")) "EVE" else getDisplayName(TextStyle.SHORT, Locale.US)
}
