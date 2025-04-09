package dev.nohus.rift.configurationpack

import dev.nohus.rift.settings.persistence.ConfigurationPack

val ConfigurationPack?.displayName: String get() = when (this) {
    ConfigurationPack.Imperium -> "帝国系通用"
    ConfigurationPack.TheInitiative -> "The Initiative."
    null -> "默认"
}
