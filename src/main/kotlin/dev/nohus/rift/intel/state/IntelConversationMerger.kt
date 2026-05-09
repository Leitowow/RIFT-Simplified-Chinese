package dev.nohus.rift.intel.state

import dev.nohus.rift.intel.ParsedChannelChatMessage
import dev.nohus.rift.intel.state.SystemEntity.Character
import dev.nohus.rift.intel.state.SystemEntity.Ess
import dev.nohus.rift.intel.state.SystemEntity.Gate
import dev.nohus.rift.intel.state.SystemEntity.NoVisual
import dev.nohus.rift.intel.state.SystemEntity.Ship
import dev.nohus.rift.intel.state.SystemEntity.Skyhook
import dev.nohus.rift.intel.state.SystemEntity.UnspecifiedCharacter
import dev.nohus.rift.intel.state.SystemEntity.Wormhole
import dev.nohus.rift.intel.state.UnderstandMessageUseCase.QuestionType.Location
import dev.nohus.rift.intel.state.UnderstandMessageUseCase.QuestionType.Number
import dev.nohus.rift.intel.state.UnderstandMessageUseCase.QuestionType.ShipTypes
import dev.nohus.rift.intel.state.UnderstandMessageUseCase.QuestionType.Status
import dev.nohus.rift.logs.parse.ChatMessageParser
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

@Single
class IntelConversationMerger {

    /**
     * @param message New message
     * @param context Previous messages in this conversation
     *
     * Returns the merged intel understanding of this message and previous messages that are part of
     * the same conversation or question/answer chain
     */
    fun merge(
        message: ParsedChannelChatMessage,
        context: List<ParsedChannelChatMessage>,
    ): IntelUnderstanding {
        val question = getAnsweredQuestion(message, context)
        return if (question != null) {
            val authors = (context.map { it.chatMessage.author } + message.chatMessage.author).distinct()
            val merged = merge(question.understanding, message.understanding, authors)
            val referenced = getReferencedMessage(question, context)
            if (referenced != null) {
                merge(referenced.understanding, merged, authors)
            } else {
                merged
            }
        } else {
            message.understanding
        }
    }

    /**
     * @param existing Previous intel
     * @param new New intel
     */
    private fun merge(existing: IntelUnderstanding, new: IntelUnderstanding, authors: List<String>): IntelUnderstanding {
        return IntelUnderstanding(
            systems = (existing.systems + new.systems).distinct(),
            entities = mergeEntities(existing, new, authors),
            kills = (existing.kills + new.kills).distinct(),
            questions = new.questions,
            movement = new.movement ?: existing.movement,
            reportedNoVisual = new.reportedNoVisual,
            reportedClear = new.reportedClear,
        )
    }

    /**
     * @param existing Previously reported entities
     * @param new Newly reported entities
     */
    private fun mergeEntities(existing: IntelUnderstanding, new: IntelUnderstanding, authors: List<String>): List<SystemEntity> {
        val entities = existing.entities
            .filterNot { it is Character && it.name in authors }
            .toMutableList()
        for (entity in new.entities) {
            when (entity) {
                is Character -> if (entity.name !in authors) entities += entity
                is UnspecifiedCharacter -> {
                    entities.removeAll { it is UnspecifiedCharacter }
                    entities += entity
                }
                is Ship -> {
                    entities.removeAll { it is Ship && it.type.id == entity.type.id }
                    entities += entity
                }
                else -> if (entity !in entities) entities += entity
            }
        }
        return entities
    }

    /**
     * @param message New message
     * @param context Previous messages in this conversation
     * @return The question that this message has answered, or null
     */
    private fun getAnsweredQuestion(
        message: ParsedChannelChatMessage,
        context: List<ParsedChannelChatMessage>,
    ): ParsedChannelChatMessage? {
        val now = Instant.now()
        val questions = context.filter {
            it.parsed.any { it.type is ChatMessageParser.TokenType.Question }
        }

        var matchedQuestion = getReferencedMessage(message, context)
        // This message has referred to the question asker by name
        if (matchedQuestion != null && isAnswer(matchedQuestion.understanding, message.understanding)) return matchedQuestion

        val maxAge = now - Duration.ofSeconds(15)
        matchedQuestion = questions
            .filter { it.chatMessage.timestamp >= maxAge }
            .maxByOrNull { it.chatMessage.timestamp }
        // This message was sent shortly after the question
        if (matchedQuestion != null && isAnswer(matchedQuestion.understanding, message.understanding)) return matchedQuestion

        return null
    }

    /**
     * @param message New message
     * @param context Previous messages in this conversation
     * @return The message that this message references, or null
     */
    private fun getReferencedMessage(
        message: ParsedChannelChatMessage,
        context: List<ParsedChannelChatMessage>,
    ): ParsedChannelChatMessage? {
        val characterNames = message.understanding.entities.filterIsInstance<Character>().map { it.name }
        return context
            .filter { it != message }
            .filter { it.chatMessage.author in characterNames }
            .maxByOrNull { it.chatMessage.timestamp }
    }

    /**
     * Checks if the two intel understandings are compatible system-wise - they don't concern different systems
     */
    private fun isCompatibleSystem(a: IntelUnderstanding, b: IntelUnderstanding): Boolean {
        if (a.systems.isEmpty() || b.systems.isEmpty()) return true
        return a.systems.all { it in b.systems } && b.systems.all { it in a.systems }
    }

    /**
     * Checks if the answer is answering this question
     */
    private fun isAnswer(question: IntelUnderstanding, answer: IntelUnderstanding): Boolean {
        return isCompatibleSystem(question, answer) && question.questions.any { question ->
            when (question.type) {
                Location -> answer.entities.any {
                    it is Gate || it is Wormhole || it is Ess || it is Skyhook || it is NoVisual
                }
                ShipTypes -> answer.entities.any {
                    it is Ship
                }
                Number -> answer.entities.any {
                    it is UnspecifiedCharacter || (it is Ship && it.count > 1)
                }
                Status -> true
            }
        }
    }
}
