package dev.nohus.rift.characters

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.CharacterWalletRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.charactersettings.GetAccountsUseCase
import dev.nohus.rift.charactersettings.GetAccountsUseCase.Account
import dev.nohus.rift.clones.Clone
import dev.nohus.rift.clones.ClonesRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.location.CharacterLocationRepository.Location
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
class CharactersViewModel(
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val characterWalletRepository: CharacterWalletRepository,
    private val clonesRepository: ClonesRepository,
    private val getAccounts: GetAccountsUseCase,
    private val windowManager: WindowManager,
    private val settings: Settings,
) : ViewModel() {

    data class CharacterItem(
        val characterId: Int,
        val settingsFile: Path?,
        val isAuthenticated: Boolean,
        val isHidden: Boolean,
        val info: AsyncResource<LocalCharactersRepository.CharacterInfo>,
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
    )

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
                characterWalletRepository.balances,
                clonesRepository.clones,
                settings.updateFlow,
            ) { characters, onlineCharacters, balances, clones, _ ->
                val items = characters
                    .map { localCharacter ->
                        CharacterItem(
                            characterId = localCharacter.characterId,
                            settingsFile = localCharacter.settingsFile,
                            isAuthenticated = localCharacter.isAuthenticated,
                            isHidden = localCharacter.isHidden,
                            info = localCharacter.info,
                            walletBalance = balances[localCharacter.characterId],
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
