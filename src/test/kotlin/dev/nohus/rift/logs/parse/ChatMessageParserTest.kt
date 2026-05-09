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
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Gate
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Keyword
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Kill
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Movement
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Question
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Ship
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.System
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Url
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapRegion
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.repositories.WordsRepository
import dev.nohus.rift.repositories.character.CharacterStatus
import dev.nohus.rift.repositories.character.CharactersRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

class ChatMessageParserTest : FreeSpec({

    isolationMode = IsolationMode.InstancePerTest

    val mockSolarSystemsRepository: SolarSystemsRepository = mockk()
    val mockShipTypesRepository: ShipTypesRepository = mockk()
    val mockCharactersRepository: CharactersRepository = mockk()
    val mockWordsRepository: WordsRepository = mockk()
    val characterNameValidator = CharacterNameValidator()
    val target = ChatMessageParser(
        mockSolarSystemsRepository,
        mockShipTypesRepository,
        mockCharactersRepository,
        mockWordsRepository,
        characterNameValidator,
    )
    every { mockSolarSystemsRepository.getFuzzySystem(any(), eq(listOf("Delve"))) } returns null
    every { mockShipTypesRepository.getFuzzyShip(any()) } returns null
    coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns emptyMap()
    every { mockWordsRepository.isWord(any()) } returns false
    every { mockWordsRepository.isTypeName(any()) } returns false

    "system link, player link, player" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("D-W7F0", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Ishani Kalki", "Shiva Callipso").existing()

        val actual = target.parse("D-W7F0  Ishani Kalki  Shiva Callipso", listOf("Delve"))

        actual shouldContain listOf(
            "D-W7F0".token(System(system), isLink = true),
            "".token(),
            "Ishani Kalki".token(Character(0), isLink = true),
            "".token(),
            "Shiva Callipso".token(Character(0)),
        )
    }

    "player, ship" {
        val ship: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("malediction") } returns ship
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("S-Killer").existing()

        val actual = target.parse("S-Killer malediction", listOf("Delve"))

        actual shouldContain listOf(
            "S-Killer".token(Character(0)),
            "malediction".token(Ship(ship)),
        )
    }

    "system clear" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("319-3D", listOf("Delve")) } returns system

        val actual = target.parse("319-3D clr", listOf("Delve"))

        actual shouldContain listOf(
            "319-3D".token(System(system)),
            "clr".token(Keyword(Clear)),
        )
    }

    "system clear cyrillic" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("319-3D", listOf("Delve")) } returns system

        val actual = target.parse("319-3D сlr", listOf("Delve"))

        actual shouldContain listOf(
            "319-3D".token(System(system)),
            "сlr".token(Keyword(Clear)),
        )
    }

    "player, extra spaces, system, clear" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("MO-GZ5", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Rinah Minayin").existing()

        val actual = target.parse("Rinah Minayin   MO-GZ5 nv", listOf("Delve"))

        actual shouldContain listOf(
            "Rinah Minayin".token(Character(0), isLink = true),
            " ".token(),
            "MO-GZ5".token(System(system)),
            "nv".token(Keyword(NoVisual)),
        )
    }

    "system with star" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("N-8YET", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Charlie Murdoch").existing()

        val actual = target.parse("N-8YET*  Charlie Murdoch", listOf("Delve"))

        actual shouldContain listOf(
            "N-8YET*".token(System(system), isLink = true),
            "".token(),
            "Charlie Murdoch".token(Character(0)),
        )
    }

    "system with star, clear" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("N-8YET", listOf("Delve")) } returns system

        val actual = target.parse("N-8YET* clr", listOf("Delve"))

        actual shouldContain listOf(
            "N-8YET*".token(System(system), isLink = true),
            "clr".token(Keyword(Clear)),
        )
    }

    "system with star, alternative clear" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("N-8YET", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("clr du").existing()

        val actual = target.parse("N-8YET* clr du", listOf("Delve"))

        actual shouldContain listOf(
            "N-8YET*".token(System(system), isLink = true),
            "clr du".token(Keyword(Clear)),
        )
    }

    "ship with star, player, system with star" {
        val system: MapSolarSystem = mockk()
        val ship: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("NOL-M9", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("Caldari Shuttle") } returns ship
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Keeppley TT").existing()

        val actual = target.parse("Caldari Shuttle*  Keeppley TT  NOL-M9*", listOf("Delve"))

        actual shouldContain listOf(
            "Caldari Shuttle*".token(Ship(ship), isLink = true),
            "".token(),
            "Keeppley TT".token(Character(0), isLink = true),
            "".token(),
            "NOL-M9*".token(System(system), isLink = true),
        )
    }

    "player, system, ship" {
        val system: MapSolarSystem = mockk()
        val ship: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("SVM-3K", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("eris") } returns ship
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("M2002M").existing()

        val actual = target.parse("M2002M  SVM-3K eris", listOf("Delve"))

        actual shouldContain listOf(
            "M2002M".token(Character(0), isLink = true),
            "".token(),
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

        val actual = target.parse("ssllss1  Yaakov Y2 2x capsule  319-3D", listOf("Delve"))

        actual shouldContain listOf(
            "ssllss1".token(Character(0), isLink = true),
            "".token(),
            "Yaakov Y2".token(Character(0)),
            "2x capsule".token(Ship(ship, count = 2), isLink = true),
            "".token(),
            "319-3D".token(System(system)),
        )
    }

    "plural ships" {
        val ship: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("capsule") } returns ship

        val actual = target.parse("both capsules", listOf("Delve"))

        actual shouldContain listOf(
            "both capsules".token(Ship(ship, count = 2, isPlural = true)),
        )
    }

    "player, question" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Ishani Kalki").existing()

        val actual = target.parse("Ishani Kalki where is he", listOf("Delve"))

        actual shouldContain listOf(
            "Ishani Kalki".token(Character(0)),
            "where is he".token(Question(Location, "where is he")),
        )
    }

    "plus player" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("ssllss1").existing()

        val actual = target.parse("+ ssllss1", listOf("Delve"))

        actual shouldContain listOf(
            "+".token(),
            "ssllss1".token(Character(0)),
        )
    }

    "plain word" {
        val actual = target.parse("rgr", listOf("Delve"))

        actual shouldContain listOf(
            "rgr".token(),
        )
    }

    "system, complex text, shortened system" {
        // TODO: More complexity here
        val system1: MapSolarSystem = mockk()
        val system2: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("MO-GZ5", listOf("Delve")) } returns system1
        every { mockSolarSystemsRepository.getFuzzySystem("1dq", listOf("Delve")) } returns system2

        val actual = target.parse("MO-GZ5 neutrals in 1dq on Mo gate", listOf("Delve"))

        actual shouldContain listOf(
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

        val actual = target.parse("2 wreaTH AND A LOKI", listOf("Delve"))

        actual shouldContain listOf(
            "2 wreaTH".token(Ship(ship1, count = 2)),
            "AND A".token(),
            "LOKI".token(Ship(ship2)),
        )
    }

    "negative ship count" {
        val ship: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("loki") } returns ship

        val actual = target.parse("-3 loki", listOf("Delve"))

        actual shouldContain listOf(
            "-3".token(),
            "loki".token(Ship(ship)),
        )
    }

    "player, plus count, system" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("ZXB-VC", listOf("Delve")) } returns system
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("stark").existing()

        val actual = target.parse("stark +3 ZXB-VC", listOf("Delve"))

        actual shouldContain listOf(
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

        val actual = target.parse("+5  ZXB-VC hecate", listOf("Delve"))

        actual shouldContain listOf(
            "+5".token(Count(5, isPlus = true), isLink = true),
            "".token(),
            "ZXB-VC".token(System(system)),
            "hecate".token(Ship(ship)),
        )
    }

    "plus count with space, system, ship" {
        val system: MapSolarSystem = mockk()
        val ship: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("ZXB-VC", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("hecate") } returns ship

        val actual = target.parse("+ 5  ZXB-VC hecate", listOf("Delve"))

        actual shouldContain listOf(
            "+ 5".token(Count(5, isPlus = true), isLink = true),
            "".token(),
            "ZXB-VC".token(System(system)),
            "hecate".token(Ship(ship)),
        )
    }

    "shiptypes question" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("ZXB-VC", listOf("Delve")) } returns system

        val actual = target.parse("ZXB-VC those +5 do we know other shiptypes?", listOf("Delve"))

        actual shouldContain listOf(
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

        val actual = target.parse("we have a lot of shuttle and pod movement of fraand mohiz in npc today", listOf("Delve"))

        actual shouldContain listOf(
            "we have a lot of".token(),
            "shuttle".token(Ship(ship1)),
            "and".token(),
            "pod".token(Ship(ship2)),
            "movement of fraand mohiz in npc today".token(),
        )
    }

    "killmail" {
        val actual = target.parse("Kill: super-ego (Hecate)", listOf("Delve"))

        actual shouldContain listOf(
            "Kill: super-ego (Hecate)".token(Kill(name = "super-ego", characterId = null, target = "Hecate")),
        )
    }

    "killmail with mobile warp disruptor" {
        val actual = target.parse("Kill: Moustache Bob Caliente (Mobile Small Warp Disruptor I)", listOf("Delve"))

        actual shouldContain listOf(
            "Kill: Moustache Bob Caliente (Mobile Small Warp Disruptor I)".token(Kill(name = "Moustache Bob Caliente", characterId = null, target = "Mobile Small Warp Disruptor I")),
        )
    }

    "system link, player, plus count, count, ship, comma, count, keyword" {
        val system: MapSolarSystem = mockk()
        val ship: Type = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("319-3D", listOf("Delve")) } returns system
        every { mockShipTypesRepository.getFuzzyShip("hecate") } returns ship
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("RB Charlote").existing()

        val actual = target.parse("319-3D  RB Charlote +3 1x hecate, 3x nv", listOf("Delve"))

        actual shouldContain listOf(
            "319-3D".token(System(system), isLink = true),
            "".token(),
            "RB Charlote".token(Character(0)),
            "+3".token(Count(3, isPlus = true)),
            "1x hecate,".token(Ship(ship, count = 1)),
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

        val actual = target.parse("FeiShi  iT0p camping in 1-2J4P purifier + sabre", listOf("Delve"))

        actual shouldContain listOf(
            "FeiShi".token(Character(0), isLink = true),
            "".token(),
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

        val actual = target.parse("Q-JQSG clearing https://adashboard.info/intel/dscan/view/D91snCmT", listOf("Delve"))

        actual shouldContain listOf(
            "Q-JQSG".token(System(system)),
            "clearing".token(),
            "https://adashboard.info/intel/dscan/view/D91snCmT".token(Url),
        )
    }

    "long comment" {
        val (actual, time) = measureTimedValue {
            target.parse("if you want to know if your +1/-1 is around look at watch list it will have their ship symbol on the list next to their name if they are on grid", listOf("Delve"))
        }

        actual shouldContain listOf(
            "if you want to know if your +1/-1 is around look at watch list it will have their ship symbol on the list next to their name if they are on grid".token(),
        )
        actual shouldHaveSize 1
        time shouldBeLessThan 200.milliseconds
    }

    "hostile crafted text" {
        // Text specifically crafted for branching ambiguity and an overwhelming amount of possible tokenizations
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("aaa", "aaa aaa", "aaa aaa aaa").existing()

        val (actual, time) = measureTimedValue {
            target.parse("aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa", listOf("Delve"))
        }

        actual shouldContain listOf(
            "aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa".token(),
        )
        actual shouldHaveSize 1
        time shouldBeLessThan 1000.milliseconds
    }

    "space at start of message" {
        val actual = target.parse(" RGC for CSM 18", listOf("Delve"))

        actual shouldContain listOf(
            " RGC for CSM 18".token(),
        )
        actual shouldHaveSize 1
    }

    "many linked characters, including 3 word names" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("CPT Grabowsky", "Kelci Papi", "Kelio Rift", "Rim'tuti'tuks", "Lucho IYI", "Shopa s topa", "Urriah Souldown").existing()

        val actual = target.parse("CPT Grabowsky  Kelci Papi  Kelio Rift  Rim'tuti'tuks  Lucho IYI  Shopa s topa  Urriah Souldown", listOf("Delve"))

        actual shouldContain listOf(
            "CPT Grabowsky".token(Character(0), isLink = true),
            "".token(),
            "Kelci Papi".token(Character(0), isLink = true),
            "".token(),
            "Kelio Rift".token(Character(0), isLink = true),
            "".token(),
            "Rim'tuti'tuks".token(Character(0), isLink = true),
            "".token(),
            "Lucho IYI".token(Character(0), isLink = true),
            "".token(),
            "Shopa s topa".token(Character(0), isLink = true),
            "".token(),
            "Urriah Souldown".token(Character(0)),
        )
    }

    "plain text link" {
        val actual = target.parse("maybe its hourly  ", listOf("Delve"))

        actual shouldContain listOf(
            "maybe its hourly  ".token(),
        )
        actual shouldHaveSize 1
    }

    "character-like plain text" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("was", "the blues").existing(CharacterStatus.Inactive(0))
        every { mockWordsRepository.isWord("was") } returns true
        every { mockWordsRepository.isWord("the") } returns true
        every { mockWordsRepository.isWord("blues") } returns true

        val actual = target.parse("nothing of value was lost it's the blues", listOf("Delve"))

        actual shouldContain listOf(
            "nothing of value was lost it's the blues".token(),
        )
        actual shouldHaveSize 1
    }

    "character-like plain text with active characters" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("was", "the blues").existing(CharacterStatus.Active(0))
        every { mockWordsRepository.isWord("was") } returns true
        every { mockWordsRepository.isWord("the") } returns true
        every { mockWordsRepository.isWord("blues") } returns true

        val actual = target.parse("nothing of value was lost it's the blues", listOf("Delve"))

        actual shouldContain listOf(
            "nothing of value".token(),
            "was".token(Character(0)),
            "lost it's".token(),
            "the blues".token(Character(0)),
        )
        actual shouldHaveSize 4
    }

    "system gate" {
        val ship: Type = mockk()
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Ruthy").existing()
        every { mockShipTypesRepository.getFuzzyShip("stabber") } returns ship
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("uvho", listOf("Delve")) } returns system

        val actual = target.parse("Ruthy stabber uvho gate", listOf("Delve"))

        actual shouldContain listOf(
            "Ruthy".token(Character(0)),
            "stabber".token(Ship(ship)),
            "uvho gate".token(Gate(system)),
        )
    }

    "system gate with more text" {
        val k7: MapSolarSystem = mockk()
        val ogy: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("k7", listOf("Delve")) } returns k7
        every { mockSolarSystemsRepository.getFuzzySystem("OGY", listOf("Delve")) } returns ogy

        val actual = target.parse("moved away from k7 gate in OGY", listOf("Delve"))

        actual shouldContain listOf(
            "moved away from".token(),
            "k7 gate".token(Gate(k7)),
            "in".token(),
            "OGY".token(System(ogy)),
        )
    }

    "gate system" {
        val ship: Type = mockk()
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("CrystalWater").existing()
        every { mockShipTypesRepository.getFuzzyShip("Retribution") } returns ship
        val fmjk5: MapSolarSystem = mockk()
        val jp4: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("FM-JK5", listOf("Delve")) } returns fmjk5
        every { mockSolarSystemsRepository.getFuzzySystem("JP4", listOf("Delve")) } returns jp4

        val actual = target.parse("FM-JK5  CrystalWater +10 gate JP4  Retribution", listOf("Delve"))

        actual shouldContain listOf(
            "FM-JK5".token(System(fmjk5), isLink = true),
            "".token(),
            "CrystalWater".token(Character(0)),
            "+10".token(Count(10, isPlus = true)),
            "gate JP4".token(Gate(jp4), isLink = true),
            "".token(),
            "Retribution".token(Ship(ship)),
        )
    }

    "going system" {
        val ship: Type = mockk()
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("CrystalWater").existing()
        every { mockShipTypesRepository.getFuzzyShip("Retribution") } returns ship
        val fmjk5: MapSolarSystem = mockk()
        val jp4: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("FM-JK5", listOf("Delve")) } returns fmjk5
        every { mockSolarSystemsRepository.getFuzzySystem("JP4", listOf("Delve")) } returns jp4

        val actual = target.parse("FM-JK5  CrystalWater going JP4", listOf("Delve"))

        actual shouldContain listOf(
            "FM-JK5".token(System(fmjk5), isLink = true),
            "".token(),
            "CrystalWater".token(Character(0)),
            "going JP4".token(Movement("going", jp4, isGate = false)),
        )
    }

    "jumped system" {
        val ship: Type = mockk()
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("CrystalWater").existing()
        every { mockShipTypesRepository.getFuzzyShip("Retribution") } returns ship
        val fmjk5: MapSolarSystem = mockk()
        val jp4: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("FM-JK5", listOf("Delve")) } returns fmjk5
        every { mockSolarSystemsRepository.getFuzzySystem("JP4", listOf("Delve")) } returns jp4

        val actual = target.parse("FM-JK5  CrystalWater jumped JP4", listOf("Delve"))

        actual shouldContain listOf(
            "FM-JK5".token(System(fmjk5), isLink = true),
            "".token(),
            "CrystalWater".token(Character(0)),
            "jumped JP4".token(Movement("jumped", jp4, isGate = false)),
        )
    }

    "jumped gate" {
        val ship: Type = mockk()
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("CrystalWater").existing()
        every { mockShipTypesRepository.getFuzzyShip("Retribution") } returns ship
        val fmjk5: MapSolarSystem = mockk()
        val jp4: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("FM-JK5", listOf("Delve")) } returns fmjk5
        every { mockSolarSystemsRepository.getFuzzySystem("JP4", listOf("Delve")) } returns jp4

        val actual = target.parse("FM-JK5  CrystalWater jumped JP4 gate", listOf("Delve"))

        actual shouldContain listOf(
            "FM-JK5".token(System(fmjk5), isLink = true),
            "".token(),
            "CrystalWater".token(Character(0)),
            "jumped JP4 gate".token(Movement("jumped", jp4, isGate = true)),
        )
    }

    "system ansiblex" {
        val system: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("1DQ", listOf("Delve")) } returns system

        val actual = target.parse("on 1DQ ansi", listOf("Delve"))

        actual shouldContain listOf(
            "on".token(),
            "1DQ ansi".token(Gate(system, isAnsiblex = true)),
        )
    }

    "gate and gate camp" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("chazzathespazman", "camp").existing()
        val system1: MapSolarSystem = mockk()
        val system2: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("B-DBYQ", listOf("Delve")) } returns system1
        every { mockSolarSystemsRepository.getFuzzySystem("J5A-IX", listOf("Delve")) } returns system2

        val actual = target.parse("chazzathespazman +7  B-DBYQ gate camp on  J5A-IX gate", listOf("Delve"))

        actual shouldContain listOf(
            "chazzathespazman".token(Character(0)),
            "+7".token(Count(7, isPlus = true), isLink = true),
            "".token(),
            "B-DBYQ".token(System(system1)),
            "gate camp".token(Keyword(GateCamp)),
            "on ".token(),
            "J5A-IX gate".token(Gate(system2)),
        )
    }

    "space at beginning" {
        val actual = target.parse(" word", listOf("Delve"))

        actual shouldContain listOf(
            " word".token(),
        )
    }

    "space at end" {
        val actual = target.parse("word ", listOf("Delve"))

        actual shouldContain listOf(
            "word ".token(),
        )
    }

    "space only" {
        val actual = target.parse(" ", listOf("Delve"))

        actual shouldContain listOf(
            " ".token(),
        )
    }

    "multiple spaces only" {
        val actual = target.parse("   ", listOf("Delve"))

        actual shouldContain listOf(
            "   ".token(),
        )
    }

    "comma" {
        val actual = target.parse(",", listOf("Delve"))

        actual shouldContain listOf(
            ",".token(),
        )
    }

    "player name matching an out-of-region system name" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("KQK").existing()
        val kqk: MapSolarSystem = mockk {
            every { regionId } returns 0
        }
        val oneSmeb: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("KQK", listOf("Delve")) } returns kqk
        every { mockSolarSystemsRepository.getRegion(0) } returns MapRegion(0, "Pure Blind", 0.0, 0.0, 0.0, 0.0, 0.0)
        every { mockSolarSystemsRepository.getFuzzySystem("1-SMEB", listOf("Delve")) } returns oneSmeb

        val actual = target.parse("KQK  1-SMEB", listOf("Delve"))

        actual shouldContain listOf(
            "KQK".token(Character(0), isLink = true),
            "".token(),
            "1-SMEB".token(System(oneSmeb)),
        )
    }

    "player name matching an in-region system name" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("KQK").existing()
        val kqk: MapSolarSystem = mockk {
            every { regionId } returns 0
        }
        val oneSmeb: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("KQK", listOf("Delve")) } returns kqk
        every { mockSolarSystemsRepository.getRegion(0) } returns MapRegion(0, "Delve", 0.0, 0.0, 0.0, 0.0, 0.0)
        every { mockSolarSystemsRepository.getFuzzySystem("1-SMEB", listOf("Delve")) } returns oneSmeb

        val actual = target.parse("KQK  1-SMEB", listOf("Delve"))

        actual shouldContain listOf(
            "KQK".token(System(kqk), isLink = true),
            "".token(),
            "1-SMEB".token(System(oneSmeb)),
        )
    }

    "player name substring matching a system name" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Jita Alt 1").existing()
        val jita: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("Jita", listOf("Delve")) } returns jita

        val actual = target.parse("Jita Alt 1", listOf("Delve"))

        actual shouldContain listOf(
            "Jita Alt 1".token(Character(0)),
        )
    }

    "inactive player name matching a type name" {
        val ship: Type = mockk()
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Drake", "Fortizar", "Festival Launcher").existing(CharacterStatus.Inactive(0))
        every { mockWordsRepository.isTypeName("Drake") } returns true
        every { mockWordsRepository.isTypeName("Fortizar") } returns true
        every { mockWordsRepository.isTypeName("Festival Launcher") } returns true
        every { mockShipTypesRepository.getFuzzyShip("Drake") } returns ship

        val actual = target.parse("Drake at a Fortizar with a Festival Launcher", listOf("Delve"))

        actual shouldContain listOf(
            "Drake".token(Ship(ship)),
            "at a Fortizar with a Festival Launcher".token(),
        )
        actual shouldHaveSize 2
    }

    "active player name matching a type name" {
        val ship: Type = mockk()
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Drake", "Fortizar", "Festival Launcher").existing(CharacterStatus.Active(0))
        every { mockWordsRepository.isTypeName("Drake") } returns true
        every { mockWordsRepository.isTypeName("Fortizar") } returns true
        every { mockWordsRepository.isTypeName("Festival Launcher") } returns true
        every { mockShipTypesRepository.getFuzzyShip("Drake") } returns ship

        val actual = target.parse("Drake at a Fortizar with a Festival Launcher", listOf("Delve"))

        actual shouldContain listOf(
            "Drake".token(Ship(ship)),
            "at a".token(),
            "Fortizar".token(Character(0)),
            "with a".token(),
            "Festival Launcher".token(Character(0)),
        )
        actual shouldHaveSize 8
    }

    "dormant character" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Dormant").existing(CharacterStatus.Dormant(0))

        val actual = target.parse("Dormant", listOf("Delve"))

        actual shouldContain listOf(
            "Dormant".token(),
        )
        actual shouldHaveSize 1
    }

    "system, player name substring matching a system name" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(eq(Originator.ChatLogs), any()) } returns listOf("Jita Alt 1").existing()
        val jita: MapSolarSystem = mockk()
        every { mockSolarSystemsRepository.getFuzzySystem("Jita", listOf("Delve")) } returns jita

        val actual = target.parse("Jita Jita Alt 1", listOf("Delve"))

        actual shouldContain listOf(
            "Jita".token(System(jita)),
            "Jita Alt 1".token(Character(0)),
        )
    }

    "successive navy ships" {
        val ship1: Type = mockk()
        val ship2: Type = mockk()
        val ship3: Type = mockk()
        val ship4: Type = mockk()
        every { mockShipTypesRepository.getFuzzyShip("caracal navy") } returns ship1
        every { mockShipTypesRepository.getFuzzyShip("osprey navy") } returns ship2
        every { mockShipTypesRepository.getFuzzyShip("navy caracal") } returns ship1
        every { mockShipTypesRepository.getFuzzyShip("navy osprey") } returns ship2
        every { mockShipTypesRepository.getFuzzyShip("caracal") } returns ship3
        every { mockShipTypesRepository.getFuzzyShip("osprey") } returns ship4

        val actual = target.parse("caracal navy osprey navy", listOf("Delve"))

        actual shouldContain listOf(
            "caracal navy".token(Ship(ship1)),
            "osprey navy".token(Ship(ship2)),
        )
    }
})

private fun String.token(type: TokenType? = null, isLink: Boolean = false): Token {
    return Token(split(" "), type = type, isLink = isLink)
}

private fun List<String>.existing(status: CharacterStatus = CharacterStatus.Active(0)): Map<String, CharacterStatus> {
    return associateWith { status }
}
