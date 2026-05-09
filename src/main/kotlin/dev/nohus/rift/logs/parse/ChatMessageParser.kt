package dev.nohus.rift.logs.parse

import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.Location
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.Number
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.ShipTypes
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.Status
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Character
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Count
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Gate
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Keyword
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Kill
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Link
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Movement
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Question
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Ship
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.System
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Url
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.repositories.WordsRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails
import dev.nohus.rift.repositories.character.CharacterStatus
import dev.nohus.rift.repositories.character.CharactersRepository
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single
class ChatMessageParser(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val shipTypesRepository: ShipTypesRepository,
    private val charactersRepository: CharactersRepository,
    private val wordsRepository: WordsRepository,
    private val characterNameValidator: CharacterNameValidator,
) {

    sealed interface TokenType {
        data class System(
            val system: MapSolarSystem,
        ) : TokenType
        data class Character(
            val characterId: Int,
            val details: CharacterDetails? = null,
        ) : TokenType
        data class Ship(
            val type: Type,
            val count: Int = 1,
            val isPlural: Boolean = false,
        ) : TokenType
        data object Link : TokenType
        data class Keyword(
            val type: KeywordType,
        ) : TokenType
        data class Count(
            val count: Int,
            val isPlus: Boolean = false,
            val isEquals: Boolean = false,
        ) : TokenType
        data class Question(
            val type: QuestionType,
            val questionText: String,
        ) : TokenType
        data class Kill(
            val name: String,
            val characterId: Int?,
            val details: CharacterDetails? = null,
            val target: String,
        ) : TokenType
        data object Url : TokenType
        data class Gate(
            val system: MapSolarSystem,
            val isAnsiblex: Boolean = false,
        ) : TokenType
        data class Movement(
            val verb: String,
            val toSystem: MapSolarSystem,
            val isGate: Boolean,
        ) : TokenType
    }

    enum class KeywordType {
        NoVisual,
        Clear,
        Wormhole,
        Spike,
        Ess,
        Skyhook,
        GateCamp,
        CombatProbes,
        Bubbles,
    }

    enum class QuestionType {
        Location,
        ShipTypes,
        Number,
        Status,
    }

    data class MultiTypeToken(
        val words: List<String>,
        val types: List<TokenType>,
    ) {
        override fun toString(): String {
            return if (types.isNotEmpty()) {
                "\"${words.joinToString(" ")}\" $types"
            } else {
                "\"${words.joinToString(" ")}\""
            }
        }
    }

    data class Token(
        val words: List<String>,
        val type: TokenType?,
        val isLink: Boolean,
    ) {
        override fun toString(): String {
            return "\"${words.joinToString(" ")}\" $type${if (isLink) " Link" else ""}"
        }
    }

    data class Parsing(
        val tokens: List<MultiTypeToken>,
        val remainingWords: List<String>,
    )

    companion object {
        private const val MAX_TOKEN_WORDS = 5
        private const val MAX_INCOMPLETE_TOKENIZATIONS = 1000
        private val SHIP_COUNT_NEXT_REGEX = """[1-9]x|[1-9]\*""".toRegex()
        private val SHIP_COUNT_PREV_REGEX = """x[1-9]""".toRegex()
        private val COUNT_PLUS_REGEX = """\+[1-9][0-9]?|[1-9][0-9]?\+|\+ [1-9][0-9]?|[1-9][0-9]? \+""".toRegex()
        private val COUNT_EQUALS_REGEX = """=[1-9][0-9]?|[1-9][0-9]? neuts""".toRegex()
        private val KILL_MAIL_TARGET_REGEX = """\([A-z ]+\)""".toRegex()
        private val URL_REGEX = """https?://.*""".toRegex()
        private val keywords = mapOf(
            "nv" to KeywordType.NoVisual,
            "clr" to KeywordType.Clear,
            "сlr" to KeywordType.Clear, // cyrillic c
            "clr du" to KeywordType.Clear,
            "сlr du" to KeywordType.Clear, // cyrillic c
            "clear" to KeywordType.Clear,
            "сlear" to KeywordType.Clear, // cyrillic c
            "clear du" to KeywordType.Clear,
            "сlear du" to KeywordType.Clear, // cyrillic c
            "wh" to KeywordType.Wormhole,
            "wormhole" to KeywordType.Wormhole,
            "k162" to KeywordType.Wormhole,
            "spike" to KeywordType.Spike,
            "local spike" to KeywordType.Spike,
            "ess" to KeywordType.Ess,
            "ess intrusion" to KeywordType.Ess,
            "ess hacking" to KeywordType.Ess,
            "ess link" to KeywordType.Ess,
            "skyhook" to KeywordType.Skyhook,
            "robbing skyhook" to KeywordType.Skyhook,
            "skyhook theft" to KeywordType.Skyhook,
            "gate camp" to KeywordType.GateCamp,
            "gate camping" to KeywordType.GateCamp,
            "gate camped" to KeywordType.GateCamp,
            "combat probes" to KeywordType.CombatProbes,
            "combat scanners" to KeywordType.CombatProbes,
            "bubble" to KeywordType.Bubbles,
            "bubbles" to KeywordType.Bubbles,
            "bubbled" to KeywordType.Bubbles,
        )
        // TODO: "core sister probes"

        // TODO: Character names that maybe should be keywords or ignored: "Gang", "was", "from", "kicked", "Ansiblex", "ansi", "on the"
        // TODO: "WORMHOLE large", "WORMHOLE XL", "WORMHOLE -> SYSTEM"
        private val questions = mapOf(
            "where is he" to Location,
            "where is he?" to Location,
            "loc?" to Location,
            "loc ?" to Location,
            "location?" to Location,
            "location ?" to Location,
            "shiptypes?" to ShipTypes,
            "shiptypes ?" to ShipTypes,
            "shiptype?" to ShipTypes,
            "shiptype ?" to ShipTypes,
            "ship types?" to ShipTypes,
            "ship types ?" to ShipTypes,
            "ship type?" to ShipTypes,
            "ship type ?" to ShipTypes,
            "ships?" to ShipTypes,
            "ships ?" to ShipTypes,
            "ship?" to ShipTypes,
            "ship ?" to ShipTypes,
            "what ships" to ShipTypes,
            "what ships?" to ShipTypes,
            "how many" to Number,
            "how many?" to Number,
            "status?" to Status,
            "status ?" to Status,
            "status please" to Status,
            "status pls" to Status,
            "status" to Status,
            "sts?" to Status,
            "clr?" to Status,
        )
        private val countQualifiers = mapOf(
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "both" to 2,
        )
    }

    /**
     * @param message Chat message to parse
     * @param regionsHint Regions the intel channel this message is from is for
     * @return All possible tokenizations of the message
     */
    suspend fun parse(
        message: String,
        regionsHint: List<String>,
    ): Set<List<Token>> = coroutineScope {
        val words = message.split(" ")
        val completeParsings = mutableListOf<List<MultiTypeToken>>()
        val incompleteParsings = LinkedHashSet<Parsing>()
        incompleteParsings += Parsing(tokens = emptyList(), remainingWords = words)

        val possibleCharacterNames = getPossibleCharacterNames(words)
        val characterNamesStatus = charactersRepository.getCharacterNamesStatus(Originator.ChatLogs, possibleCharacterNames)

        while (incompleteParsings.isNotEmpty()) {
            if (incompleteParsings.size > MAX_INCOMPLETE_TOKENIZATIONS) {
                // This text has an unusual number of branching tokenizations, do not continue parsing
                return@coroutineScope setOf(listOf(Token(words, type = null, isLink = false)))
            }

            val parsing = incompleteParsings.first()
            incompleteParsings.remove(parsing)

            if (parsing.remainingWords.first().isBlank()) {
                val newTokens = if (parsing.tokens.isNotEmpty()) {
                    // A token cannot start with a space, the previous token is a link
                    val lastToken = parsing.tokens.last()
                    parsing.tokens.dropLast(1) + lastToken.copy(types = lastToken.types.filterNot { it is Link } + Link) + MultiTypeToken(listOf(""), types = listOf())
                } else {
                    // This is a space at the beginning of the message
                    listOf(MultiTypeToken(listOf(""), types = listOf()))
                }

                val remainingWords = parsing.remainingWords.drop(1)
                if (remainingWords.isEmpty()) {
                    completeParsings += newTokens
                } else {
                    incompleteParsings += Parsing(tokens = newTokens, remainingWords = remainingWords)
                }
                continue
            }

            for (wordsToConsume in 1..parsing.remainingWords.size.coerceAtMost(MAX_TOKEN_WORDS)) {
                val token = parsing.remainingWords.take(wordsToConsume)

                // A token cannot contain a space except at the end
                if (token.any { it.isBlank() }) break

                val types = getPossibleTokenTypes(token, characterNamesStatus, regionsHint)
                val newToken = MultiTypeToken(token, types = types)
                var tokens = parsing.tokens + newToken
                val isAtEnd = wordsToConsume == parsing.remainingWords.size

                tokens = findKillMail(tokens, characterNamesStatus)
                tokens = findGates(tokens)
                tokens = findMovement(tokens, isAtEnd)
                tokens = findShipCounts(tokens)
                tokens = findQuestions(tokens)
                tokens = findMergeablePlainText(tokens)

                if (isAtEnd) {
                    completeParsings += tokens
                } else {
                    val remainingWords = parsing.remainingWords.drop(wordsToConsume)
                    incompleteParsings += Parsing(tokens = tokens, remainingWords = remainingWords)
                }
            }
        }

        completeParsings
            .asSequence()
            .map { filterCharactersUntilDone(it, characterNamesStatus) }
            .map { filterMultiTypes(it, regionsHint) }
            .map(::mergePlainTextTokens)
            .toSet()
    }

    private fun getPossibleCharacterNames(words: List<String>): List<String> {
        return words.indices
            .asSequence()
            .map { words.drop(it) }
            .flatMap { listOf(it.take(1), it.take(2), it.take(3)).map { it.joinToString(" ") } }
            .filterNot { it.startsWith(" ") || it.endsWith(" ") || it.contains("  ") }
            .filter(characterNameValidator::isValid)
            .distinct()
            .toList()
    }

    private fun findKillMail(tokens: List<MultiTypeToken>, characterNamesStatus: Map<String, CharacterStatus>): List<MultiTypeToken> {
        return if (tokens.size >= 3) {
            val threeTokens = tokens.takeLast(3)
            val (t1, t2, t3) = threeTokens
            val killTranslations = listOf("Kill:", "Abschuss:")
            if (t1.words.singleOrNull() in killTranslations && t3.words.joinToString(" ").matches(KILL_MAIL_TARGET_REGEX)) {
                val player = t2.words.joinToString(" ")
                val target = t3.words.joinToString(" ").removePrefix("(").removeSuffix(")")
                val words = threeTokens.flatMap { it.words }
                val characterId = (characterNamesStatus[player] as? CharacterStatus.Exists)?.characterId
                tokens.dropLast(3) + MultiTypeToken(words, types = listOf(Kill(player, characterId, null, target)))
            } else {
                tokens
            }
        } else {
            tokens
        }
    }

    private fun findGates(tokens: List<MultiTypeToken>): List<MultiTypeToken> {
        if (tokens.size >= 2) {
            val lastTokens = tokens.takeLast(2)
            val system = lastTokens.mapNotNull { it.types.filterIsInstance<System>().firstOrNull() }.singleOrNull()?.system
            val other = lastTokens.singleOrNull { it.types.none { it is System || it is Keyword } }?.words?.singleOrNull()?.lowercase()
            if (system != null && other != null) {
                val isGate = other == "gate"
                val isAnsiblex = other in listOf("ansiblex", "ansi")
                if (isGate || isAnsiblex) {
                    return tokens.dropLast(2) + MultiTypeToken(lastTokens.flatMap { it.words }, types = listOf(Gate(system, isAnsiblex)))
                }
            }
        }
        return tokens
    }

    private fun findMovement(tokens: List<MultiTypeToken>, isAtEnd: Boolean): List<MultiTypeToken> {
        val keywords = listOf("going", "jumped", "jumping")
        if (tokens.size >= 2) {
            val lastTokens = tokens.takeLast(2)
            if (lastTokens[1].types.filterIsInstance<Gate>().isNotEmpty()) {
                val gate = lastTokens[1].types.filterIsInstance<Gate>().first()
                val before = lastTokens[0].words.joinToString(" ")
                if (before.lowercase() in keywords) {
                    return tokens.dropLast(2) + MultiTypeToken(lastTokens.flatMap { it.words }, types = listOf(Movement(before, gate.system, isGate = true)))
                }
            }
        }
        if (isAtEnd) { // Only consume the last 2 tokens as a movement if they are the end otherwise the next token could've been a gate instead
            if (tokens.size >= 2) {
                val lastTokens = tokens.takeLast(2)
                if (lastTokens[1].types.filterIsInstance<System>().isNotEmpty()) {
                    val system = lastTokens[1].types.filterIsInstance<System>().first().system
                    val before = lastTokens[0].words.joinToString(" ")
                    if (before.lowercase() in keywords) {
                        return tokens.dropLast(2) + MultiTypeToken(lastTokens.flatMap { it.words }, types = listOf(Movement(before, system, isGate = false)))
                    }
                }
            }
        } else {
            if (tokens.size >= 3) {
                val lastTokens = tokens.takeLast(3)
                if (lastTokens[1].types.filterIsInstance<System>().isNotEmpty()) {
                    val system = lastTokens[1].types.filterIsInstance<System>().first().system
                    val before = lastTokens[0].words.joinToString(" ")
                    if (before.lowercase() in keywords) {
                        return tokens.dropLast(3) + MultiTypeToken(lastTokens.flatMap { it.words }, types = listOf(Movement(before, system, isGate = false))) + lastTokens.last()
                    }
                }
            }
        }
        return tokens
    }

    private fun findShipCounts(tokens: List<MultiTypeToken>): List<MultiTypeToken> {
        if (tokens.size >= 2) {
            run {
                val (count, ship) = tokens.takeLast(2)
                val isNextTokenShip = ship.types.any { it is Ship }
                val word = count.words.singleOrNull()
                if (isNextTokenShip && word != null) {
                    val number = when {
                        word.toIntOrNull()?.let { it < 10 } == true -> {
                            word.toInt() // "2 loki"
                        }
                        word in countQualifiers.keys -> {
                            countQualifiers.getValue(word) // "two loki"
                        }
                        word.matches(SHIP_COUNT_NEXT_REGEX) -> {
                            word.filter { it.isDigit() }.toInt() // "2x loki", "2* loki"
                        }
                        else -> null
                    }
                    if (number != null && number > 0) {
                        val shipWithCount = ship.copy(
                            words = count.words + ship.words,
                            types = ship.types.map { type -> if (type is Ship) type.copy(count = number) else type },
                        )
                        return tokens.dropLast(2) + shipWithCount
                    }
                }
            }
            run {
                val (ship, count) = tokens.takeLast(2)
                val isPrevTokenShip = ship.types.any { it is Ship }
                val word = count.words.singleOrNull()
                if (isPrevTokenShip && word != null) {
                    val number = if (word.matches(SHIP_COUNT_PREV_REGEX)) {
                        word.filter { it.isDigit() }.toInt() // "loki x2"
                    } else {
                        null
                    }
                    if (number != null) {
                        val shipWithCount = ship.copy(
                            words = ship.words + count.words,
                            types = ship.types.map { type -> if (type is Ship) type.copy(count = number) else type },
                        )
                        return tokens.dropLast(2) + shipWithCount
                    }
                }
            }
        }
        val token = tokens.last()
        val text = token.words.joinToString(" ")
        return if (text.matches(SHIP_COUNT_PREV_REGEX) || text.matches(SHIP_COUNT_NEXT_REGEX)) {
            val count = text.filter { it.isDigit() }.toInt()
            tokens.dropLast(1) + token.copy(types = listOf(Count(count))) // "x2"
        } else if (text.matches(COUNT_PLUS_REGEX)) {
            val count = text.filter { it.isDigit() }.toInt()
            tokens.dropLast(1) + token.copy(types = listOf(Count(count, isPlus = true))) // "+2", "2+", "+ 2", "2 +"
        } else if (text.lowercase().matches(COUNT_EQUALS_REGEX)) {
            val count = text.filter { it.isDigit() }.toInt()
            tokens.dropLast(1) + token.copy(types = listOf(Count(count, isEquals = true))) // "=15"
        } else {
            tokens
        }
    }

    private fun findQuestions(tokens: List<MultiTypeToken>): List<MultiTypeToken> {
        val token = tokens.last()
        val originalText = token.words.joinToString(" ")
        val text = originalText.lowercase()
        val question = questions.entries.find { (question, _) -> question == text }
        return if (question != null) {
            val originalQuestionTextIndex = text.indexOf(question.key)
            val originalQuestionText = originalText.substring(originalQuestionTextIndex, originalQuestionTextIndex + question.key.length)
            tokens.dropLast(1) + token.copy(types = listOf(Question(type = question.value, questionText = originalQuestionText)))
        } else {
            tokens
        }
    }

    private fun findMergeablePlainText(tokens: List<MultiTypeToken>): List<MultiTypeToken> {
        return if (tokens.size >= 4) {
            // Last two are potentially going to be used for a 3-token find (e.g. killmail)
            mergePlainTextMultiTypeTokens(tokens.dropLast(2)) + tokens.takeLast(2)
        } else {
            tokens
        }
    }

    private fun filterCharactersUntilDone(parsing: List<MultiTypeToken>, characterNamesStatus: Map<String, CharacterStatus>): List<MultiTypeToken> {
        var previous: List<MultiTypeToken>
        var new = parsing
        do {
            previous = new
            new = filterCharacters(previous, characterNamesStatus)
        } while (new != previous)
        return new
    }

    private fun filterCharacters(parsing: List<MultiTypeToken>, characterNamesStatus: Map<String, CharacterStatus>): List<MultiTypeToken> {
        return buildList {
            for ((index, token) in parsing.withIndex()) {
                if (token.types.any { it is Character }) {
                    val fullText = token.words.joinToString(" ")
                    val status = characterNamesStatus.getValue(fullText)
                    if (status is CharacterStatus.Dormant) {
                        // Character is dormant, ignore
                        add(token.copy(types = token.types.filterNot { it is Character }))
                        continue
                    }
                    if (status !is CharacterStatus.Active) {
                        val isEnglish = token.words.all { wordsRepository.isWord(it) }
                        val isTypeName = wordsRepository.isTypeName(fullText)
                        val isLowercase = token.words.all { word -> word.all { it.isLowerCase() } }
                        if (isEnglish) {
                            if (isLowercase) {
                                val before = parsing.getOrNull(index - 1)?.takeIf { it.types.all { it is Question || it is Link } }?.words?.lastOrNull()
                                val after = parsing.getOrNull(index + 1)?.takeIf { it.types.all { it is Question || it is Link } }?.words?.firstOrNull()
                                val surroundingLowercaseWords = listOfNotNull(before, after)
                                    .filter { word ->
                                        word.all { it.isLowerCase() || it in listOf('\'', '?') } || word in listOf("I")
                                    }
                                if (surroundingLowercaseWords.isNotEmpty()) {
                                    // Lowercase English words character name touches a lowercase plaintext, ignore
                                    add(token.copy(types = token.types.filterNot { it is Character }))
                                    continue
                                }
                            } else {
                                val isOnlyFirstLetterUppercase = token.words.joinToString("").drop(1).all { it.isLowerCase() }
                                if (isOnlyFirstLetterUppercase) {
                                    val nextWord = parsing.getOrNull(index + 1)?.takeIf { it.types.all { it is Question || it is Link } }?.words?.firstOrNull()
                                    val nextLowercaseWord = nextWord?.all { it.isLowerCase() || it in listOf('\'', '?') }
                                    if (nextLowercaseWord == true) {
                                        // English words character name with first capital letter, with next word being lowercase plaintext, ignore
                                        add(token.copy(types = token.types.filterNot { it is Character }))
                                        continue
                                    }
                                }
                            }
                        }
                        if (isTypeName) {
                            // Character name is a type name
                            add(token.copy(types = token.types.filterNot { it is Character }))
                            continue
                        }
                    }
                }
                add(token)
            }
        }
    }

    private fun filterMultiTypes(parsing: List<MultiTypeToken>, regionsHint: List<String>): List<Token> {
        return buildList {
            for (token in parsing) {
                val isLink = token.types.any { it is Link }
                if (token.types.any { it is Ship } && token.types.any { it is System }) {
                    // Token is both a system and a ship (Naga)
                    val otherTokens = parsing - token
                    val hasOtherSystem = otherTokens.any { it.types.any { it is System } }
                    if (hasOtherSystem) {
                        add(Token(token.words, token.types.filterIsInstance<Ship>().first(), isLink))
                    } else {
                        add(Token(token.words, token.types.filterIsInstance<System>().first(), isLink))
                    }
                    continue
                }
                if (token.types.any { it is System } && token.types.any { it is Character }) {
                    // Token is both a system and a player
                    val inRegionSystem = token.types.filterIsInstance<System>()
                        .firstOrNull { solarSystemsRepository.getRegion(it.system.regionId)?.name in regionsHint }
                    if (inRegionSystem != null) {
                        // If the system is in this region, choose the system
                        add(Token(token.words, type = inRegionSystem, isLink))
                    } else {
                        // Otherwise choose the player
                        add(Token(token.words, type = token.types.filterIsInstance<Character>().first(), isLink))
                    }
                    continue
                }
                val type = token.types.firstOrNull { it !is Link }
                add(
                    Token(
                        words = token.words,
                        type = type,
                        isLink = isLink && type != null,
                    ),
                )
            }
        }
    }

    private fun mergePlainTextMultiTypeTokens(parsing: List<MultiTypeToken>): List<MultiTypeToken> {
        return buildList<MultiTypeToken> {
            for (token in parsing) {
                val new = if (token.types.isEmpty() && lastOrNull()?.types?.isEmpty() == true) {
                    val merged = MultiTypeToken(last().words + token.words, types = emptyList())
                    removeLast()
                    merged
                } else {
                    token
                }
                add(new)
            }
        }
    }

    private fun mergePlainTextTokens(parsing: List<Token>): List<Token> {
        return buildList<Token> {
            for (token in parsing) {
                val new = if (token.type == null && lastOrNull().let { it != null && it.type == null }) {
                    val merged = Token(last().words + token.words, type = null, isLink = false)
                    removeLast()
                    merged
                } else {
                    token
                }
                add(new)
            }
        }
    }

    private fun getPossibleTokenTypes(
        words: List<String>,
        characterNamesStatus: Map<String, CharacterStatus>,
        regionsHint: List<String>,
    ): List<TokenType> {
        val text = words.joinToString(" ").removeSuffix("*") // Links sometimes end with *
        val cleanedText = text.replace(Regex("[(),.]"), "")

        return buildList {
            if (words.last().endsWith("*")) add(Link)

            if (words.singleOrNull()?.matches(URL_REGEX) == true) {
                add(Url)
                return@buildList
            }

            val keywordType = keywords[cleanedText.lowercase()]
            if (keywordType != null) {
                add(Keyword(type = keywordType))
                return@buildList
            }

            val system = solarSystemsRepository.getFuzzySystem(text, regionsHint)
            if (system != null) add(System(system))

            var ship = shipTypesRepository.getFuzzyShip(cleanedText)
            if (ship != null) {
                add(Ship(ship, isPlural = false))
            } else if (text.last() == 's') {
                ship = shipTypesRepository.getFuzzyShip(cleanedText.dropLast(1))
                if (ship != null) {
                    add(Ship(ship, isPlural = true))
                }
            }

            if (ship == null) {
                // Ship names are assumed to be ships, only check characters if not a ship
                (characterNamesStatus[text] as? CharacterStatus.Exists)?.let {
                    add(Character(it.characterId))
                }
            }
        }
    }
}
