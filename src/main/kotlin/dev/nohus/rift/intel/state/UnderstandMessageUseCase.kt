package dev.nohus.rift.intel.state

import dev.nohus.rift.intel.state.SystemEntity.Bubbles
import dev.nohus.rift.intel.state.SystemEntity.Character
import dev.nohus.rift.intel.state.SystemEntity.CombatProbes
import dev.nohus.rift.intel.state.SystemEntity.Ess
import dev.nohus.rift.intel.state.SystemEntity.Gate
import dev.nohus.rift.intel.state.SystemEntity.GateCamp
import dev.nohus.rift.intel.state.SystemEntity.Ship
import dev.nohus.rift.intel.state.SystemEntity.Skyhook
import dev.nohus.rift.intel.state.SystemEntity.Spike
import dev.nohus.rift.intel.state.SystemEntity.UnspecifiedCharacter
import dev.nohus.rift.intel.state.SystemEntity.Wormhole
import dev.nohus.rift.logs.parse.ChatMessageParser
import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType
import dev.nohus.rift.logs.parse.ChatMessageParser.Token
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single
class UnderstandMessageUseCase(
    private val understandRemoteDscanUseCase: UnderstandRemoteDscanUseCase,
    private val characterDetailsRepository: CharacterDetailsRepository,
) {

    data class Kill(
        val name: String,
        val characterId: Int?,
        val target: String,
    )

    data class Question(
        val type: QuestionType,
        val questionText: String,
    )

    enum class QuestionType {
        Location,
        ShipTypes,
        Number,
        Status,
    }

    data class Movement(
        val toSystem: MapSolarSystem,
    )

    suspend operator fun invoke(tokens: List<Token>): IntelUnderstanding = coroutineScope {
        val systems = mutableListOf<MapSolarSystem>()
        val entities = mutableListOf<SystemEntity>()
        val kills = mutableListOf<Kill>()
        val questions = mutableListOf<Question>()
        var movement: Movement? = null
        var reportedNoVisual = false
        var reportedClear = false
        var reportedTotalCount: Int? = null

        val deferredCharacterDetails = tokens
            .asSequence()
            .mapNotNull { it.type }
            .filterIsInstance<TokenType.Character>()
            .map { it.characterId }
            .distinct()
            .map { async { characterDetailsRepository.getCharacterDetails(Originator.ChatLogs, it) } }
            .toList()
        entities += understandRemoteDscanUseCase(tokens)
        val characterDetails = deferredCharacterDetails.awaitAll().filterNotNull().associateBy { it.characterId }

        tokens
            .filter { it.type != null }
            .map map@{ token ->
                val type = token.type ?: return@map
                val text = token.words.joinToString(" ")
                when (type) {
                    is TokenType.Count -> {
                        if (type.isPlus) {
                            entities += UnspecifiedCharacter(type.count)
                        } else if (type.isEquals) {
                            reportedTotalCount = type.count
                        } else {
                            reportedTotalCount = type.count
                        }
                    }
                    is TokenType.Gate -> entities += Gate(type.system, type.isAnsiblex)
                    is TokenType.Movement -> movement = Movement(type.toSystem)
                    is TokenType.Keyword -> {
                        when (type.type) {
                            KeywordType.NoVisual -> reportedNoVisual = true
                            KeywordType.Clear -> reportedClear = true
                            KeywordType.Wormhole -> entities += Wormhole
                            KeywordType.Spike -> entities += Spike
                            KeywordType.Ess -> entities += Ess
                            KeywordType.Skyhook -> entities += Skyhook
                            KeywordType.GateCamp -> entities += GateCamp
                            KeywordType.CombatProbes -> entities += CombatProbes
                            KeywordType.Bubbles -> entities += Bubbles
                        }
                    }

                    is TokenType.Kill -> kills += Kill(type.name, type.characterId, type.target)
                    TokenType.Link -> {}
                    is TokenType.Character -> {
                        characterDetails[type.characterId]?.let {
                            entities += Character(it.name, it.characterId, it)
                        }
                    }
                    is TokenType.Question -> {
                        val questionType = when (type.type) {
                            ChatMessageParser.QuestionType.Location -> QuestionType.Location
                            ChatMessageParser.QuestionType.ShipTypes -> QuestionType.ShipTypes
                            ChatMessageParser.QuestionType.Number -> QuestionType.Number
                            ChatMessageParser.QuestionType.Status -> QuestionType.Status
                        }
                        questions += Question(questionType, text)
                    }

                    is TokenType.Ship -> entities += Ship(type.type, type.count)
                    is TokenType.System -> systems += type.system
                    TokenType.Url -> {}
                }
            }

        reportedTotalCount?.let { total ->
            val additional = (total - entities.filter { it is Character || it is UnspecifiedCharacter }.size).coerceAtLeast(0)
            val unspecified = entities.firstOrNull { it is UnspecifiedCharacter } as? UnspecifiedCharacter
            val newUnspecified = unspecified?.copy(count = unspecified.count + additional) ?: UnspecifiedCharacter(count = additional)
            if (unspecified != null) entities -= unspecified
            entities += newUnspecified
        }

        IntelUnderstanding(
            systems = systems,
            entities = entities,
            kills = kills,
            questions = questions,
            movement = movement,
            reportedNoVisual = reportedNoVisual,
            reportedClear = reportedClear,
        )
    }
}
