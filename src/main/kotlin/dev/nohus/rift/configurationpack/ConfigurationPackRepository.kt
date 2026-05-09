package dev.nohus.rift.configurationpack

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.ConfigurationPack.Imperium
import dev.nohus.rift.settings.persistence.ConfigurationPack.PhoenixCoalition
import dev.nohus.rift.settings.persistence.ConfigurationPack.TheInitiative
import dev.nohus.rift.settings.persistence.IntelChannel
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.time.LocalDate

@Single
class ConfigurationPackRepository(
    private val settings: Settings,
    private val localCharactersRepository: LocalCharactersRepository,
) {
    data class SuggestedIntelChannels(
        val promptTitleText: String,
        val promptButtonText: String,
        val channels: List<IntelChannel>,
    )

    fun set(configurationPack: ConfigurationPack?) {
        if (settings.configurationPack != configurationPack) {
            settings.configurationPack = configurationPack
            settings.isConfigurationPackReminderDismissed = false
        }
    }

    fun getSuggestedPack(): ConfigurationPack? {
        val characterAlliances = localCharactersRepository.characters.value
            .mapNotNull { it.info?.allianceId }
            .toSet()
        return ConfigurationPack.entries.firstOrNull { pack ->
            getPackMemberAllianceIds(pack).any { it in characterAlliances }
        }
    }

    private fun getPackMemberAllianceIds(pack: ConfigurationPack?): List<Int> {
        return when (pack) {
            Imperium -> listOf(
                1354830081, // Goonswarm Federation
                99003214, // Brave Collective
                99010079, // Brave United
                99013363, // Dracarys Wing
                99009163, // Dracarys.
                99012042, // Fanatic Legion.
                99003995, // Invidia Gloriae Comes
                99011239, // Ligma Grindset
                99013568, // S0B Citizens Alliance
                99001969, // SONS of BANE
                99009331, // Scumlords
                99011162, // Shadow Ultimatum
                99011223, // Sigma Grindset
                131511956, // Tactical Narcotics Team
                99010877, // Out of the Blue.
            )

            TheInitiative -> listOf(
                1900696668, // The Initiative.
            )

            PhoenixCoalition -> listOf(
                99002685, // Synergy of Steel
                741557221, // Razor Alliance
                99001317, // Banderlogs Alliance
                99010281, // GameTheory
                99012770, // Black Rose.
                99005274, // La Ligue des mondes libres
                99012040, // Regnum Astera
                99013231, // Blood Drive
                99013216, // Nomad Alliance
                154104258, // Apocalypse Now.
                99010896, // Caldari Alliance
                99013539, // The Disciples of Space Piracy
                99013456, // Northern Frontier Group
                99013759, // Imurukka Conglomerate
                99012410, // DECOY
            )

            null -> emptyList()
        }
    }

    fun getSuggestedIntelChannels(): SuggestedIntelChannels? {
        return when (settings.configurationPack) {
            Imperium -> SuggestedIntelChannels(
                promptTitleText = "Would you like intel channels of the Imperium to be configured automatically?",
                promptButtonText = "Add Imperium channels",
                channels = listOf(
                    IntelChannel("east.imperium", "Catch"),
                    IntelChannel("east.imperium", "Immensea"),
                    IntelChannel("east.imperium", "Impass"),
                    IntelChannel("east.imperium", "Tenerifis"),
                    IntelChannel("fareast.imperium", "Detorid"),
                    IntelChannel("fareast.imperium", "Wicked Creek"),
                    IntelChannel("fareast.imperium", "Insmother"),
                    IntelChannel("fareast.imperium", "Cache"),
                    IntelChannel("fareast.imperium", "Scalding Pass"),
                    IntelChannel("west.imperium", "Delve"),
                    IntelChannel("west.imperium", "Querious"),
                    IntelChannel("west.imperium", "Period Basis"),
                    IntelChannel("southeast.imperium", "Esoteria"),
                    IntelChannel("southeast.imperium", "Feythabolis"),
                    IntelChannel("southeast.imperium", "Paragon Soul"),
                    IntelChannel("aridia.imperium", "Aridia"),
                    IntelChannel("curse.imperium", "Curse"),
                    IntelChannel("ftn.imperium", "Fountain"),
                    IntelChannel("khanid.imperium", "Khanid"),
                    IntelChannel("triangle.imperium", "Pochven"),
                ),
            )

            TheInitiative -> SuggestedIntelChannels(
                promptTitleText = "Would you like intel channels of The Initiative. to be configured automatically?",
                promptButtonText = "Add Init channels",
                channels = listOf(
                    IntelChannel("I. Ftn Intel", "Fountain"),
                    IntelChannel("I. OR Intel", "Outer Ring"),
                    IntelChannel("I. Aridia Intel", "Aridia"),
                    IntelChannel("I. Curse Intel", "Curse"),
                    IntelChannel("I. Poch Intel", "Pochven"),
                    IntelChannel("I. C Ring Intel", "Cloud Ring"),
                ),
            )

            PhoenixCoalition -> SuggestedIntelChannels(
                promptTitleText = "Would you like intel channels of the Phoenix Coalition to be configured automatically?",
                promptButtonText = "Add Phoenix Coalition channels",
                channels = listOf(
                    IntelChannel("wc.Venal+Br+Te", "Venal"),
                    IntelChannel("wc.Venal+Br+Te", "Branch"),
                    IntelChannel("wc.Venal+Br+Te", "Tenal"),
                ),
            )

            null -> null
        }
    }

    fun isJabberEnabled(): Boolean {
        return when (settings.configurationPack) {
            Imperium -> true
            TheInitiative -> false
            PhoenixCoalition -> false
            null -> false
        }
    }

    sealed class JumpBridgesReference(open val packName: String) {
        data class Url(override val packName: String, val url: String) : JumpBridgesReference(packName)
        data class Text(override val packName: String, val text: String, val date: LocalDate) :
            JumpBridgesReference(packName)
    }

    fun getJumpBridges(): JumpBridgesReference? {
        return when (settings.configurationPack) {
            Imperium -> JumpBridgesReference.Url(
                packName = "The Imperium",
                url = "https://wiki.goonswarm.org/w/Alliance:Stargate",
            )
            TheInitiative -> null
            PhoenixCoalition -> JumpBridgesReference.Text(
                packName = "Phoenix Coalition",
                text = """
                VI2K-J -> H-NOU5
                H-NOU5 -> VI2K-J
                MA-VDX -> BKG-Q2
                MC6O-F -> E-D0VZ
                E-D0VZ -> MC6O-F
                6-AOLS -> ZH3-BS
                P3EN-E -> 4GYV-Q
                4GYV-Q -> P3EN-E
                MA-XAP -> 2DWM-2
                2DWM-2 -> MA-XAP
                0R-F2F -> WBR5-R
                WBR5-R -> 0R-F2F
                E9KD-N -> P-2TTL
                DAYP-G -> 3HX-DL
                3HX-DL -> DAYP-G
                YMJG-4 -> C2X-M5
                C2X-M5 -> YMJG-4
                1W-0KS -> FMBR-8
                FMBR-8 -> 1W-0KS
                BWI1-9 -> ZJ-QOO
                15W-GC -> UMI-KK
                UMI-KK -> 15W-GC
                S-EVIQ -> S-B7IT
                S-B7IT -> S-EVIQ
                V0DF-2 -> YLS8-J
                3T7-M8 -> E3UY-6
                IFJ-EL -> FA-DMO
                FA-DMO -> IFJ-EL
                ZXA-V6 -> EOY-BG
                FH-TTC -> VORM-W
                7-K5EL -> 4-HWWF
                4-HWWF -> 7-K5EL
                N2IS-B -> A4L-A2
                A4L-A2 -> N2IS-B
                IPAY-2 -> 0J3L-V
                0J3L-V -> IPAY-2
                DL1C-E -> GIH-ZG
                GIH-ZG -> DL1C-E
                T-ZWA1 -> 8TPX-N
                8TPX-N -> T-ZWA1
                B8O-KJ -> C-4ZOS
                C-4ZOS -> B8O-KJ
                0M-103 -> I1-BE8
                WW-OVQ -> POQP-K
                POQP-K -> WW-OVQ
                IMK-K1 -> PM-DWE
                PM-DWE -> IMK-K1
                W-4FA9 -> 8-4GQM
                8-4GQM -> W-4FA9
                G-LOIT -> IR-DYY
                AP9-LV -> Q-EHMJ
                Q-EHMJ -> AP9-LV
                H-EY0P -> P-E9GN
                MSHD-4 -> Q-CAB2
                Q-CAB2 -> MSHD-4
                5T-KM3 -> A3-RQ3
                A3-RQ3 -> 5T-KM3
                WH-2EZ -> EOA-ZC
                FIO1-8 -> Y-C3EQ
                GW7P-8 -> NV-3KA
                NV-3KA -> GW7P-8
                47L-J4 -> 05R-7A
                05R-7A -> 47L-J4
                M-MD31 -> H-5GUI
                H-5GUI -> M-MD31
                Y-1918 -> RO90-H
                RO90-H -> Y-1918
                """.trimIndent(),
                date = LocalDate.of(2026, 2, 21),
            )

            null -> null
        }
    }

    fun getSovereigntyUpgradesUrl(): String? {
        return when (settings.configurationPack) {
            Imperium -> "https://goonfleet.com/index.php/topic/371770-equinox-upgrade-information-station/"
            TheInitiative -> null
            PhoenixCoalition -> null
            null -> null
        }
    }
}
