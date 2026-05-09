package dev.nohus.rift.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import org.nibor.autolink.LinkExtractor
import org.nibor.autolink.LinkSpan
import org.nibor.autolink.LinkType

fun annotateLinks(
    text: String,
    linkStyle: SpanStyle,
): AnnotatedString {
    val linkExtractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL, LinkType.WWW)).build()
    return buildAnnotatedString {
        linkExtractor.extractSpans(text).map { span ->
            val content = text.drop(span.beginIndex).take(span.endIndex - span.beginIndex)
            if (span is LinkSpan) {
                withLink(
                    LinkAnnotation.Url(
                        url = content,
                        styles = TextLinkStyles(
                            style = linkStyle,
                            hoveredStyle = linkStyle.copy(textDecoration = TextDecoration.Underline),
                        ),
                        linkInteractionListener = {
                            content.toURIOrNull()?.openBrowser()
                        },
                    ),
                ) {
                    append(content)
                }
            } else {
                append(content)
            }
        }
    }
}
