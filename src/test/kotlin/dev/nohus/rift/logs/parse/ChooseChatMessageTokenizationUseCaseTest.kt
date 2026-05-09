package dev.nohus.rift.logs.parse

import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType.Clear
import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType.GateCamp
import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType.NoVisual
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.Location
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.ShipTypes
import dev.nohus.rift.logs.parse.ChatMessageParser.Token
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Character
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Count
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Keyword
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Kill
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Question
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Ship
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.System
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Url
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.repositories.WordsRepository
import dev.nohus.rift.repositories.character.CharacterStatus
import dev.nohus.rift.repositories.character.CharactersRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

class ChooseChatMessageTokenizationUseCaseTest : FreeSpec({

    isolationMode = IsolationMode.InstancePerTest

    val mockSolarSystemsRepository: SolarSystemsRepository = mockk()
    val mockShipTypesRepository: ShipTypesRepository = mockk()
    val mockCharactersRepository: CharactersRepository = mockk()
    val mockWordsRepository: WordsRepository = mockk()
    val characterNameValidator = CharacterNameValidator()
    val target = ChooseChatMessageTokenizationUseCase()
    val parser = ChatMessageParser(
        mockSolarSystemsRepository,
        mockShipTypesRepository,
        mockCharactersRepository,
        mockWordsRepository,
        characterNameValidator,
    )
    every { mockSolarSystemsRepository.getFuzzySystem(any(), any()) } returns null
    every { mockShipTypesRepository.getFuzzyShip(any()) } returns null
    coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns emptyMap()
    every { mockWordsRepository.isWord(any()) } returns false
    every { mockWordsRepository.isTypeName(any()) } returns false

    "system link, player link, player" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("D-W7F0", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Ishani Kalki", "Shiva Callipso").existing()
        val tokenizations = parser.parse("D-W7F0  Ishani Kalki  Shiva Callipso", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "D-W7F0".token(System(system), isLink = true),
            "Ishani Kalki".token(Character(0), isLink = true),
            "Shiva Callipso".token(Character(0)),
        )
    }

    "player, ship" {
        val ship: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("malediction") } returns ship
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("S-Killer").existing()
        val tokenizations = parser.parse("S-Killer malediction", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "S-Killer".token(Character(0)),
            "malediction".token(Ship(ship)),
        )
    }

    "system clear" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("319-3D", listOf("Delve")) } returns system
        val tokenizations = parser.parse("319-3D clr", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "319-3D".token(System(system)),
            "clr".token(Keyword(Clear)),
        )
    }

    "player, extra spaces, system, clear" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("MO-GZ5", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Rinah Minayin").existing()
        val tokenizations = parser.parse("Rinah Minayin   MO-GZ5 nv", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "Rinah Minayin".token(Character(0), isLink = true),
            "MO-GZ5".token(System(system)),
            "nv".token(Keyword(NoVisual)),
        )
    }

    "system with star" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("N-8YET", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Charlie Murdoch").existing()
        val tokenizations = parser.parse("N-8YET*  Charlie Murdoch", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "N-8YET".token(System(system), isLink = true),
            "Charlie Murdoch".token(Character(0)),
        )
    }

    "system with star, clear" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("N-8YET", listOf("Delve")) } returns system
        val tokenizations = parser.parse("N-8YET* clr", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "N-8YET".token(System(system), isLink = true),
            "clr".token(Keyword(Clear)),
        )
    }

    "ship with star, player, system with star" {
        val system: MapSolarSystem = mockk()
        val ship: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("NOL-M9", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("Caldari Shuttle") } returns ship
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Keeppley TT").existing()
        val tokenizations = parser.parse("Caldari Shuttle*  Keeppley TT  NOL-M9*", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "Caldari Shuttle".token(Ship(ship), isLink = true),
            "Keeppley TT".token(Character(0), isLink = true),
            "NOL-M9".token(System(system), isLink = true),
        )
    }

    "player, system, ship" {
        val system: MapSolarSystem = mockk()
        val ship: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("SVM-3K", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("eris") } returns ship
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("M2002M").existing()
        val tokenizations = parser.parse("M2002M  SVM-3K eris", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "M2002M".token(Character(0), isLink = true),
            "SVM-3K".token(System(system)),
            "eris".token(Ship(ship)),
        )
    }

    "player link, player, count, ship link, system" {
        val system: MapSolarSystem = mockk()
        val ship: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("319-3D", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("capsule") } returns ship
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("ssllss1", "Yaakov Y2").existing()
        val tokenizations = parser.parse("ssllss1  Yaakov Y2 2x capsule  319-3D", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "ssllss1".token(Character(0), isLink = true),
            "Yaakov Y2".token(Character(0)),
            "2x capsule".token(Ship(ship, count = 2), isLink = true),
            "319-3D".token(System(system)),
        )
    }

    "plural ships" {
        val ship: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("capsule") } returns ship
        val tokenizations = parser.parse("both capsules", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "both capsules".token(Ship(ship, count = 2, isPlural = true)),
        )
    }

    "player, question" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Ishani Kalki").existing()
        val tokenizations = parser.parse("Ishani Kalki where is he", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "Ishani Kalki".token(Character(0)),
            "where is he".token(Question(Location, "where is he")),
        )
    }

    "plus player" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("ssllss1").existing()
        val tokenizations = parser.parse("+ ssllss1", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "+".token(),
            "ssllss1".token(Character(0)),
        )
    }

    "plain word" {
        val tokenizations = parser.parse("rgr", listOf("Delve"))
        val actual = target(tokenizations)

        actual shouldBe listOf(
            "rgr".token(),
        )
    }

    "system, complex text, shortened system" {
        // TODO: More complexity here
        val system1: MapSolarSystem = mockk()
        val system2: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("MO-GZ5", listOf("Delve")) } returns system1
        every { mockSolarSystemsRepository.getFuzzySystem("1dq", listOf("Delve")) } returns system2
        val tokenizations = parser.parse("MO-GZ5 neutrals in 1dq on Mo gate", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "MO-GZ5".token(System(system1)),
            "neutrals in".token(),
            "1dq".token(System(system2)),
            "on Mo gate".token(),
        )
    }

    "ship count, changing capital text" {
        val ship1: Type = mockk()
        val ship2: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("wreaTH") } returns ship1
        every { mockShipTypesRepository.getFuzzyShip("LOKI") } returns ship2
        val tokenizations = parser.parse("2 wreaTH AND A LOKI", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "2 wreaTH".token(Ship(ship1, count = 2)),
            "AND A".token(),
            "LOKI".token(Ship(ship2)),
        )
    }

    "player, plus count, system" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("ZXB-VC", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("stark").existing()
        val tokenizations = parser.parse("stark +3 ZXB-VC", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "stark".token(Character(0)),
            "+3".token(Count(3, isPlus = true)),
            "ZXB-VC".token(System(system)),
        )
    }

    "plus count, system, ship" {
        val system: MapSolarSystem = mockk()
        val ship: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("ZXB-VC", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("hecate") } returns ship
        val tokenizations = parser.parse("+5  ZXB-VC hecate", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "+5".token(Count(5, isPlus = true), isLink = true),
            "ZXB-VC".token(System(system)),
            "hecate".token(Ship(ship)),
        )
    }

    "shiptypes question" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("ZXB-VC", listOf("Delve")) } returns system
        val tokenizations = parser.parse("ZXB-VC those +5 do we know other shiptypes?", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "ZXB-VC".token(System(system)),
            "those".token(),
            "+5".token(Count(5, isPlus = true)),
            "do we know other".token(),
            "shiptypes?".token(Question(ShipTypes, "shiptypes?")),
        )
    }

    "comment" {
        // TODO: More complexity here
        val ship1: Type = mockk()
        val ship2: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("shuttle") } returns ship1
        every { mockShipTypesRepository.getFuzzyShip("pod") } returns ship2
        val tokenizations = parser.parse("we have a lot of shuttle and pod movement of fraand mohiz in npc today", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "we have a lot of".token(),
            "shuttle".token(Ship(ship1)),
            "and".token(),
            "pod".token(Ship(ship2)),
            "movement of fraand mohiz in npc today".token(),
        )
    }

    "killmail" {
        val tokenizations = parser.parse("Kill: super-ego (Hecate)", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "Kill: super-ego (Hecate)".token(Kill(name = "super-ego", characterId = null, target = "Hecate")),
        )
    }

    "system link, player, plus count, count, ship, comma, count, keyword" {
        val system: MapSolarSystem = mockk()
        val ship: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("319-3D", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("hecate") } returns ship
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("RB Charlote").existing()
        val tokenizations = parser.parse("319-3D  RB Charlote +3 1x hecate, 3x nv", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "319-3D".token(System(system), isLink = true),
            "RB Charlote".token(Character(0)),
            "+3".token(Count(3, isPlus = true)),
            "1x hecate".token(Ship(ship, count = 1)),
            "3x".token(Count(3)),
            "nv".token(Keyword(NoVisual)),
        )
    }

    "player link, player, text, system, ship names" {
        val system: MapSolarSystem = mockk()
        val ship1: Type = mockk()
        val ship2: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("1-2J4P", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("purifier") } returns ship1
        every { mockShipTypesRepository.getFuzzyShip("sabre") } returns ship2
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("FeiShi", "iT0p").existing()
        val tokenizations = parser.parse("FeiShi  iT0p camping in 1-2J4P purifier + sabre", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "FeiShi".token(Character(0), isLink = true),
            "iT0p".token(Character(0)),
            "camping in".token(),
            "1-2J4P".token(System(system)),
            "purifier".token(Ship(ship1)),
            "+".token(),
            "sabre".token(Ship(ship2)),
        )
    }

    "system, text, url" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("Q-JQSG", listOf("Delve")) } returns system
        val tokenizations = parser.parse("Q-JQSG clearing https://adashboard.info/intel/dscan/view/D91snCmT", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "Q-JQSG".token(System(system)),
            "clearing".token(),
            "https://adashboard.info/intel/dscan/view/D91snCmT".token(Url),
        )
    }

    "many linked characters, including 3 word names" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("CPT Grabowsky", "Kelci Papi", "Kelio Rift", "Rim'tuti'tuks", "Lucho IYI", "Shopa s topa", "Urriah Souldown").existing()
        val tokenizations = parser.parse("CPT Grabowsky  Kelci Papi  Kelio Rift  Rim'tuti'tuks  Lucho IYI  Shopa s topa  Urriah Souldown", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "CPT Grabowsky".token(Character(0), isLink = true),
            "Kelci Papi".token(Character(0), isLink = true),
            "Kelio Rift".token(Character(0), isLink = true),
            "Rim'tuti'tuks".token(Character(0), isLink = true),
            "Lucho IYI".token(Character(0), isLink = true),
            "Shopa s topa".token(Character(0), isLink = true),
            "Urriah Souldown".token(Character(0)),
        )
    }

    "player, plus count, link, system link" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("4K-TRB", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Sixty Ever4", "Sixty").existing()
        val tokenizations = parser.parse("Sixty Ever4 +5 gang  4K-TRB*", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "Sixty Ever4".token(Character(0)),
            "+5".token(Count(5, isPlus = true)),
            "gang".token(),
            "4K-TRB".token(System(system), isLink = true),
        )
    }

    "two kills" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Nuodaxier", "nuodaxier001").existing()
        val tokenizations = parser.parse("Kill: Nuodaxier (Ishtar)  Kill: nuodaxier001 (Ishtar) Loki/Wolf", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "Kill: Nuodaxier (Ishtar)".token(Kill("Nuodaxier", characterId = 0, null, "Ishtar"), isLink = true),
            "Kill: nuodaxier001 (Ishtar)".token(Kill("nuodaxier001", characterId = 0, null, "Ishtar")),
            "Loki/Wolf".token(),
        )
    }

    "gate and gate camp" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("chazzathespazman", "camp").existing()
        val system1: MapSolarSystem = mockk()
        val system2: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("B-DBYQ", listOf("Delve")) } returns system1
        every { mockSolarSystemsRepository.getFuzzySystem("J5A-IX", listOf("Delve")) } returns system2
        val tokenizations = parser.parse("chazzathespazman +7  B-DBYQ gate camp on  J5A-IX gate", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "chazzathespazman".token(Character(0)),
            "+7".token(Count(7, isPlus = true), isLink = true),
            "B-DBYQ".token(System(system1)),
            "gate camp".token(Keyword(GateCamp)),
            "on".token(),
            "J5A-IX gate".token(TokenType.Gate(system2)),
        )
    }

    "ambiguous navy ships" {
        val ship1: Type = mockk()
        val ship2: Type = mockk()
        val ship3: Type = mockk()
        val ship4: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("exequror") } returns ship1
        every { mockShipTypesRepository.getFuzzyShip("exequror navy") } returns ship2
        every { mockShipTypesRepository.getFuzzyShip("navy caracal") } returns ship3
        every { mockShipTypesRepository.getFuzzyShip("caracal") } returns ship4
        val tokenizations = parser.parse("exequror navy caracal", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "exequror navy".token(Ship(ship2)),
            "caracal".token(Ship(ship4)),
        )
    }

    "ambiguous navy ships with capitalisation" {
        val ship1: Type = mockk()
        val ship2: Type = mockk()
        val ship3: Type = mockk()
        val ship4: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("Osprey") } returns ship1
        every { mockShipTypesRepository.getFuzzyShip("Osprey Navy") } returns ship2
        every { mockShipTypesRepository.getFuzzyShip("Navy Brutix") } returns ship3
        every { mockShipTypesRepository.getFuzzyShip("Brutix") } returns ship4
        val tokenizations = parser.parse("Osprey Navy, Brutix", listOf("Delve"))

        val actual = target(tokenizations)

        actual shouldBe listOf(
            "Osprey Navy".token(Ship(ship2)),
            "Brutix".token(Ship(ship4)),
        )
    }
})

private fun String.token(type: TokenType? = null, isLink: Boolean = false): Token {
    return Token(split(" "), type = type, isLink = isLink)
}

private fun List<String>.existing(): Map<String, CharacterStatus> {
    return associateWith { CharacterStatus.Active(0) }
}
