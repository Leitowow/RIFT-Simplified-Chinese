package dev.nohus.rift.charactersettings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.charactersettings.GetAccountsUseCase.Account
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.network.AsyncResource
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
import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
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
        val settingsFile: Path?,
        val info: AsyncResource<LocalCharactersRepository.CharacterInfo>,
    )

    data class UiState(
        val characters: List<CharacterItem> = emptyList(),
        val accounts: List<Account> = emptyList(),
        val copying: CopyingState = CopyingState.SelectingSource,
        val dialogMessage: DialogMessage? = null,
    )

    sealed interface CopyingState {
        data object SelectingSource : CopyingState
        data class SelectingDestination(
            val sourceId: Int,
        ) : CopyingState

        data class DestinationSelected(
            val source: CopyingCharacter,
            val destination: List<CopyingCharacter>,
        ) : CopyingState
    }

    data class CopyingCharacter(
        val id: Int,
        val name: String,
    )

    private val _state = MutableStateFlow(
        UiState(),
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
                            settingsFile = localCharacter.settingsFile,
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
    }

    fun onCopySourceClick(characterId: Int) {
        _state.update { it.copy(copying = CopyingState.SelectingDestination(characterId)) }
    }

    fun onCopyDestinationClick(characterId: Int) {
        val state = _state.value.copying
        if (state is CopyingState.SelectingDestination) {
            val sourceName =
                _state.value.characters.firstOrNull { it.characterId == state.sourceId }?.info?.success?.name ?: return
            val destinationName =
                _state.value.characters.firstOrNull { it.characterId == characterId }?.info?.success?.name ?: return
            _state.update {
                it.copy(
                    copying = CopyingState.DestinationSelected(
                        source = CopyingCharacter(state.sourceId, sourceName),
                        destination = listOf(CopyingCharacter(characterId, destinationName)),
                    ),
                )
            }
        } else if (state is CopyingState.DestinationSelected) {
            val destinationName =
                _state.value.characters.firstOrNull { it.characterId == characterId }?.info?.success?.name ?: return
            val destinations = state.destination + CopyingCharacter(characterId, destinationName)
            _state.update {
                it.copy(
                    copying = CopyingState.DestinationSelected(
                        source = state.source,
                        destination = destinations,
                    ),
                )
            }
        }
    }

    fun onCopySettingsConfirmClick() {
        val state = _state.value.copying
        if (state is CopyingState.DestinationSelected) {
            val success = copyEveCharacterSettingsUseCase(state.source.id, state.destination.map { it.id })

            val dialogMessage = if (success) {
                DialogMessage(
                    title = "Settings copied",
                    message = "EVE settings have been copied from ${state.source.name} to ${state.destination.joinToString { it.name }}.",
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
        windowManager.onWindowClose(RiftWindow.CharacterSettings)
    }
}
