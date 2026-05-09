package dev.nohus.rift.charactersettings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.CharacterInfo
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.charactersettings.GetAccountsUseCase.Account
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.nio.file.Path

@Factory
class CharacterSettingsViewModel(
    private val copyEveCharacterSettingsUseCase: CopyEveCharacterSettingsUseCase,
    private val localCharactersRepository: LocalCharactersRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val getAccounts: GetAccountsUseCase,
    private val accountAssociationsRepository: AccountAssociationsRepository,
    private val windowManager: WindowManager,
    private val settings: Settings,
) : ViewModel() {

    data class CharacterItem(
        val characterId: Int,
        val accountId: Int?,
        val settingsFiles: Map<String, Path>,
        val info: CharacterInfo?,
    )

    data class UiState(
        val characters: List<CharacterItem> = emptyList(),
        val accounts: List<Account> = emptyList(),
        val copying: CopyingState = CopyingState.SelectingSource,
        val isOnline: Boolean,
        val dialogMessage: DialogMessage? = null,
    )

    sealed interface CopyingState {
        data object SelectingSource : CopyingState

        data class SelectingSourceLauncherProfile(
            val source: CopyingCharacter,
            val profiles: List<String>,
        ) : CopyingState

        data class SelectingDestination(
            val source: CopyingCharacter,
            val sourceLauncherProfile: String,
        ) : CopyingState

        data class DestinationSelected(
            val source: CopyingCharacter,
            val destination: List<CopyingCharacter>,
            val sourceLauncherProfile: String,
        ) : CopyingState

        data class SelectingTargetLauncherProfile(
            val source: CopyingCharacter,
            val destination: List<CopyingCharacter>,
            val sourceLauncherProfile: String,
            val profiles: List<String>,
        ) : CopyingState
    }

    data class CopyingCharacter(
        val character: CharacterItem,
    ) {
        val id get() = character.characterId
        val name get() = character.info?.name ?: character.characterId.toString()
    }

    private val _state = MutableStateFlow(
        UiState(
            isOnline = onlineCharactersRepository.onlineCharacters.value.isNotEmpty(),
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
                settings.updateFlow,
            ) { _, _, _ ->
                delay(500) // Allow account associations to settle
                val characters = localCharactersRepository.characters.value
                val accounts = getAccounts()
                val accountAssociations = accountAssociationsRepository.getAssociations()
                val items = characters
                    .map { localCharacter ->
                        CharacterItem(
                            characterId = localCharacter.characterId,
                            accountId = accountAssociations[localCharacter.characterId],
                            settingsFiles = localCharacter.settingsFiles,
                            info = localCharacter.info,
                        )
                    }
                _state.update {
                    it.copy(
                        characters = items,
                        accounts = accounts,
                    )
                }
            }.collect()
        }
        viewModelScope.launch {
            onlineCharactersRepository.onlineCharacters.collect { onlineCharacters ->
                _state.update { it.copy(isOnline = onlineCharacters.isNotEmpty()) }
            }
        }
    }

    fun onCopySourceClick(characterId: Int) {
        val character = _state.value.characters.firstOrNull { it.characterId == characterId } ?: return
        val profiles = character.settingsFiles.keys.takeIf { it.isNotEmpty() }?.toList() ?: return
        if (profiles.size > 1) {
            // This character has settings in more than 1 profile, so we need to select the source profile
            _state.update { it.copy(copying = CopyingState.SelectingSourceLauncherProfile(CopyingCharacter(character), profiles)) }
        } else {
            // This character has 1 profile, no need to select the source profile
            onSourceProfileSelected(CopyingCharacter(character), profiles.single())
        }
    }

    fun onCopySourceProfileClick(sourceProfile: String) {
        val state = _state.value.copying
        if (state is CopyingState.SelectingSourceLauncherProfile) {
            onSourceProfileSelected(state.source, sourceProfile)
        }
    }

    private fun onSourceProfileSelected(source: CopyingCharacter, profile: String) {
        _state.update { it.copy(copying = CopyingState.SelectingDestination(source, profile)) }
    }

    fun onCopyDestinationClick(characterId: Int) {
        val state = _state.value.copying
        if (state is CopyingState.SelectingDestination) {
            val destinationCharacter = _state.value.characters.firstOrNull { it.characterId == characterId } ?: return
            _state.update {
                it.copy(
                    copying = CopyingState.DestinationSelected(
                        source = state.source,
                        destination = listOf(CopyingCharacter(destinationCharacter)),
                        sourceLauncherProfile = state.sourceLauncherProfile,
                    ),
                )
            }
        } else if (state is CopyingState.DestinationSelected) {
            val destinationCharacter = _state.value.characters.firstOrNull { it.characterId == characterId } ?: return
            val destinations = state.destination + CopyingCharacter(destinationCharacter)
            _state.update {
                it.copy(
                    copying = CopyingState.DestinationSelected(
                        source = state.source,
                        destination = destinations,
                        sourceLauncherProfile = state.sourceLauncherProfile,
                    ),
                )
            }
        }
    }

    fun onCopySettingsConfirmClick() {
        val state = _state.value.copying
        if (state is CopyingState.DestinationSelected) {
            val allProfiles = _state.value.characters.flatMap { it.settingsFiles.keys }.distinct()
            if (allProfiles.size > 1) {
                // There is more than 1 profile, need to select the destination profile
                _state.update { it.copy(copying = CopyingState.SelectingTargetLauncherProfile(state.source, state.destination, state.sourceLauncherProfile, allProfiles)) }
            } else {
                // There is only one profile, no need to select target profile
                copySettings(state.source, state.destination, state.sourceLauncherProfile, allProfiles.single())
            }
        }
    }

    fun onCopyTargetProfileClick(targetProfile: String) {
        val state = _state.value.copying
        if (state is CopyingState.SelectingTargetLauncherProfile) {
            copySettings(state.source, state.destination, state.sourceLauncherProfile, targetProfile)
        }
    }

    private fun copySettings(
        source: CopyingCharacter,
        destination: List<CopyingCharacter>,
        sourceLauncherProfile: String,
        targetLauncherProfile: String,
    ) {
        val success = copyEveCharacterSettingsUseCase(source.id, destination.map { it.id }, sourceLauncherProfile, targetLauncherProfile)

        val dialogMessage = if (success) {
            DialogMessage(
                title = "Settings copied",
                message = "EVE settings have been copied from ${source.name} to ${destination.joinToString { it.name }}.",
                type = MessageDialogType.Info,
            )
        } else {
            DialogMessage(
                title = "Copying failed",
                message = "There is something wrong with your character settings files.",
                type = MessageDialogType.Warning,
            )
        }
        _state.update {
            it.copy(
                dialogMessage = dialogMessage,
            )
        }
    }

    fun onAssignAccount(characterId: Int, accountId: Int) {
        accountAssociationsRepository.associate(characterId, accountId)
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialogMessage = null) }
        onCancelClick()
    }

    fun onCancelClick() {
        _state.update { it.copy(copying = CopyingState.SelectingSource) }
        windowManager.onWindowClose(RiftWindow.CharacterSettings, uuid = null)
    }
}
