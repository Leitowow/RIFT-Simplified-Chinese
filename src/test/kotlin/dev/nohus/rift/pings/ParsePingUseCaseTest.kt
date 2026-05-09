package dev.nohus.rift.pings

import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.MapStatusRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.character.CharactersRepository
import dev.nohus.rift.standings.StandingsRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class ParsePingUseCaseTest : FreeSpec({

    isolationMode = IsolationMode.InstancePerTest

    val timestamp = Instant.now()
    val mockCharactersRepository: CharactersRepository = mockk()
    val mockSolarSystemsRepository: SolarSystemsRepository = mockk()
    val mockMapStatusRepository: MapStatusRepository = mockk()
    val mockStandingsRepository: StandingsRepository = mockk()
    val target = ParsePingUseCase(
        charactersRepository = mockCharactersRepository,
        solarSystemsRepository = mockSolarSystemsRepository,
        mapStatusRepository = mockMapStatusRepository,
        standingsRepository = mockStandingsRepository,
    )

    coEvery { mockCharactersRepository.getCharacterId(Originator.Pings, "Havish Montak") } returns 1
    coEvery { mockCharactersRepository.getCharacterId(Originator.Pings, "Mrbluff343") } returns 2
    coEvery { mockCharactersRepository.getCharacterId(Originator.Pings, "Mist Amatin") } returns 3
    coEvery { mockCharactersRepository.getCharacterId(Originator.Pings, "Asher Elias") } returns 4
    coEvery { mockCharactersRepository.getCharacterId(Originator.Pings, "Lodena Minax") } returns 5
    coEvery { mockCharactersRepository.getCharacterId(Originator.Pings, "Arkadios Sol") } returns 6
    val system1Dq1: MapSolarSystem = mockk { every { id } returns 100000001 }
    val systemUalx: MapSolarSystem = mockk { every { id } returns 100000002 }
    val system0sht: MapSolarSystem = mockk { every { id } returns 100000003 }
    val systemUqmo: MapSolarSystem = mockk { every { id } returns 100000004 }
    val system49U6: MapSolarSystem = mockk { every { id } returns 100000005 }
    val system407m: MapSolarSystem = mockk { every { id } returns 100000006 }
    every { mockSolarSystemsRepository.getFuzzySystem(any(), any(), any()) } returns null
    every { mockSolarSystemsRepository.getFuzzySystem("1DQ1-A", emptyList()) } returns system1Dq1
    every { mockSolarSystemsRepository.getFuzzySystem("UALX-3", emptyList()) } returns systemUalx
    every { mockSolarSystemsRepository.getFuzzySystem("0SHT", emptyList()) } returns system0sht
    every { mockSolarSystemsRepository.getFuzzySystem("U-Q", emptyList()) } returns null
    every { mockSolarSystemsRepository.getFuzzySystem("U-Q", emptyList(), listOf(30000629)) } returns systemUqmo
    every { mockSolarSystemsRepository.getFuzzySystem("49-U", emptyList(), listOf(30000629)) } returns system49U6
    every { mockSolarSystemsRepository.getFuzzySystem("4-07", emptyList(), listOf(30000629)) } returns system407m
    every { mockStandingsRepository.getFriendlyAllianceIds() } returns setOf(1)
    every { mockMapStatusRepository.status } returns mockk {
        every { value } returns mapOf(
            // U-QMOA
            30000629 to mockk {
                every { sovereignty } returns mockk {
                    every { allianceId } returns 1
                }
            },
            // U-QVWD
            30001155 to mockk {
                every { sovereignty } returns mockk {
                    every { allianceId } returns 2
                }
            },
        )
    }

    listOf(
        """
            Single line
            ~~~ This was a guardbees broadcast from toaster_jane to all at 2024-01-25 02:18:57.549510 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.PlainText(
                timestamp = timestamp,
                sourceText = "",
                text = "Single line",
                sender = "toaster_jane",
                target = "all",
            ),
        ),
        """
            Single‍‍‍‍‍‍‍ line
            ~~~ This was a guardbees broadcast from toaster_jane to all at 2024-01-25 02:18:57.549510 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.PlainText(
                timestamp = timestamp,
                sourceText = "",
                text = "Single line",
                sender = "toaster_jane",
                target = "all",
            ),
        ),
        """
            more sif into beehive on mal plox now

            ~~~ This was a guardbees broadcast from toaster_jane to all at 2024-01-25 02:18:57.549510 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.PlainText(
                timestamp = timestamp,
                sourceText = "",
                text = "more sif into beehive on mal plox now",
                sender = "toaster_jane",
                target = "all",
            ),
        ),
        """
            Hostiles need some time to dock and spin ships. Bring tackle and hunters. NEUTS on sentinels too.

            FC Name: Havish Montak
            Formup Location: 1DQ1-A
            PAP Type: Strategic
            Comms: Op 4 https://gnf.lt/2eMgwE2.html
            Doctrine: Void Rays (MWD) (Boosts > Logi > Kikis > Hawk/Slasher/Hyena/Keres)

            ~~~ This was a coord broadcast from dakota_holtgard to all at 2024-01-22 18:43:14.530878 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "Hostiles need some time to dock and spin ships. Bring tackle and hunters. NEUTS on sentinels too.",
                fleetCommander = FleetCommander("Havish Montak", 1),
                fleet = null,
                formupLocations = listOf(FormupLocation.System(100000001)),
                papType = PapType.Strategic,
                comms = Comms.Mumble("Op 4", "https://gnf.lt/2eMgwE2.html"),
                doctrine = Doctrine(
                    text = "Void Rays (MWD) (Boosts > Logi > Kikis > Hawk/Slasher/Hyena/Keres)",
                    link = "https://goonfleet.com/index.php/topic/345055-active-strat-void-rays-mwd-kikis/",
                ),
                broadcastSource = "coord",
                target = "all",
            ),
        ),
        """
            WTF 205 - Command Destroyers  

            What mindlink do you need to boost? How the hell does booshing work? Why do I have a combat timer on this gate? Find the answers to all these questions and more in this class.  

            FC: Mrbluff343 
            Fleet: WTF 205 
            Formup: 1DQ1-A PAP 
            Type: Peacetime 
            Comms: General Doctrine: Pontifex/Stork/Bifrost/Magus/Draugur - Ensure you have a MJFG fitted.  

            Please note there is no SRP for Gooniversity Classes. Our 200 series covers advanced topics, so bring your own ships at your own risk.
            
            ~~~ This was a broadcast from ankh_lai to gooniversity at 2024-01-20 23:09:29.893448 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "WTF 205 - Command Destroyers\n\nWhat mindlink do you need to boost? How the hell does booshing work? Why do I have a combat timer on this gate? Find the answers to all these questions and more in this class.\n\nPlease note there is no SRP for Gooniversity Classes. Our 200 series covers advanced topics, so bring your own ships at your own risk.",
                fleetCommander = FleetCommander("Mrbluff343", 2),
                fleet = "WTF 205",
                formupLocations = listOf(FormupLocation.System(100000001)),
                papType = PapType.Peacetime,
                comms = Comms.Text("General"),
                doctrine = Doctrine(
                    text = "Pontifex/Stork/Bifrost/Magus/Draugur - Ensure you have a MJFG fitted.",
                    link = null,
                ),
                broadcastSource = null,
                target = "gooniversity",
            ),
        ),
        """
            I found an absolutely amazing wh rout of awesome. Getin, im itching for some blood!
    
            FC Name: Mist Amatin
            Formup Location: 1DQ1-A
            PAP Type: Strategic
            Comms: Op 3 https://gnf.lt/NOH1FNH.html
            Doctrine: *FC Choice* (Fits in MOTD)
            Shield ENIs > Scalpels > Dictors > Handout vigils
            
            ~~~ This was a coord broadcast from epofhis to all at 2024-01-24 14:05:11.680333 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "I found an absolutely amazing wh rout of awesome. Getin, im itching for some blood!",
                fleetCommander = FleetCommander("Mist Amatin", 3),
                fleet = null,
                formupLocations = listOf(FormupLocation.System(100000001)),
                papType = PapType.Strategic,
                comms = Comms.Mumble("Op 3", "https://gnf.lt/NOH1FNH.html"),
                doctrine = Doctrine(
                    text = "*FC Choice* (Fits in MOTD)\nShield ENIs > Scalpels > Dictors > Handout vigils",
                    link = "https://goonfleet.com/index.php/topic/349390-active-peacetime-eni-fleet/",
                ),
                broadcastSource = "coord",
                target = "all",
            ),
        ),
        """
            Fleet up on Asher

            FC: Asher Elias
            Fleet name: Asher's big time fun fleet #1 sir
            Comms: Op 1
            Sun Tzu quote: Optional

            ~~~ This was a broadcast from asher_elias to discord at 2024-01-31 00:17:02.533642 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "Fleet up on Asher\n\nSun Tzu quote: Optional",
                fleetCommander = FleetCommander("Asher Elias", 4),
                fleet = "Asher's big time fun fleet #1 sir",
                formupLocations = emptyList(),
                papType = null,
                comms = Comms.Text("Op 1"),
                doctrine = null,
                broadcastSource = null,
                target = "discord",
            ),
        ),
        """
            Fleet up on Asher
            
            FC: Asher Elias
            Formup Location: U-Q
            
            ~~~ This was a broadcast from asher_elias to discord at 2024-01-31 00:17:02.533642 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "Fleet up on Asher",
                fleetCommander = FleetCommander("Asher Elias", 4),
                fleet = null,
                formupLocations = listOf(FormupLocation.System(100000004)),
                papType = null,
                comms = null,
                doctrine = null,
                broadcastSource = null,
                target = "discord",
            ),
        ),
        """
            WTF 0 - Imperium Basics

            Not sure what SIGs are? Still trying to get mumble to work? Lost your blingy ship in a strat op and now you’re spacebroke? Then join this fleet to find out all the essentials to function in the Imperium.

            FC: Lodena Minax
            Fleet: WTF 0
            Formup: Theory only, join from anywhere. 
            PAP Type: SIG/SQUAD
            Comms: Gooniversity Discord -> The Big Classroom - https://discord.gg/gooniversity
            Doctrine: None - This is a theory class

            ~~~ This was a bigbee_pings broadcast from lodena_minax to gooniversity at 2024-09-18 20:29:54.423840 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "WTF 0 - Imperium Basics\n\nNot sure what SIGs are? Still trying to get mumble to work? Lost your blingy ship in a strat op and now you’re spacebroke? Then join this fleet to find out all the essentials to function in the Imperium.",
                fleetCommander = FleetCommander("Lodena Minax", 5),
                fleet = "WTF 0",
                formupLocations = listOf(FormupLocation.Text("Theory only, join from anywhere.")),
                papType = PapType.Text("SIG/SQUAD"),
                comms = Comms.Text("Gooniversity Discord -> The Big Classroom - https://discord.gg/gooniversity"),
                doctrine = Doctrine("None - This is a theory class", null),
                broadcastSource = "bigbee_pings",
                target = "gooniversity",
            ),
        ),
        """
            Fleet up on Asher
            
            FC: Asher Elias
            Formup Location: 49-U / 4-07
            
            ~~~ This was a broadcast from asher_elias to discord at 2024-01-31 00:17:02.533642 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "Fleet up on Asher",
                fleetCommander = FleetCommander("Asher Elias", 4),
                fleet = null,
                formupLocations = listOf(FormupLocation.System(100000005), FormupLocation.System(100000006)),
                papType = null,
                comms = null,
                doctrine = null,
                broadcastSource = null,
                target = "discord",
            ),
        ),
        """
            Sub cap move op! Departing 0900 on the dot
            
            FC Name: Asher Elias
            Formup Location: UALX-3, 0SHT
            ~~~ This was a coord broadcast from nyx_viliana to all at 2025-05-27 08:43:11.912502 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "Sub cap move op! Departing 0900 on the dot",
                fleetCommander = FleetCommander("Asher Elias", 4),
                fleet = null,
                formupLocations = listOf(FormupLocation.System(100000002), FormupLocation.System(100000003)),
                papType = null,
                comms = null,
                doctrine = null,
                broadcastSource = "coord",
                target = "all",
            ),
        ),
        """
            Saturday night brawl seems to be brewing. We're not fighting over anything important so no one will probably overcommit, let's goooo 

            FC:Asher Elias
            Op1
            Raven Navy
            0SHT
            
            ~~~ This was a broadcast from asher_elias to all at 2025-06-01 00:41:13.573521 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "Saturday night brawl seems to be brewing. We're not fighting over anything important so no one will probably overcommit, let's goooo\n\nOp1\nRaven Navy\n0SHT",
                fleetCommander = FleetCommander("Asher Elias", 4),
                fleet = null,
                formupLocations = listOf(FormupLocation.System(100000003)),
                papType = null,
                comms = null,
                doctrine = null,
                broadcastSource = null,
                target = "all",
            ),
        ),
        """
            N3- to X2 move op is still running. Join the fleet and read the MOTD for instructions.

            FC Name: Asher Elias
            Pap Type: None
            Comms: None
            
            ~~~ This was a coord broadcast from nyx_viliana to all at 2025-06-01 10:03:49.397481 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "N3- to X2 move op is still running. Join the fleet and read the MOTD for instructions.",
                fleetCommander = FleetCommander("Asher Elias", 4),
                fleet = null,
                formupLocations = listOf(),
                papType = null,
                comms = Comms.Text("None"),
                doctrine = null,
                broadcastSource = "coord",
                target = "all",
            ),
        ),
        """
            Let's go goons first 3 fleets

            FC Name: Arkadios Sol
            Formup Location: UALX-3
            PAP Type: Strategic
            Comms: Op 1 https://gnf.lt/dYehZh9.html
            Doctrine: Vultures (Booster > Basi > Vulture > FNI > Onyx > Lachesis/Huginn > Svipul > Else)

            FC Name: Mist Amatin
            Formup Location: UALX-3
            PAP Type: Strategic
            Comms: Op 2 https://gnf.lt/vLwgoyY.html
            Doctrine: Vultures (Booster > Basi > Vulture > FNI > Onyx > Lachesis/Huginn > Svipul > Else)

            FC Name: Lodena Minax
            Formup Location: UALX-3
            PAP Type: Strategic
            Comms: Op 3 https://gnf.lt/NOH1FNH.html
            Doctrine: Harpy Fleet (Boosters > Kirin/Scalpel > Harpy > Else) READ MOTD! READ MOTD! READ MOTD!
            
            ~~~ This was a broadcast from zintage_enaka to all at 2025-06-04 17:29:26.865509 EVE ~~~
        """.trimIndent() to listOf(
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "Let's go goons first 3 fleets",
                fleetCommander = FleetCommander("Arkadios Sol", 6),
                fleet = null,
                formupLocations = listOf(FormupLocation.System(100000002)),
                papType = PapType.Strategic,
                comms = Comms.Mumble("Op 1", "https://gnf.lt/dYehZh9.html"),
                doctrine = Doctrine("Vultures (Booster > Basi > Vulture > FNI > Onyx > Lachesis/Huginn > Svipul > Else)", "https://goonfleet.com/index.php/topic/369029-active-strat-vultures/"),
                broadcastSource = null,
                target = "all",
            ),
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "",
                fleetCommander = FleetCommander("Mist Amatin", 3),
                fleet = null,
                formupLocations = listOf(FormupLocation.System(100000002)),
                papType = PapType.Strategic,
                comms = Comms.Mumble("Op 2", "https://gnf.lt/vLwgoyY.html"),
                doctrine = Doctrine("Vultures (Booster > Basi > Vulture > FNI > Onyx > Lachesis/Huginn > Svipul > Else)", "https://goonfleet.com/index.php/topic/369029-active-strat-vultures/"),
                broadcastSource = null,
                target = "all",
            ),
            PingModel.FleetPing(
                timestamp = timestamp,
                sourceText = "",
                description = "",
                fleetCommander = FleetCommander("Lodena Minax", 5),
                fleet = null,
                formupLocations = listOf(FormupLocation.System(100000002)),
                papType = PapType.Strategic,
                comms = Comms.Mumble("Op 3", "https://gnf.lt/NOH1FNH.html"),
                doctrine = Doctrine("Harpy Fleet (Boosters > Kirin/Scalpel > Harpy > Else) READ MOTD! READ MOTD! READ MOTD!", "https://goonfleet.com/index.php/topic/346057-active-strat-harpyfleet/"),
                broadcastSource = null,
                target = "all",
            ),
        ),
    ).forEachIndexed { index, (text, expected) ->
        "ping $index is parsed correctly" {
            val actual = target(timestamp, text)

            actual.shouldHaveSize(expected.size)
            actual.forEachIndexed { index, actualPing ->
                val expectedPing = expected[index]
                actualPing.shouldBeEqualToComparingFields(
                    other = expectedPing,
                    fieldsEqualityCheckConfig = FieldsEqualityCheckConfig(
                        propertiesToExclude = listOf(
                            PingModel.FleetPing::sourceText,
                            PingModel.PlainText::sourceText,
                        ),
                    ),
                )
            }
        }
    }
})
