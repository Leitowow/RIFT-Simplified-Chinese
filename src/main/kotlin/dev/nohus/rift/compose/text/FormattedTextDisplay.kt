package dev.nohus.rift.compose.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation.Clickable
import androidx.compose.ui.text.LinkAnnotation.Url
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.di.koin
import dev.nohus.rift.game.GameUiController
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull

@Composable
fun FormattedText.toLinkedAnnotatedString(hoveredLink: Link? = null): LinkedAnnotatedString {
    return when (this) {
        is FormattedText.Plain -> LinkedAnnotatedString(AnnotatedString(text), emptyList())
        is FormattedText.Formatted -> toLinkedAnnotatedString(hoveredLink)
        is FormattedText.Compound -> {
            val links = mutableListOf<Pair<SpanIndices, Link>>()
            val annotatedString = buildAnnotatedString {
                texts.forEach {
                    val linkedAnnotatedString = it.toLinkedAnnotatedString(hoveredLink)
                    links += linkedAnnotatedString.links.map { (indices, link) ->
                        SpanIndices(indices.start + length, indices.end + length) to link
                    }
                    append(linkedAnnotatedString.text)
                }
            }
            LinkedAnnotatedString(annotatedString, links)
        }
    }
}

fun FormattedText.toPlainString(): String {
    return when (this) {
        is FormattedText.Plain -> text
        is FormattedText.Formatted -> text.toPlainString()
        is FormattedText.Compound -> texts.joinToString("") {
            it.toPlainString()
        }
    }
}

@Composable
private fun FormattedText.Formatted.toLinkedAnnotatedString(hoveredLink: Link? = null): LinkedAnnotatedString {
    val text = text.toLinkedAnnotatedString()
    val links = text.links.toMutableList()
    val annotatedString = buildAnnotatedString {
        append(text.text)
        spans.forEach { span ->
            val indices = span.target.getIndices(text.text)
            when (span) {
                is Span.Color -> {
                    val color = when (span.color) {
                        FormattedTextColor.Highlighted -> RiftTheme.colors.textHighlighted
                        FormattedTextColor.Primary -> RiftTheme.colors.textPrimary
                        FormattedTextColor.Secondary -> RiftTheme.colors.textSecondary
                        FormattedTextColor.Disabled -> RiftTheme.colors.textDisabled
                    }
                    addStyle(SpanStyle(color = color), indices.start, indices.end)
                }

                is Span.Weight -> {
                    addStyle(SpanStyle(fontWeight = span.fontWeight), indices.start, indices.end)
                }

                is Span.Italics -> {
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), indices.start, indices.end)
                }

                is Span.Underline -> {
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), indices.start, indices.end)
                }

                is Span.CustomColor -> {
                    addStyle(SpanStyle(color = span.color), indices.start, indices.end)
                }

                is Span.Size -> {
                    with(LocalDensity.current) {
                        addStyle(SpanStyle(fontSize = span.size.toSp()), indices.start, indices.end)
                    }
                }

                is Span.CustomLink -> {
                    links += indices to span.link
                    if (span.link == hoveredLink) {
                        when (span.link.style) {
                            LinkStyle.HoverUnderline -> addStyle(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                ),
                                start = indices.start,
                                end = indices.end,
                            )
                            LinkStyle.Default -> addStyle(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = RiftTheme.colors.textLinkHovered,
                                    fontWeight = FontWeight.Bold,
                                ),
                                start = indices.start,
                                end = indices.end,
                            )
                            LinkStyle.External -> addStyle(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = RiftTheme.colors.textExternalLinkHovered,
                                    fontWeight = FontWeight.Bold,
                                ),
                                start = indices.start,
                                end = indices.end,
                            )
                            LinkStyle.Help -> addStyle(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = RiftTheme.colors.textHelpLinkHovered,
                                    fontWeight = FontWeight.Bold,
                                ),
                                start = indices.start,
                                end = indices.end,
                            )
                            LinkStyle.Invite -> addStyle(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = RiftTheme.colors.textInviteLinkHovered,
                                    fontWeight = FontWeight.Bold,
                                ),
                                start = indices.start,
                                end = indices.end,
                            )
                        }
                    } else {
                        when (span.link.style) {
                            LinkStyle.HoverUnderline -> {}
                            LinkStyle.Default -> addStyle(
                                style = SpanStyle(
                                    color = RiftTheme.colors.textLink,
                                    fontWeight = FontWeight.Bold,
                                ),
                                start = indices.start,
                                end = indices.end,
                            )
                            LinkStyle.External -> addStyle(
                                style = SpanStyle(
                                    color = RiftTheme.colors.textExternalLink,
                                    fontWeight = FontWeight.Bold,
                                ),
                                start = indices.start,
                                end = indices.end,
                            )
                            LinkStyle.Help -> addStyle(
                                style = SpanStyle(
                                    color = RiftTheme.colors.textHelpLink,
                                    fontWeight = FontWeight.Bold,
                                ),
                                start = indices.start,
                                end = indices.end,
                            )
                            LinkStyle.Invite -> addStyle(
                                style = SpanStyle(
                                    color = RiftTheme.colors.textInviteLink,
                                    fontWeight = FontWeight.Bold,
                                ),
                                start = indices.start,
                                end = indices.end,
                            )
                        }
                    }
                }
            }
        }
    }
    return LinkedAnnotatedString(
        text = annotatedString,
        links = links,
    )
}

data class LinkedAnnotatedString(
    val text: AnnotatedString,
    val links: List<Pair<SpanIndices, Link>>,
)

data class SpanIndices(
    val start: Int,
    val end: Int,
)

@Composable
private fun SpanTarget.getIndices(text: AnnotatedString): SpanIndices {
    return when (this) {
        is SpanTarget.Range -> SpanIndices(startIndex, endIndex)
        is SpanTarget.Text -> {
            val start = text.indexOf(this.text)
            SpanIndices(start, start + this.text.length)
        }
        is SpanTarget.Formatted -> {
            val string = this.text.toLinkedAnnotatedString().text.text
            val start = text.indexOf(string)
            SpanIndices(start, start + string.length)
        }
        is SpanTarget.Full -> SpanIndices(0, text.length)
    }
}
