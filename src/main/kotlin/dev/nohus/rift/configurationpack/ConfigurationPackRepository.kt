package dev.nohus.rift.configurationpack

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.ConfigurationPack.Imperium
import dev.nohus.rift.settings.persistence.ConfigurationPack.TheInitiative
import dev.nohus.rift.settings.persistence.IntelChannel
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

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
            .mapNotNull { it.info.success?.allianceId }
            .toSet()
        return ConfigurationPack.entries.firstOrNull { pack ->
            getPackMemberAllianceIds(pack).any { it in characterAlliances }
        }
    }

    private fun getPackMemberAllianceIds(pack: ConfigurationPack?): List<Int> {
        return when (pack) {
            Imperium -> listOf(
                99003214, // Brave Collective
                99009163, // Dracarys.
                99012042, // Fanatic Legion.
                150097440, // Get Off My Lawn
                1354830081, // Goonswarm Federation
                131511956, // Tactical Narcotics Team
                99003995, // Invidia Gloriae Comes
                99009331, // Scumlords
                99010140, // Stribog Clade
                99011162, // Shadow Ultimatum
                99011223, // Sigma Grindset
                99010931, // WE FORM BL0B
            )
            TheInitiative -> listOf(
                1900696668, // The Initiative.
            )
            null -> emptyList()
        }
    }

    fun getSuggestedIntelChannels(): SuggestedIntelChannels? {
        return when (settings.configurationPack) {
            Imperium -> SuggestedIntelChannels(
                promptTitleText = "您想要自动配置帝国系通用的预警频道吗？",
                promptButtonText = "添加帝国系频道",
                channels = listOf(
                    IntelChannel("aridia.imperium", "Aridia"),
                    IntelChannel("curse.imperium", "Curse"),
                    IntelChannel("east.imperium", "Catch"),
                    IntelChannel("east.imperium", "Immensea"),
                    IntelChannel("east.imperium", "Impass"),
                    IntelChannel("east.imperium", "Tenerifis"),
                    IntelChannel("ftn.imperium", "Fountain"),
                    IntelChannel("khanid.imperium", "Khanid"),
                    IntelChannel("southeast.imperium", "Esoteria"),
                    IntelChannel("southeast.imperium", "Feythabolis"),
                    IntelChannel("southeast.imperium", "Paragon Soul"),
                    IntelChannel("triangle.imperium", "Pochven"),
                    IntelChannel("west.imperium", "Delve"),
                    IntelChannel("west.imperium", "Period Basis"),
                    IntelChannel("west.imperium", "Querious"),
                ),
            )
            TheInitiative -> SuggestedIntelChannels(
                promptTitleText = "您想要自动配置 The Initiative. 的预警频道吗？",
                promptButtonText = "添加 Init 频道",
                channels = listOf(
                    IntelChannel("I. Ftn Intel", "Fountain"),
                    IntelChannel("I. OR Intel", "Outer Ring"),
                    IntelChannel("I. Aridia Intel", "Aridia"),
                    IntelChannel("I. Curse Intel", "Curse"),
                    IntelChannel("I. Poch Intel", "Pochven"),
                    IntelChannel("I. C Ring Intel", "Cloud Ring"),
                ),
            )
            null -> null
        }
    }

    fun isJabberEnabled(): Boolean {
        return when (settings.configurationPack) {
            Imperium -> true
            TheInitiative -> false
            null -> false
        }
    }

    fun getJumpBridgeNetworkUrl(): String? {
        return when (settings.configurationPack) {
            Imperium -> "https://wiki.goonswarm.org/w/Alliance:Stargate"
            TheInitiative -> null
            null -> null
        }
    }
}
