package dev.nohus.rift.logs.parse

import dev.nohus.rift.logs.parse.ChatMessageParser.Token
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class ChooseChatMessageTokenizationUseCase {

    /**
     * Choose the best chat message tokenization out of all provided parsings
     */
    operator fun invoke(tokenizations: Set<List<Token>>): List<Token> {
        var remaining = tokenizations.map(::normalizeTokenization)

        // Take those with the maximum number of killmails
        val groupByKillmails = remaining.groupBy { tokens ->
            tokens.count { it.type is TokenType.Kill }
        }
        val maxKillmails = groupByKillmails.keys.max()
        remaining = groupByKillmails[maxKillmails]!!

        // Take those with the maximum number of recognized links
        val groupByKnownLinks = remaining.groupBy { tokens ->
            tokens.count { it.isLink }
        }
        val maxKnownLinks = groupByKnownLinks.keys.max()
        remaining = groupByKnownLinks[maxKnownLinks]!!

        // Prefer for recognized tokens to cover more text
        val groupBySumOfTokensLength = remaining.groupBy { tokens ->
            tokens.filter { it.type != null && it.type !is TokenType.Question }.sumOf { it.words.joinToString(" ").length }
        }
        remaining = groupBySumOfTokensLength[groupBySumOfTokensLength.keys.max()]!!

        // Prefer for ship names to cover more text (Exequror Navy Issue rather than Exequror Navy)
        val groupBySumOfShipNamesLength = remaining.groupBy { tokens ->
            tokens.filter { it.type is TokenType.Ship }.sumOf { it.words.joinToString(" ").length }
        }
        remaining = groupBySumOfShipNamesLength[groupBySumOfShipNamesLength.keys.max()]!!

        // Prefer for navy ships to end with "navy" rather than start
        val groupByNavyAtEnd = remaining.groupBy { tokens ->
            tokens.filter { it.type is TokenType.Ship }.sumOf { it.words.indexOfFirst { it.lowercase() == "navy" } }
        }
        remaining = groupByNavyAtEnd[groupByNavyAtEnd.keys.max()]!!

        // Prefer last token bo the recognized as something
        if (remaining.any { tokens -> tokens.last().type != null }) {
            remaining = remaining.filter { tokens -> tokens.last().type != null }
        }

        // Take those with the maximum number of keywords
        val groupByKeywords = remaining.groupBy { tokens ->
            tokens.count { it.type is TokenType.Keyword }
        }
        remaining = groupByKeywords[groupByKeywords.keys.max()]!!

        // Prefer all character names to be recognized
        val characterNames = remaining.flatMap { tokens ->
            tokens.filter { it.type is TokenType.Character }.map { it.words }
        }.toSet()
        remaining.filterNot { tokens ->
            tokens.filter { it.type is TokenType.Character }.any { it.words in characterNames }
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer for a Count to exist before a Ship
        if (remaining.any { tokens ->
                tokens.windowed(2).any { (count, ship) ->
                    count.type is TokenType.Count && ship.type is TokenType.Ship
                }
            }
        ) {
            remaining = remaining.filter { tokens ->
                tokens.windowed(2).any { (count, ship) ->
                    count.type is TokenType.Count && ship.type is TokenType.Ship
                }
            }
        }

        // Prefer a single plus count
        remaining.filter { tokens ->
            tokens.count { it.type is TokenType.Count && it.type.isPlus } == 1
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer for a token to not be absorbed by a question
        val recognizedTokens = remaining.flatMap { tokens ->
            tokens.filter { it.type != null }.map { it.words.joinToString(" ") }
        }.toSet()
        remaining.filter { tokens ->
            val questions = tokens.filter { it.type is TokenType.Question }
            questions.none {
                val text = it.words.joinToString(" ")
                val questionToken = it.type as TokenType.Question
                val beforeQuestion = text.substringBefore(questionToken.questionText)
                val afterQuestion = text.substringAfter(questionToken.questionText)
                recognizedTokens.any { recognizedToken -> recognizedToken in beforeQuestion || recognizedToken in afterQuestion }
            }
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer for a token to be recognized if it wouldn't be anything else anyway
        remaining.filter { tokens ->
            val unrecognizedTokens = tokens.filter { it.type == null }
            unrecognizedTokens.none {
                val text = it.words.joinToString(" ")
                recognizedTokens.any { recognizedToken -> recognizedToken in text }
            }
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer for player names to cover more text (longer player names)
        val groupBySumOfNamesLength = remaining.groupBy { tokens ->
            tokens.filter { it.type is TokenType.Character }.sumOf { it.words.joinToString(" ").length }
        }
        remaining = groupBySumOfNamesLength[groupBySumOfNamesLength.keys.max()]!!

        // Prefer for questions to cover more text ("status pls" rather than "status")
        val groupBySumOfQuestionsLength = remaining.groupBy { tokens ->
            tokens.filter { it.type is TokenType.Question }.sumOf { it.words.joinToString(" ").length }
        }
        remaining = groupBySumOfQuestionsLength[groupBySumOfQuestionsLength.keys.max()]!!

        // Prefer for a system to be linked
        remaining.filter { tokens ->
            tokens.any { it.type is TokenType.System }
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer more recognized tokens
        val groupByCountOfRecognizedTokens = remaining.groupBy { tokens ->
            tokens.count { it.type != null }
        }
        remaining = groupByCountOfRecognizedTokens[groupByCountOfRecognizedTokens.keys.max()]!!

        if (logger.isDebugEnabled()) {
            if (remaining.size > 1) {
                logger.error { "\n" + remaining.joinToString("\n") }
            }
        }

        return remaining.first()
    }

    private fun normalizeTokenization(tokenization: List<Token>): List<Token> {
        return tokenization.mapNotNull { token ->
            val words = token.words
                .map { word -> word.normalizeRecognizedTokenWord(token.type) }
                .filterNot { it.isBlank() }
            if (words.isEmpty()) {
                null
            } else {
                token.copy(words = words)
            }
        }
    }

    private fun String.normalizeRecognizedTokenWord(type: TokenType?): String {
        if (type == null || type is TokenType.Kill || type is TokenType.Question || type is TokenType.Url) return this
        return removeSuffix("*")
            .trim { it == '(' || it == ')' || it == ',' || it == '.' }
    }
}
