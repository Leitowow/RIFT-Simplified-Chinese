package dev.nohus.rift.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.regex.PatternSyntaxException
import kotlin.io.path.createFile

fun URI.openBrowser() {
    try {
        Desktop.getDesktop().browse(this)
    } catch (e: UnsupportedOperationException) {
        Runtime.getRuntime().exec(arrayOf("xdg-open", toString()))
    } catch (e: IOException) {
        // System has no default browser, etc.
    }
}

fun Path.openFileManager() {
    try {
        Desktop.getDesktop().open(toFile())
    } catch (e: UnsupportedOperationException) {
        Runtime.getRuntime().exec(arrayOf("xdg-open", toString()))
    }
}

fun Path.createNewFile() {
    try {
        createFile()
    } catch (ignored: IOException) {}
}

fun String.toURIOrNull(): URI? {
    return try {
        URI(this)
    } catch (e: URISyntaxException) {
        null
    }
}

fun String.toRegexOrNull(): Regex? {
    return try {
        toRegex()
    } catch (e: PatternSyntaxException) {
        null
    }
}

fun String.toRegexOrNull(option: RegexOption): Regex? {
    return try {
        toRegex(option)
    } catch (e: PatternSyntaxException) {
        null
    }
}

operator fun MatchResult.get(key: String): String {
    return groups[key]!!.value
}

fun AnnotatedString.Builder.withColor(color: Color, block: AnnotatedString.Builder.() -> Unit) {
    withStyle(style = SpanStyle(color = color)) {
        block()
    }
}

fun <T> List<T>.toggle(element: T): List<T> {
    return if (element in this) this - element else this + element
}

fun <T> Set<T>.toggle(element: T): Set<T> {
    return if (element in this) this - element else this + element
}

fun Color.desaturate(factor: Float): Color {
    val l = 0.3f * red + 0.6f * green + 0.1f * blue
    val r = red + factor * (l - red)
    val g = green + factor * (l - green)
    val b = blue + factor * (l - blue)
    return Color(r, g, b, alpha)
}

suspend fun <T, R> Collection<T>.mapAsync(transform: suspend CoroutineScope.(T) -> R): List<R> {
    return coroutineScope {
        map { async { transform(it) } }.awaitAll()
    }
}

inline fun <T> Iterable<T>.sumOfDouble(selector: (T) -> Double): Double {
    var sum = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

operator fun <K : Any, V : Any> Cache<K, V>.plusAssign(map: Map<K, V>) {
    map.forEach { (key, value) ->
        put(key, value)
    }
}
