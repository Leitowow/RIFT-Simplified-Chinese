package dev.nohus.rift.compose.text

import androidx.compose.ui.text.font.FontWeight

class FormattedTextBuilder {
    private val texts = mutableListOf<FormattedText>()

    fun append(text: String) {
        texts += text.toFormattedText()
    }

    fun append(text: FormattedText) {
        texts += text
    }

    fun appendLine(text: String) {
        append(text)
        appendLine()
    }

    fun appendLine() {
        append("\n")
    }

    fun withColor(color: FormattedTextColor, block: FormattedTextBuilder.() -> Unit) {
        val text = FormattedTextBuilder().apply(block).build()
        texts += FormattedText.Formatted(text, listOf(Span.Color(SpanTarget.Full, color)))
    }

    fun withWeight(fontWeight: FontWeight, block: FormattedTextBuilder.() -> Unit) {
        val text = FormattedTextBuilder().apply(block).build()
        texts += FormattedText.Formatted(text, listOf(Span.Weight(SpanTarget.Full, fontWeight)))
    }

    fun withLink(link: Link, block: FormattedTextBuilder.() -> Unit) {
        val text = FormattedTextBuilder().apply(block).build()
        texts += FormattedText.Formatted(text, listOf(Span.CustomLink(SpanTarget.Full, link)))
    }

    fun build(): FormattedText {
        if (texts.isEmpty()) return FormattedText.Plain("")
        return texts.reduce { acc, text -> acc + text }
    }
}

inline fun buildFormattedText(block: FormattedTextBuilder.() -> Unit): FormattedText {
    return FormattedTextBuilder().apply(block).build()
}

class SpanBuilder(
    val spans: MutableList<Span> = mutableListOf(),
) {
    fun color(content: SpanTarget, color: FormattedTextColor) {
        spans.add(Span.Color(content, color))
    }

    fun weight(content: SpanTarget, fontWeight: FontWeight) {
        spans.add(Span.Weight(content, fontWeight))
    }
}

operator fun FormattedText.plus(other: FormattedText): FormattedText {
    return if (this is FormattedText.Compound) {
        FormattedText.Compound(texts + other)
    } else if (other is FormattedText.Compound) {
        FormattedText.Compound(listOf(this) + other.texts)
    } else {
        FormattedText.Compound(listOf(this, other))
    }
}

fun FormattedText.format(block: SpanBuilder.() -> Unit): FormattedText.Formatted {
    return FormattedText.Formatted(this, SpanBuilder().apply(block).spans)
}

fun String.toFormattedText(): FormattedText {
    return FormattedText.Plain(this)
}

fun String.format(block: SpanBuilder.() -> Unit): FormattedText.Formatted {
    return toFormattedText().format(block)
}
