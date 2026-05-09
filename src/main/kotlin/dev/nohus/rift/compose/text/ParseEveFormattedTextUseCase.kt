package dev.nohus.rift.compose.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import dev.nohus.rift.game.GameUiController
import dev.nohus.rift.utils.get
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class ParseEveFormattedTextUseCase(
    val gameUiController: GameUiController,
) {

    operator fun invoke(text: String): FormattedText {
        val tokens = parse(emptyList(), text)
        return format(tokens)
    }

    /**
     * Returns formatted text from tokens
     */
    private fun format(remaining: List<Token>): FormattedText {
        if (remaining.isEmpty()) return "".toFormattedText()
        when (val nextToken = remaining.first()) {
            is Token.OpenTag -> {
                // Find the corresponding close tag
                var depth = 1
                var index = 1
                while (index <= remaining.lastIndex) {
                    remaining[index].let {
                        if (it is Token.OpenTag && it.tag == nextToken.tag) depth++
                        if (it is Token.CloseTag && it.tag == nextToken.tag) depth--
                        if (depth == 0) {
                            return applyTag(nextToken, remaining.subList(1, index)) + format(remaining.drop(index + 1))
                        }
                        index++
                    }
                }
                // There was no close tag, apply until the end
                return applyTag(nextToken, remaining.drop(1))
            }
            is Token.CloseTag -> {
                // Close tag without a corresponding open tag, ignore it
                return format(remaining.drop(1))
            }
            is Token.Text -> {
                return nextToken.text.toFormattedText() + format(remaining.drop(1))
            }
        }
    }

    /**
     * Returns formatted text from tokens, applying the given tag as the formatting
     */
    private fun applyTag(tag: Token.OpenTag, content: List<Token>): FormattedText {
        return when (tag.tag) {
            "b" -> FormattedText.Formatted(
                text = format(content),
                spans = listOf(Span.Weight(SpanTarget.Full, FontWeight.Bold)),
            )
            "i" -> FormattedText.Formatted(
                text = format(content),
                spans = listOf(Span.Italics(SpanTarget.Full)),
            )
            "u" -> FormattedText.Formatted(
                text = format(content),
                spans = listOf(Span.Underline(SpanTarget.Full)),
            )
            "font" -> {
                val spans = mutableListOf<Span>()
                tag.parameters["size"]?.toIntOrNull()?.let {
                    spans += Span.Size(SpanTarget.Full, it)
                }
                tag.parameters["color"]?.removePrefix("#")?.let {
                    if (it.length == 8) {
                        val alpha = it.take(2).toInt(16)
                        val red = it.substring(2, 4).toInt(16)
                        val green = it.substring(4, 6).toInt(16)
                        val blue = it.substring(6, 8).toInt(16)
                        spans += Span.CustomColor(SpanTarget.Full, Color(red, green, blue, alpha))
                    }
                }
                FormattedText.Formatted(format(content), spans)
            }
            "a" -> {
                tag.parameters["href"].let { href ->
                    val scheme = href?.substringBefore(":")
                    when (scheme) {
                        null -> {
                            format(content)
                        }
                        "showinfo", "opportunity", "localsvc", "helpPointer", "joinChannel" -> {
                            val text = format(content)
                            val style = when (scheme) {
                                "helpPointer" -> LinkStyle.Help
                                "joinChannel" -> LinkStyle.Invite
                                else -> LinkStyle.Default
                            }
                            val span = Span.CustomLink(
                                target = SpanTarget.Full,
                                link = Link(
                                    style = style,
                                    onClick = { gameUiController.pushUrl(href, text.toPlainString()) },
                                    contextMenuItems = null,
                                ),
                            )
                            FormattedText.Formatted(format(content), listOf(span))
                        }
                        else -> {
                            val span = Span.CustomLink(
                                target = SpanTarget.Full,
                                link = Link(
                                    style = LinkStyle.External,
                                    onClick = { href.toURIOrNull()?.openBrowser() },
                                    contextMenuItems = null,
                                ),
                            )
                            FormattedText.Formatted(format(content), listOf(span))
                        }
                    }
                }
            }
            "loc" -> format(content)
            "br" -> "\n".toFormattedText() + format(content)
            else -> {
                logger.warn { "Unimplemented tag: $tag" }
                format(content)
            }
        }
    }

    private val tagName = """^[A-z]+""".toRegex()
    private val parameter = """(?<key>[A-z]+)="(?<value>[^"]*)"""".toRegex()

    sealed interface Token {
        data class Text(val text: String) : Token
        data class OpenTag(val tag: String, val parameters: Map<String, String>) : Token
        data class CloseTag(val tag: String) : Token
    }

    /**
     * Parses an EVE formatted text into a list of [Token]s.
     */
    private tailrec fun parse(current: List<Token>, remaining: String): List<Token> {
        if (remaining.isEmpty()) return current
        when (val nextTagIndex = remaining.indexOf('<')) {
            -1 -> {
                // Text is tag-free, just add it
                return current + Token.Text(remaining)
            }
            0 -> {
                // Text starts with a tag
                val tagEndIndex = remaining.indexOf('>')
                val tag = remaining.take(tagEndIndex + 1).removeSurrounding("<", ">").trim()
                val name = tagName.find(tag.removePrefix("/").trim())?.value
                    ?: return parse(current + Token.Text("<"), remaining.drop(1))
                return if (tag.startsWith('/')) {
                    parse(current + Token.CloseTag(name), remaining.drop(tagEndIndex + 1))
                } else {
                    val parameters = parameter.findAll(tag).map { it["key"] to it["value"] }.toMap()
                    parse(current + Token.OpenTag(name, parameters), remaining.drop(tagEndIndex + 1))
                }
            }
            else -> {
                // Text has a tag later on
                val plain = remaining.take(nextTagIndex)
                return parse(current + Token.Text(plain), remaining.drop(nextTagIndex))
            }
        }
    }
}
