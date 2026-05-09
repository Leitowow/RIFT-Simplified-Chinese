package dev.nohus.rift.chat

import dev.nohus.rift.compose.EntityInteractionProvider
import dev.nohus.rift.compose.EntityInteractionProvider.Interaction
import dev.nohus.rift.compose.text.FormattedText
import dev.nohus.rift.compose.text.Link
import dev.nohus.rift.compose.text.LinkStyle
import dev.nohus.rift.compose.text.buildFormattedText
import dev.nohus.rift.logs.parse.ChatMessageParser
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType
import dev.nohus.rift.logs.parse.ChooseChatMessageTokenizationUseCase
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import org.koin.core.annotation.Single

@Single
class LinkMessageUseCase(
    private val chatMessageParser: ChatMessageParser,
    private val chooseChatMessageTokenizationUseCase: ChooseChatMessageTokenizationUseCase,
    private val entityInteractionProvider: EntityInteractionProvider,
) {

    suspend operator fun invoke(message: String): FormattedText {
        val parsings = chatMessageParser.parse(message, emptyList())
        val parsing = chooseChatMessageTokenizationUseCase(parsings)
        return buildFormattedText {
            parsing.forEachIndexed { index, token ->
                if (index > 0) append(" ")
                val text = token.words.joinToString(" ")
                val link = when (token.type) {
                    is TokenType.Character -> {
                        entityInteractionProvider.getCharacter(token.type.characterId).toLink()
                    }
                    is TokenType.Count -> null
                    is TokenType.Gate -> {
                        entityInteractionProvider.getLocation(token.type.system.id).toLink()
                    }
                    is TokenType.Keyword -> null
                    is TokenType.Kill -> {
                        token.type.characterId?.let { entityInteractionProvider.getCharacter(it) }?.toLink()
                    }
                    TokenType.Link -> null
                    is TokenType.Movement -> {
                        entityInteractionProvider.getLocation(token.type.toSystem.id).toLink()
                    }
                    is TokenType.Question -> null
                    is TokenType.Ship -> {
                        entityInteractionProvider.getShip(token.type.type).toLink()
                    }
                    is TokenType.System -> {
                        entityInteractionProvider.getLocation(token.type.system.id).toLink()
                    }
                    TokenType.Url -> {
                        Link(
                            style = LinkStyle.External,
                            onClick = { text.toURIOrNull()?.openBrowser() },
                            contextMenuItems = null,
                        )
                    }
                    null -> null
                }

                if (link != null) {
                    withLink(link) {
                        append(text)
                    }
                } else {
                    append(text)
                }
            }
        }
    }

    private fun Interaction.toLink() = Link(
        style = LinkStyle.Default,
        onClick = this.onClick,
        contextMenuItems = this.contextMenuItems,
    )
}
