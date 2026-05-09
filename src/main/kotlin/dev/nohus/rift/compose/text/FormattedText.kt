package dev.nohus.rift.compose.text

import androidx.compose.ui.text.font.FontWeight
import dev.nohus.rift.compose.ContextMenuItem
import java.util.UUID

sealed interface FormattedText {
    data class Plain(
        val text: String,
    ) : FormattedText

    data class Formatted(
        val text: FormattedText,
        val spans: List<Span>,
    ) : FormattedText

    data class Compound(
        val texts: List<FormattedText>,
    ) : FormattedText
}

sealed class Span(open val target: SpanTarget) {
    data class Color(override val target: SpanTarget, val color: FormattedTextColor) : Span(target)
    data class CustomColor(override val target: SpanTarget, val color: androidx.compose.ui.graphics.Color) : Span(target)
    data class Weight(override val target: SpanTarget, val fontWeight: FontWeight) : Span(target)
    data class Italics(override val target: SpanTarget) : Span(target)
    data class Underline(override val target: SpanTarget) : Span(target)
    data class Size(override val target: SpanTarget, val size: Int) : Span(target)
    data class CustomLink(override val target: SpanTarget, val link: Link) : Span(target)
}

data class Link(
    val id: UUID = UUID.randomUUID(),
    val style: LinkStyle,
    val onClick: (() -> Unit)? = null,
    val contextMenuItems: List<ContextMenuItem>? = null,
)

enum class LinkStyle {
    HoverUnderline, // Normal text style, with only underline on hover
    Default, // Standard orange EVE link
    External, // External yellow EVE link
    Help, // Help blue EVE link
    Invite, // Invite light blue EVE link
}

sealed interface SpanTarget {
    data class Range(val startIndex: Int, val endIndex: Int) : SpanTarget
    data class Text(val text: String) : SpanTarget
    data class Formatted(val text: FormattedText) : SpanTarget
    object Full : SpanTarget
}

enum class FormattedTextColor {
    Highlighted,
    Primary,
    Secondary,
    Disabled,
}
