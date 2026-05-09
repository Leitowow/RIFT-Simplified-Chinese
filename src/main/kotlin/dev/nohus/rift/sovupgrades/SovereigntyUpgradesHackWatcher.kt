package dev.nohus.rift.sovupgrades

import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.notifications.NotificationsController
import dev.nohus.rift.notifications.NotificationsController.Notification.SovereigntyUpgradeImportNotification
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.get
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class SovereigntyUpgradesHackWatcher(
    private val settings: Settings,
    private val clipboard: Clipboard,
    private val typesRepository: TypesRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val sovereigntyUpgradesRepository: SovereigntyUpgradesRepository,
    private val notificationsController: NotificationsController,
) {

    companion object {
        private const val HEADER_PREFIX = "Sovereignty Hub "
        private val upgradeRegex = """^\d+\t(?<upgrade>.+)\t(?<status>Online|Offline)$""".toRegex()
    }

    data class SovereigntyUpgrade(val type: Type, val isOnline: Boolean)

    suspend fun start() = coroutineScope {
        clipboard.state.filterNotNull().collect { text ->
            if (!settings.isSovereigntyUpgradesHackImportingEnabled) return@collect
            if (!text.startsWith(HEADER_PREFIX)) return@collect
            val lines = text.lines().filter { it.isNotBlank() }
            if (lines.size < 2) return@collect
            val systemName = lines[0].removePrefix(HEADER_PREFIX)
            val system = solarSystemsRepository.getSystem(systemName) ?: return@collect
            val upgrades = lines.mapNotNull {
                val result = upgradeRegex.find(it) ?: return@mapNotNull null
                val type = typesRepository.getType(result["upgrade"]) ?: return@mapNotNull null
                val isOnline = result["status"] == "Online"
                SovereigntyUpgrade(type, isOnline)
            }
            if (upgrades.isNotEmpty()) {
                logger.info { "System: ${system.name}, Upgrades: ${upgrades.map { it.type.name }}" }
                val importedUpgrades = upgrades.mapNotNull {
                    if (!it.isOnline && !settings.isSovereigntyUpgradesHackImportingOfflineEnabled) return@mapNotNull null
                    it.type
                }
                if (importedUpgrades.isNotEmpty()) {
                    val notification = SovereigntyUpgradeImportNotification(system, importedUpgrades)
                    notificationsController.show(notification)
                    sovereigntyUpgradesRepository.setUpgrades(system, importedUpgrades)
                }
            }
        }
    }
}
