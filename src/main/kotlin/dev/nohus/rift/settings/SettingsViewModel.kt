package dev.nohus.rift.settings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.files.DetectEveSettingsDirectoryUseCase
import dev.nohus.rift.characters.files.GetEveCharactersSettingsUseCase
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.configurationpack.ConfigurationPackRepository.JumpBridgesReference
import dev.nohus.rift.configurationpack.ConfigurationPackRepository.SuggestedIntelChannels
import dev.nohus.rift.logs.DetectLogsDirectoryUseCase
import dev.nohus.rift.logs.GetChatLogsDirectoryUseCase
import dev.nohus.rift.logs.MatchChatLogFilenameUseCase
import dev.nohus.rift.repositories.JumpBridgesRepository
import dev.nohus.rift.repositories.JumpBridgesRepository.JumpBridgeConnection
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.GetStorageStatsUseCase.StorageStats
import dev.nohus.rift.settings.persistence.CharacterPortraits
import dev.nohus.rift.settings.persistence.CharacterPortraitsParallaxStrength
import dev.nohus.rift.settings.persistence.CharacterPortraitsStandingsTargets
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.IntelChannel
import dev.nohus.rift.settings.persistence.IntelMap
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.sovupgrades.SovereigntyUpgradesRepository
import dev.nohus.rift.utils.Pos
import dev.nohus.rift.utils.openFileManager
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

@Factory
class SettingsViewModel(
    @InjectedParam private val inputModel: SettingsInputModel,
    private val settings: Settings,
    private val detectLogsDirectoryUseCase: DetectLogsDirectoryUseCase,
    private val detectEveSettingsDirectoryUseCase: DetectEveSettingsDirectoryUseCase,
    private val getChatLogsDirectoryUseCase: GetChatLogsDirectoryUseCase,
    private val matchChatLogFilenameUseCase: MatchChatLogFilenameUseCase,
    private val getEveCharactersSettingsUseCase: GetEveCharactersSettingsUseCase,
    private val configurationPackRepository: ConfigurationPackRepository,
    solarSystemsRepository: SolarSystemsRepository,
    private val windowManager: WindowManager,
    private val clipboard: Clipboard,
    private val jumpBridgesParser: JumpBridgesParser,
    private val jumpBridgesRepository: JumpBridgesRepository,
    private val sovereigntyUpgradesParser: SovereigntyUpgradesParser,
    private val sovereigntyUpgradesRepository: SovereigntyUpgradesRepository,
    private val getStorageStatsUseCase: GetStorageStatsUseCase,
) : ViewModel() {

    data class UiState(
        val selectedTab: SettingsTab,
        val intelChannels: List<IntelChannel>,
        val suggestedIntelChannels: SuggestedIntelChannels?,
        val autocompleteIntelChannels: List<String> = emptyList(),
        val regions: List<String>,
        val isShowingSystemDistance: Boolean,
        val isUsingJumpBridgesForDistance: Boolean,
        val intelExpireSeconds: Int,
        val logsDirectory: String,
        val isLogsDirectoryValid: Boolean,
        val settingsDirectory: String,
        val isSettingsDirectoryValid: Boolean,
        val isDisplayEveTime: Boolean,
        val isShowSetupWizardOnNextStartEnabled: Boolean,
        val isRememberOpenWindows: Boolean,
        val isRememberWindowPlacement: Boolean,
        val isEditNotificationWindowOpen: Boolean = false,
        val notificationEditPlacement: Pos? = null,
        val isUsingDarkTrayIcon: Boolean,
        val isShowIskCents: Boolean,
        val isSmartAlwaysAbove: Boolean,
        val soundsVolume: Int,
        val configurationPack: ConfigurationPack?,
        val dialogMessage: DialogMessage? = null,
        val uiScale: Float,
        val isWindowTransparencyEnabled: Boolean,
        val windowTransparencyModifier: Float,
        val characterPortraits: CharacterPortraits,
        val isZkillboardMonitoringEnabled: Boolean,
        val storageStats: StorageStats? = null,
        // Map
        val intelMap: IntelMap,
        val isUsingRiftAutopilotRoute: Boolean,
        val jumpBridgeNetwork: List<JumpBridgeConnection>,
        val jumpBridgeCopyState: JumpBridgeCopyState,
        val jumpBridgesReference: JumpBridgesReference?,
        val jumpBridgeSearchState: JumpBridgeSearchState,
        val isJumpBridgeSearchDialogShown: Boolean,
        val sovereigntyUpgradesCopyState: SovereigntyUpgradesCopyState,
        val sovereigntyUpgrades: Map<MapSolarSystem, List<Type>>,
        val sovereigntyUpgradesUrl: String?,
        // Sovereignty
        val isSovereigntyUpgradesHackImportingEnabled: Boolean,
        val isSovereigntyUpgradesHackImportingOfflineEnabled: Boolean,
    )

    sealed class SettingsTab(val id: Int) {
        data object General : SettingsTab(0)
        data object Intel : SettingsTab(1)
        data object Map : SettingsTab(2)
        data object Sovereignty : SettingsTab(3)
        data object Misc : SettingsTab(4)
    }

    sealed interface JumpBridgeCopyState {
        data object NotCopied : JumpBridgeCopyState
        data class Copied(val network: List<JumpBridgeConnection>) : JumpBridgeCopyState
    }

    sealed interface JumpBridgeSearchState {
        data object NotSearched : JumpBridgeSearchState
        data class Searching(val progress: Float, val connectionsCount: Int) : JumpBridgeSearchState
        data object SearchFailed : JumpBridgeSearchState
        data class SearchDone(val network: List<JumpBridgeConnection>) : JumpBridgeSearchState
    }

    sealed interface SovereigntyUpgradesCopyState {
        data object NotCopied : SovereigntyUpgradesCopyState
        data class Copied(val upgrades: Map<MapSolarSystem, List<Type>>) : SovereigntyUpgradesCopyState
    }

    private val _state = MutableStateFlow(
        UiState(
            selectedTab = when (inputModel) {
                SettingsInputModel.Normal -> SettingsTab.General
                SettingsInputModel.EveInstallation -> SettingsTab.General
                SettingsInputModel.IntelChannels -> SettingsTab.Intel
            },
            intelChannels = settings.intelChannels,
            suggestedIntelChannels = configurationPackRepository.getSuggestedIntelChannels(),
            regions = solarSystemsRepository.getKnownSpaceRegions().map { it.name }.sorted(),
            isShowingSystemDistance = settings.isShowingSystemDistance,
            isUsingJumpBridgesForDistance = settings.isUsingJumpBridgesForDistance,
            intelExpireSeconds = settings.intelExpireSeconds,
            logsDirectory = settings.eveLogsDirectory?.pathString ?: "",
            isLogsDirectoryValid = getChatLogsDirectoryUseCase(settings.eveLogsDirectory) != null,
            settingsDirectory = settings.eveSettingsDirectory?.pathString ?: "",
            isSettingsDirectoryValid = getEveCharactersSettingsUseCase(settings.eveSettingsDirectory).isNotEmpty(),
            isDisplayEveTime = settings.isDisplayEveTime,
            isShowSetupWizardOnNextStartEnabled = settings.isShowSetupWizardOnNextStart,
            isRememberOpenWindows = settings.isRememberOpenWindows,
            isRememberWindowPlacement = settings.isRememberWindowPlacement,
            notificationEditPlacement = settings.notificationEditPosition,
            isUsingDarkTrayIcon = settings.isUsingDarkTrayIcon,
            isShowIskCents = settings.isShowIskCents,
            isSmartAlwaysAbove = settings.isSmartAlwaysAbove,
            soundsVolume = settings.soundsVolume,
            configurationPack = settings.configurationPack,
            uiScale = settings.uiScale,
            isWindowTransparencyEnabled = settings.isWindowTransparencyEnabled,
            windowTransparencyModifier = settings.windowTransparencyModifier,
            characterPortraits = settings.characterPortraits,
            isZkillboardMonitoringEnabled = settings.isZkillboardMonitoringEnabled,
            // Map
            intelMap = settings.intelMap,
            isUsingRiftAutopilotRoute = settings.isUsingRiftAutopilotRoute,
            jumpBridgeNetwork = jumpBridgesRepository.getConnections(),
            jumpBridgeCopyState = JumpBridgeCopyState.NotCopied,
            jumpBridgesReference = configurationPackRepository.getJumpBridges(),
            jumpBridgeSearchState = JumpBridgeSearchState.NotSearched,
            isJumpBridgeSearchDialogShown = false,
            sovereigntyUpgradesCopyState = SovereigntyUpgradesCopyState.NotCopied,
            sovereigntyUpgrades = sovereigntyUpgradesRepository.upgrades.value,
            sovereigntyUpgradesUrl = configurationPackRepository.getSovereigntyUpgradesUrl(),
            // Sovereignty
            isSovereigntyUpgradesHackImportingEnabled = settings.isSovereigntyUpgradesHackImportingEnabled,
            isSovereigntyUpgradesHackImportingOfflineEnabled = settings.isSovereigntyUpgradesHackImportingOfflineEnabled,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            updateIntelChannelAutocomplete(settings.eveLogsDirectory)
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        intelChannels = settings.intelChannels,
                        suggestedIntelChannels = configurationPackRepository.getSuggestedIntelChannels(),
                        isShowingSystemDistance = settings.isShowingSystemDistance,
                        isUsingJumpBridgesForDistance = settings.isUsingJumpBridgesForDistance,
                        intelExpireSeconds = settings.intelExpireSeconds,
                        isDisplayEveTime = settings.isDisplayEveTime,
                        isShowSetupWizardOnNextStartEnabled = settings.isShowSetupWizardOnNextStart,
                        isRememberOpenWindows = settings.isRememberOpenWindows,
                        isRememberWindowPlacement = settings.isRememberWindowPlacement,
                        notificationEditPlacement = settings.notificationEditPosition,
                        isUsingDarkTrayIcon = settings.isUsingDarkTrayIcon,
                        isShowIskCents = settings.isShowIskCents,
                        isSmartAlwaysAbove = settings.isSmartAlwaysAbove,
                        soundsVolume = settings.soundsVolume,
                        configurationPack = settings.configurationPack,
                        uiScale = settings.uiScale,
                        isWindowTransparencyEnabled = settings.isWindowTransparencyEnabled,
                        windowTransparencyModifier = settings.windowTransparencyModifier,
                        characterPortraits = settings.characterPortraits,
                        isZkillboardMonitoringEnabled = settings.isZkillboardMonitoringEnabled,
                        // Map
                        intelMap = settings.intelMap,
                        isUsingRiftAutopilotRoute = settings.isUsingRiftAutopilotRoute,
                        // Sovereignty
                        isSovereigntyUpgradesHackImportingEnabled = settings.isSovereigntyUpgradesHackImportingEnabled,
                        isSovereigntyUpgradesHackImportingOfflineEnabled = settings.isSovereigntyUpgradesHackImportingOfflineEnabled,
                    )
                }
                val logsDirectory = settings.eveLogsDirectory
                if (logsDirectory?.pathString != _state.value.logsDirectory) {
                    updateIntelChannelAutocomplete(logsDirectory)
                    _state.update {
                        it.copy(
                            logsDirectory = logsDirectory?.pathString ?: "",
                            isLogsDirectoryValid = getChatLogsDirectoryUseCase(logsDirectory) != null,
                        )
                    }
                }
                val settingsDirectory = settings.eveSettingsDirectory
                if (settingsDirectory?.pathString != _state.value.settingsDirectory) {
                    _state.update {
                        it.copy(
                            settingsDirectory = settingsDirectory?.pathString ?: "",
                            isSettingsDirectoryValid = getEveCharactersSettingsUseCase(settingsDirectory).isNotEmpty(),
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            clipboard.state.filterNotNull().collect { text ->
                if (_state.value.selectedTab == SettingsTab.Sovereignty) {
                    val network = jumpBridgesParser.parse(text)
                    if (network.isNotEmpty()) {
                        _state.update { it.copy(jumpBridgeCopyState = JumpBridgeCopyState.Copied(network)) }
                    } else {
                        _state.update { it.copy(jumpBridgeCopyState = JumpBridgeCopyState.NotCopied) }
                    }

                    val sovUpgrades = sovereigntyUpgradesParser.parse(text)
                    if (sovUpgrades.isNotEmpty()) {
                        _state.update { it.copy(sovereigntyUpgradesCopyState = SovereigntyUpgradesCopyState.Copied(sovUpgrades)) }
                    } else {
                        _state.update { it.copy(sovereigntyUpgradesCopyState = SovereigntyUpgradesCopyState.NotCopied) }
                    }
                }
            }
        }
    }

    fun onTabSelected(tab: SettingsTab) {
        _state.update { it.copy(selectedTab = tab) }

        if (tab == SettingsTab.Misc) {
            viewModelScope.launch {
                getStorageStatsUseCase().collect { storageStats ->
                    _state.update { it.copy(storageStats = storageStats) }
                }
            }
        }
    }

    private fun updateIntelChannelAutocomplete(logsDirectory: Path?) {
        val channelNames = try {
            getChatLogsDirectoryUseCase(logsDirectory)
                ?.listDirectoryEntries()
                ?.mapNotNull { matchChatLogFilenameUseCase(it)?.channelName }
                ?.distinct()
                ?: emptyList()
        } catch (e: IOException) {
            logger.error { "Could not get intel channels for autocomplete: ${e.message}" }
            emptyList()
        }
        _state.update { it.copy(autocompleteIntelChannels = channelNames) }
    }

    fun onSuggestedIntelChannelsClick() {
        val channels = configurationPackRepository.getSuggestedIntelChannels()?.channels ?: emptyList()
        settings.intelChannels = (settings.intelChannels + channels).sortedBy { it.name }
    }

    fun onIntelChannelAdded(name: String, region: String?) {
        val channel = IntelChannel(name, region)
        val channels = (settings.intelChannels + channel).sortedBy { it.name }
        settings.intelChannels = channels
    }

    fun onIntelChannelDelete(channel: IntelChannel) {
        settings.intelChannels -= channel
    }

    fun onIsShowingSystemDistanceChange(enabled: Boolean) {
        settings.isShowingSystemDistance = enabled
    }

    fun onIsUsingJumpBridgesForDistance(enabled: Boolean) {
        settings.isUsingJumpBridgesForDistance = enabled
    }

    fun onIntelExpireSecondsChange(seconds: Int) {
        settings.intelExpireSeconds = seconds
    }

    fun onLogsDirectoryChanged(text: String) {
        val directory = try {
            Path.of(text)
        } catch (_: InvalidPathException) {
            null
        }
        settings.eveLogsDirectory = directory
        _state.update {
            it.copy(
                logsDirectory = text,
                isLogsDirectoryValid = getChatLogsDirectoryUseCase(directory) != null,
            )
        }
    }

    fun onDetectLogsDirectoryClick() {
        val logsDirectory = detectLogsDirectoryUseCase()
        if (logsDirectory == null) {
            val title = "找不到日志"
            val message =
                "Cannot find your EVE Online logs directory.\nPlease enter a path ending in \"EVE/logs\" manually."
            _state.update { it.copy(dialogMessage = DialogMessage(title, message, MessageDialogType.Warning)) }
        }
    }

    fun onSettingsDirectoryChanged(text: String) {
        val directory = try {
            Path.of(text)
        } catch (_: InvalidPathException) {
            null
        }
        settings.eveSettingsDirectory = directory
        _state.update {
            it.copy(
                settingsDirectory = text,
                isSettingsDirectoryValid = getEveCharactersSettingsUseCase(directory).isNotEmpty(),
            )
        }
    }

    fun onDetectSettingsDirectoryClick() {
        val directory = detectEveSettingsDirectoryUseCase()
        if (directory == null) {
            val title = "找不到安装目录"
            val message =
                "Cannot find your EVE Online settings directory.\nPlease enter a path ending in \"CCP/EVE/[..]_tq_tranquility\" manually."
            _state.update { it.copy(dialogMessage = DialogMessage(title, message, MessageDialogType.Warning)) }
        }
    }

    fun onCharacterPortraitsStandingsEffectStrengthChanged(strength: Float) {
        settings.characterPortraits = settings.characterPortraits.copy(standingsEffectStrength = strength)
    }

    fun onCharacterPortraitsStandingsTargetsChanged(targets: CharacterPortraitsStandingsTargets) {
        settings.characterPortraits = settings.characterPortraits.copy(standingsTargets = targets)
    }

    fun onCharacterPortraitsParallaxStrengthChanged(strength: CharacterPortraitsParallaxStrength) {
        settings.characterPortraits = settings.characterPortraits.copy(parallaxStrength = strength)
    }

    fun onShowSetupWizardOnNextStartChanged(enabled: Boolean) {
        settings.isShowSetupWizardOnNextStart = enabled
    }

    fun onIsDisplayEveTimeChanged(enabled: Boolean) {
        settings.isDisplayEveTime = enabled
    }

    fun onRememberOpenWindowsChanged(enabled: Boolean) {
        settings.isRememberOpenWindows = enabled
    }

    fun onRememberWindowPlacementChanged(enabled: Boolean) {
        settings.isRememberWindowPlacement = enabled
    }

    fun onEditNotificationClick() {
        _state.update { it.copy(isEditNotificationWindowOpen = true) }
    }

    fun onConfigurePushoverClick() {
        windowManager.onWindowOpen(RiftWindow.Push)
    }

    fun onEditNotificationDone(editPos: Pos?, pos: Pos?) {
        _state.update { it.copy(isEditNotificationWindowOpen = false) }
        if (editPos != null) {
            settings.notificationEditPosition = editPos
        }
        if (pos != null) {
            settings.notificationPosition = pos
        }
    }

    fun onIsZkillboardMonitoringChanged(enabled: Boolean) {
        settings.isZkillboardMonitoringEnabled = enabled
    }

    fun onIsUsingDarkTrayIconChanged(enabled: Boolean) {
        if (settings.isUsingDarkTrayIcon != enabled) {
            showRestartRequiredDialog("New tray icon will take effect after you restart the application.")
            settings.isUsingDarkTrayIcon = enabled
        }
    }

    fun onIsShowIskCentsChanged(enabled: Boolean) {
        settings.isShowIskCents = enabled
    }

    fun onIsSmartAlwaysAboveChanged(enabled: Boolean) {
        settings.isSmartAlwaysAbove = enabled
    }

    fun onIsWindowTransparencyChanged(enabled: Boolean) {
        if (settings.isWindowTransparencyEnabled != enabled) {
            if (enabled) {
                showRestartRequiredDialog("Window transparency will be enabled after you restart the application. This feature requires a modern GPU.")
            } else {
                showRestartRequiredDialog("Window transparency will be disabled after you restart the application.")
            }
            settings.isWindowTransparencyEnabled = enabled
        }
    }

    fun onWindowTransparencyModifierChanged(modifier: Float) {
        settings.windowTransparencyModifier = modifier
    }

    fun onUiScaleChanged(uiScale: Float) {
        showRestartRequiredDialog("Changing the UI scale will take effect after you restart the application.")
        settings.uiScale = uiScale
    }

    fun onSoundsVolumeChange(volume: Int) {
        settings.soundsVolume = volume
    }

    fun onConfigurationPackChange(configurationPack: ConfigurationPack?) {
        if (settings.configurationPack != configurationPack) {
            showRestartRequiredDialog("Some elements of a configuration pack will only take effect after you restart the application.")
            configurationPackRepository.set(configurationPack)
        }
    }

    fun onIntelPopupTimeoutSecondsChange(seconds: Int) {
        settings.intelMap = settings.intelMap.copy(intelPopupTimeoutSeconds = seconds)
    }

    fun onIsUsingCompactModeChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isUsingCompactMode = enabled)
    }

    fun onIsFollowingCharacterWithinLayoutsChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isFollowingCharacterWithinLayouts = enabled)
    }

    fun onIsFollowingCharacterAcrossLayoutsChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isFollowingCharacterAcrossLayouts = enabled)
    }

    fun onIsScrollZoomInvertedChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isInvertZoom = enabled)
    }

    fun onIsAlwaysShowingSystemsChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isAlwaysShowingSystems = enabled)
    }

    fun onIsPreferringRegionMapsChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isPreferringRegionMaps = enabled)
    }

    fun onMapNotesClick() {
        windowManager.onWindowOpen(RiftWindow.MapMarkers)
    }

    fun onIsUsingRiftAutopilotRouteChange(enabled: Boolean) {
        settings.isUsingRiftAutopilotRoute = enabled
    }

    fun onJumpBridgeForgetClick() {
        jumpBridgesRepository.setConnections(emptyList())
        _state.update { it.copy(jumpBridgeNetwork = emptyList()) }
    }

    fun onJumpBridgeImportClick() {
        val network = (_state.value.jumpBridgeCopyState as? JumpBridgeCopyState.Copied)?.network ?: return
        importJumpBridges(network)
    }

    fun onJumpBridgeSearchImportClick() {
        val network = (_state.value.jumpBridgeSearchState as? JumpBridgeSearchState.SearchDone)?.network ?: return
        importJumpBridges(network)
    }

    private fun importJumpBridges(network: List<JumpBridgeConnection>) {
        jumpBridgesRepository.setConnections(network)
        _state.update {
            it.copy(
                jumpBridgeNetwork = network,
                jumpBridgeCopyState = JumpBridgeCopyState.NotCopied,
                jumpBridgeSearchState = JumpBridgeSearchState.NotSearched,
            )
        }
    }

    fun onJumpBridgeSearchClick() {
        _state.update { it.copy(isJumpBridgeSearchDialogShown = true) }
    }

    fun onIsJumpBridgeNetworkShownChange(enabled: Boolean) {
        settings.intelMap = settings.intelMap.copy(isJumpBridgeNetworkShown = enabled)
    }

    fun onJumpBridgeNetworkOpacityChange(percent: Int) {
        settings.intelMap = settings.intelMap.copy(jumpBridgeNetworkOpacity = percent)
    }

    fun onJumpBridgeDialogDismissed() {
        _state.update { it.copy(isJumpBridgeSearchDialogShown = false) }
    }

    fun onJumpBridgeSearchDialogConfirmClick() {
        onJumpBridgeDialogDismissed()
        if (_state.value.jumpBridgeSearchState !is JumpBridgeSearchState.NotSearched) return
        viewModelScope.launch {
            _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.Searching(0f, 0)) }
            jumpBridgesRepository.search().collect { searchState ->
                when (searchState) {
                    is JumpBridgesRepository.SearchState.Progress -> {
                        _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.Searching(searchState.progress, searchState.connectionsCount)) }
                    }
                    JumpBridgesRepository.SearchState.Error -> {
                        _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.SearchFailed) }
                        kotlinx.coroutines.delay(2000)
                        _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.NotSearched) }
                    }
                    is JumpBridgesRepository.SearchState.Result -> {
                        _state.update { it.copy(jumpBridgeSearchState = JumpBridgeSearchState.SearchDone(searchState.connections)) }
                    }
                }
            }
        }
    }

    fun onJumpBridgeCopyClick() {
        val text = jumpBridgesRepository.getConnections().joinToString("\n") { connection ->
            "${connection.from.name} -> ${connection.to.name}"
        }
        Clipboard.copy(text)
        _state.update { it.copy(dialogMessage = DialogMessage("导出成功", "Jump Bridge network copied to clipboard", MessageDialogType.Info)) }
    }

    fun onSovereigntyUpgradesImportClick() {
        val upgrades = (_state.value.sovereigntyUpgradesCopyState as? SovereigntyUpgradesCopyState.Copied)?.upgrades ?: return
        sovereigntyUpgradesRepository.setUpgrades(upgrades)
        _state.update {
            it.copy(
                sovereigntyUpgradesCopyState = SovereigntyUpgradesCopyState.NotCopied,
                sovereigntyUpgrades = upgrades,
            )
        }
    }

    fun onSovereigntyUpgradesForgetClick() {
        sovereigntyUpgradesRepository.setUpgrades(emptyMap())
        _state.update { it.copy(sovereigntyUpgrades = emptyMap()) }
    }

    fun onSovereigntyUpgradesCopyClick() {
        val text = sovereigntyUpgradesRepository.upgrades.value.entries.joinToString("\n") { (system, upgrades) ->
            "${system.name} <- ${upgrades.joinToString(", ") { it.name }}"
        }
        Clipboard.copy(text)
        _state.update { it.copy(dialogMessage = DialogMessage("导出成功", "Sovereignty upgrades copied to clipboard", MessageDialogType.Info)) }
    }

    fun onIsSovereigntyUpgradesHackImportingEnabledClick(enabled: Boolean) {
        settings.isSovereigntyUpgradesHackImportingEnabled = enabled
    }

    fun onIsSovereigntyUpgradesHackImportingOfflineEnabledClick(enabled: Boolean) {
        settings.isSovereigntyUpgradesHackImportingOfflineEnabled = enabled
    }

    fun onClipboardTesterClick() {
        windowManager.onWindowOpen(RiftWindow.ClipboardTest)
    }

    fun onOpenAppData() {
        _state.value.storageStats?.dataDirectory?.openFileManager()
    }

    fun onOpenAppCache() {
        _state.value.storageStats?.cacheDirectory?.openFileManager()
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialogMessage = null) }
    }

    private fun showRestartRequiredDialog(message: String) {
        viewModelScope.launch {
            delay(Duration.ofMillis(300))
            val dialogMessage = DialogMessage("需要重启", message, MessageDialogType.Info)
            _state.update { it.copy(dialogMessage = dialogMessage) }
        }
    }
}
