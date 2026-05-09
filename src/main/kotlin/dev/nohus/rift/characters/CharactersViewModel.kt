package dev.nohus.rift.characters

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.charactersettings.GetAccountsUseCase
import dev.nohus.rift.charactersettings.GetAccountsUseCase.Account
import dev.nohus.rift.clones.Clone
import dev.nohus.rift.clones.ClonesRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.location.CharacterLocationRepository.Location
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.sso.authentication.EveSsoRepository
import dev.nohus.rift.sso.scopes.ScopeGroup
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.wallet.WalletRepository
import dev.nohus.rift.wallet.WalletRepository.WalletBalance
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.deleteExisting

private val logger = KotlinLogging.logger {}

@Factory
class CharactersViewModel(
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val walletRepository: WalletRepository,
    private val clonesRepository: ClonesRepository,
    private val eveSsoRepository: EveSsoRepository,
    private val getAccounts: GetAccountsUseCase,
    private val windowManager: WindowManager,
    private val settings: Settings,
) : ViewModel() {

    data class CharacterItem(
        val characterId: Int,
        val settingsFiles: Map<String, Path>,
        val authenticationStatus: AuthenticationStatus,
        val isHidden: Boolean,
        val info: LocalCharactersRepository.CharacterInfo?,
        val walletBalance: Double?,
        val clones: List<Clone>,
    )

    data class UiState(
        val characters: List<CharacterItem> = emptyList(),
        val accounts: List<Account> = emptyList(),
        val onlineCharacters: List<Int> = emptyList(),
        val locations: Map<Int, Location> = emptyMap(),
        val isChoosingDisabledCharacters: Boolean = false,
        val isSsoDialogOpen: Boolean = false,
        val isShowingClones: Boolean,
        val deletingCharacter: CharacterItem? = null,
    )

    sealed class AuthenticationStatus {
        data object Authenticated : AuthenticationStatus()
        data class PartiallyAuthenticated(val missingScopes: List<ScopeGroup>) : AuthenticationStatus()
        data object Unauthenticated : AuthenticationStatus()
    }

    private val _state = MutableStateFlow(
        UiState(
            isShowingClones = settings.isShowingCharactersClones,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            localCharactersRepository.load()
        }
        viewModelScope.launch {
            combine(
                localCharactersRepository.allCharacters,
                onlineCharactersRepository.onlineCharacters,
                walletRepository.state,
                clonesRepository.clones,
                settings.updateFlow,
            ) { characters, onlineCharacters, walletsState, clones, _ ->
                val items = characters
                    .map { localCharacter ->
                        val missingScopes = ScopeGroups.all - localCharacter.scopes.toSet()
                        val authenticationStatus = when {
                            missingScopes.isEmpty() -> AuthenticationStatus.Authenticated
                            localCharacter.scopes.isEmpty() -> AuthenticationStatus.Unauthenticated
                            else -> AuthenticationStatus.PartiallyAuthenticated(missingScopes)
                        }
                        val walletBalance = walletsState.loadedState?.success?.balances
                            ?.firstOrNull { it is WalletBalance.Character && it.characterId == localCharacter.characterId }
                            ?.balance
                        CharacterItem(
                            characterId = localCharacter.characterId,
                            settingsFiles = localCharacter.settingsFiles,
                            authenticationStatus = authenticationStatus,
                            isHidden = localCharacter.isHidden,
                            info = localCharacter.info,
                            walletBalance = walletBalance,
                            clones = clones[localCharacter.characterId] ?: emptyList(),
                        )
                    }
                val sortedItems = items.sortedByDescending { it.characterId in onlineCharacters }
                _state.update { it.copy(characters = sortedItems) }
            }.collect()
        }
        viewModelScope.launch {
            localCharactersRepository.allCharacters.collect {
                val accounts = getAccounts()
                _state.update { it.copy(accounts = accounts) }
            }
        }
        viewModelScope.launch {
            characterLocationRepository.locations.collect { locations ->
                _state.update { it.copy(locations = locations) }
            }
        }
        observeOnlineCharacters()
    }

    fun onSsoClick() {
        _state.update { it.copy(isSsoDialogOpen = true) }
    }

    fun onCloseSso() {
        _state.update { it.copy(isSsoDialogOpen = false) }
    }

    fun onCopySettingsClick() {
        windowManager.onWindowOpen(RiftWindow.CharacterSettings)
    }

    fun onChooseDisabledClick() {
        _state.update { it.copy(isChoosingDisabledCharacters = !it.isChoosingDisabledCharacters) }
    }

    fun onDisableCharacterClick(characterId: Int) {
        settings.hiddenCharacterIds += characterId
    }

    fun onEnableCharacterClick(characterId: Int) {
        settings.hiddenCharacterIds -= characterId
    }

    fun onDeleteCharacterClick(characterId: Int) {
        val item = _state.value.characters.firstOrNull { it.characterId == characterId } ?: return
        _state.update { it.copy(deletingCharacter = item) }
    }

    fun onDeleteCharacterConfirm() {
        val item = _state.value.deletingCharacter ?: return
        try {
            item.settingsFiles.values.forEach { it.deleteExisting() }
            settings.hiddenCharacterIds -= item.characterId
            eveSsoRepository.removeAuthentication(item.characterId)
            viewModelScope.launch {
                localCharactersRepository.load()
                _state.update { it.copy(deletingCharacter = null) }
            }
        } catch (e: IOException) {
            logger.error(e) { "Failed deleting character" }
        }
    }

    fun onDeleteCharacterCancel() {
        _state.update { it.copy(deletingCharacter = null) }
    }

    fun onIsShowingCharactersClonesChange(enabled: Boolean) {
        settings.isShowingCharactersClones = enabled
        _state.update { it.copy(isShowingClones = enabled) }
    }

    private fun observeOnlineCharacters() = viewModelScope.launch {
        onlineCharactersRepository.onlineCharacters.collect { onlineCharacters ->
            _state.update { it.copy(onlineCharacters = onlineCharacters) }
        }
    }
}
