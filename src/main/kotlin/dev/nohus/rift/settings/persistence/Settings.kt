package dev.nohus.rift.settings.persistence

import dev.nohus.rift.alerts.Alert
import dev.nohus.rift.standings.StandingsRepository.Standings
import dev.nohus.rift.utils.Pos
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import kotlin.io.path.pathString

@Single
class Settings(
    private val persistence: SettingsPersistence,
) {
    private var model = persistence.load()
    private val _updateFlow = MutableStateFlow(model)
    val updateFlow = _updateFlow.asStateFlow()

    private fun update(update: SettingsModel.() -> SettingsModel) {
        val newModel = model.update()
        if (newModel != model) {
            model = newModel
            _updateFlow.tryEmit(newModel)
            persistence.save(newModel)
        }
    }

    var eveLogsDirectory: Path?
        get() = model.eveLogsDirectory?.let { Path.of(it) }
        set(value) = update { copy(eveLogsDirectory = value?.pathString) }

    var eveSettingsDirectory: Path?
        get() = model.eveSettingsDirectory?.let { Path.of(it) }
        set(value) = update { copy(eveSettingsDirectory = value?.pathString) }

    var intelMap: IntelMap
        get() = model.intelMap
        set(value) = update { copy(intelMap = value) }

    var authenticatedCharacters: Map<Int, SsoAuthentication>
        get() = model.authenticatedCharacters
        set(value) = update { copy(authenticatedCharacters = value) }

    var intelChannels: List<IntelChannel>
        get() = model.intelChannels
        set(value) = update { copy(intelChannels = value) }

    var isRememberOpenWindows: Boolean
        get() = model.isRememberOpenWindows
        set(value) = update { copy(isRememberOpenWindows = value) }

    var isRememberWindowPlacement: Boolean
        get() = model.isRememberWindowPlacement
        set(value) = update { copy(isRememberWindowPlacement = value) }

    var openWindows: Set<RiftWindow>
        get() = model.openWindows
        set(value) = update { copy(openWindows = value) }

    var windowSettings: Map<RiftWindow, List<WindowSettings>>
        get() = model.windowSettings
        set(value) = update { copy(windowSettings = value) }

    var notificationEditPosition: Pos?
        get() = model.notificationEditPosition
        set(value) = update { copy(notificationEditPosition = value) }

    var notificationPosition: Pos?
        get() = model.notificationPosition
        set(value) = update { copy(notificationPosition = value) }

    var alerts: List<Alert>
        get() = model.alerts
        set(value) = update { copy(alerts = value) }

    var isSetupWizardFinished: Boolean
        get() = model.isSetupWizardFinished
        set(value) = update { copy(isSetupWizardFinished = value) }

    var isShowSetupWizardOnNextStart: Boolean
        get() = model.isShowSetupWizardOnNextStart
        set(value) = update { copy(isShowSetupWizardOnNextStart = value) }

    var isDemoMode: Boolean
        get() = model.isDemoMode
        set(value) = update { copy(isDemoMode = value) }

    var isDisplayEveTime: Boolean
        get() = model.isDisplayEveTime
        set(value) = update { copy(isDisplayEveTime = value) }

    var isShowIskCents: Boolean
        get() = model.isShowIskCents
        set(value) = update { copy(isShowIskCents = value) }

    val displayTimeZone: ZoneId
        get() = if (model.isDisplayEveTime) ZoneId.of("UTC") else ZoneId.systemDefault()

    var jabberJidLocalPart: String?
        get() = model.jabberJidLocalPart
        set(value) = update { copy(jabberJidLocalPart = value) }

    var jabberPassword: String?
        get() = model.jabberPassword
        set(value) = update { copy(jabberPassword = value) }

    var jabberCollapsedGroups: List<String>
        get() = model.jabberCollapsedGroups
        set(value) = update { copy(jabberCollapsedGroups = value) }

    var jabberIsUsingBiggerFontSize: Boolean
        get() = model.jabberIsUsingBiggerFontSize
        set(value) = update { copy(jabberIsUsingBiggerFontSize = value) }

    var jabberConferences: List<String>
        get() = model.jabberConferences
        set(value) = update { copy(jabberConferences = value) }

    var isSettingsReadFailure: Boolean
        get() = model.isSettingsReadFailure
        set(value) = update { copy(isSettingsReadFailure = value) }

    var isUsingDarkTrayIcon: Boolean
        get() = model.isUsingDarkTrayIcon
        set(value) = update { copy(isUsingDarkTrayIcon = value) }

    var intelReports: IntelReports
        get() = model.intelReports
        set(value) = update { copy(intelReports = value) }

    var intelFeed: IntelFeed
        get() = model.intelFeed
        set(value) = update { copy(intelFeed = value) }

    var soundsVolume: Int
        get() = model.soundsVolume
        set(value) = update { copy(soundsVolume = value) }

    var alertGroups: Set<String>
        get() = model.alertGroups
        set(value) = update { copy(alertGroups = value) }

    var configurationPack: ConfigurationPack?
        get() = model.configurationPack
        set(value) = update { copy(configurationPack = value) }

    var isConfigurationPackReminderDismissed: Boolean
        get() = model.isConfigurationPackReminderDismissed
        set(value) = update { copy(isConfigurationPackReminderDismissed = value) }

    var hiddenCharacterIds: List<Int>
        get() = model.hiddenCharacterIds
        set(value) = update { copy(hiddenCharacterIds = value) }

    var jumpBridgeNetwork: Map<String, String>?
        get() = model.jumpBridgeNetwork
        set(value) = update { copy(jumpBridgeNetwork = value) }

    var isUsingRiftAutopilotRoute: Boolean
        get() = model.isUsingRiftAutopilotRoute
        set(value) = update { copy(isUsingRiftAutopilotRoute = value) }

    var isSettingAutopilotToAll: Boolean
        get() = model.isSettingAutopilotToAll
        set(value) = update { copy(isSettingAutopilotToAll = value) }

    var whatsNewVersion: String?
        get() = model.whatsNewVersion
        set(value) = update { copy(whatsNewVersion = value) }

    var jumpRange: JumpRange?
        get() = model.jumpRange
        set(value) = update { copy(jumpRange = value) }

    var selectedPlanetTypes: List<Int>
        get() = model.selectedPlanetTypes
        set(value) = update { copy(selectedPlanetTypes = value) }

    var selectedSovereigntyUpgradeTypes: List<Int>
        get() = model.selectedSovereigntyUpgradeTypes
        set(value) = update { copy(selectedSovereigntyUpgradeTypes = value) }

    var installationId: String?
        get() = model.installationId
        set(value) = update { copy(installationId = value) }

    var isShowingSystemDistance: Boolean
        get() = model.isShowingSystemDistance
        set(value) = update { copy(isShowingSystemDistance = value) }

    var isUsingJumpBridgesForDistance: Boolean
        get() = model.isUsingJumpBridgesForDistance
        set(value) = update { copy(isUsingJumpBridgesForDistance = value) }

    var intelExpireSeconds: Int
        get() = model.intelExpireSeconds
        set(value) = update { copy(intelExpireSeconds = value) }

    var standings: Standings
        get() = model.standings
        set(value) = update { copy(standings = value) }

    var planetaryIndustry: PlanetaryIndustry
        get() = model.planetaryIndustry
        set(value) = update { copy(planetaryIndustry = value) }

    var isShowingCharactersClones: Boolean
        get() = model.isShowingCharactersClones
        set(value) = update { copy(isShowingCharactersClones = value) }

    var planetaryIndustryTriggeredAlerts: Map<String, Map<String, Long>>
        get() = model.planetaryIndustryTriggeredAlerts
        set(value) = update { copy(planetaryIndustryTriggeredAlerts = value) }

    var pushover: Pushover
        get() = model.pushover
        set(value) = update { copy(pushover = value) }

    var ntfy: Ntfy
        get() = model.ntfy
        set(value) = update { copy(ntfy = value) }

    val skipSplashScreen: Boolean
        get() = model.skipSplashScreen

    var dismissedWarnings: List<String>
        get() = model.dismissedWarnings
        set(value) = update { copy(dismissedWarnings = value) }

    var uiScale: Float
        get() = model.uiScale
        set(value) = update { copy(uiScale = value) }

    var accountAssociations: Map<Int, Int>
        get() = model.accountAssociations
        set(value) = update { copy(accountAssociations = value) }

    var isTrayIconWorking: Boolean
        get() = model.isTrayIconWorking
        set(value) = update { copy(isTrayIconWorking = value) }

    var isWindowTransparencyEnabled: Boolean
        get() = model.isWindowTransparencyEnabled
        set(value) = update { copy(isWindowTransparencyEnabled = value) }

    var windowTransparencyModifier: Float
        get() = model.windowTransparencyModifier
        set(value) = update { copy(windowTransparencyModifier = value) }

    var isSmartAlwaysAbove: Boolean
        get() = model.isSmartAlwaysAbove
        set(value) = update { copy(isSmartAlwaysAbove = value) }

    var mapMarkers: List<MapMarker>
        get() = model.mapMarkers
        set(value) = update { copy(mapMarkers = value) }

    var assetLocationPins: Map<Long, LocationPinStatus>
        get() = model.assetLocationPins
        set(value) = update { copy(assetLocationPins = value) }

    var assetLocationCustomNames: Map<Long, String>
        get() = model.assetLocationCustomNames
        set(value) = update { copy(assetLocationCustomNames = value) }

    var isJukeboxRevealed: Boolean
        get() = model.isJukeboxRevealed
        set(value) = update { copy(isJukeboxRevealed = value) }

    var sovereigntyUpgrades: Map<String, List<Int>>
        get() = model.sovereigntyUpgrades
        set(value) = update { copy(sovereigntyUpgrades = value) }

    var isSovereigntyUpgradesHackImportingEnabled: Boolean
        get() = model.isSovereigntyUpgradesHackImportingEnabled
        set(value) = update { copy(isSovereigntyUpgradesHackImportingEnabled = value) }

    var isSovereigntyUpgradesHackImportingOfflineEnabled: Boolean
        get() = model.isSovereigntyUpgradesHackImportingOfflineEnabled
        set(value) = update { copy(isSovereigntyUpgradesHackImportingOfflineEnabled = value) }

    var preferredExternalServices: List<ExternalService>
        get() = model.preferredExternalServices2
        set(value) = update { copy(preferredExternalServices2 = value) }

    var corpWalletDivisionNames: Map<Int, Map<Int, String>>
        get() = model.corpWalletDivisionNames
        set(value) = update { copy(corpWalletDivisionNames = value) }

    var newVersionSeenTimestamp: Instant?
        get() = model.newVersionSeenTimestamp?.let { Instant.ofEpochMilli(it) }
        set(value) = update { copy(newVersionSeenTimestamp = value?.toEpochMilli()) }

    var characterPortraits: CharacterPortraits
        get() = model.characterPortraits
        set(value) = update { copy(characterPortraits = value) }

    var isZkillboardMonitoringEnabled: Boolean
        get() = model.isZkillboardMonitoringEnabled
        set(value) = update { copy(isZkillboardMonitoringEnabled = value) }

    var manufacturingAssistant: ManufacturingAssistant
        get() = model.manufacturingAssistant
        set(value) = update { copy(manufacturingAssistant = value) }
}
