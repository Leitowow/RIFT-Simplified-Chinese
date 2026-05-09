package dev.nohus.rift.characters.repositories

import dev.nohus.rift.characters.files.GetEveCharactersSettingsUseCase
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.combine
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.character.CharacterAffiliationRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.sso.scopes.ScopeGroup
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.utils.stateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.MainUIDispatcher
import org.koin.core.annotation.Single
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

@Single
class LocalCharactersRepository(
    private val settings: Settings,
    private val getEveCharactersSettingsUseCase: GetEveCharactersSettingsUseCase,
    private val esiApi: EsiApi,
    private val characterAffiliationRepository: CharacterAffiliationRepository,
) {

    data class LocalCharacter(
        val characterId: Int,
        val settingsFiles: Map<String, Path>, // Launcher profile name -> File
        val scopes: List<ScopeGroup>,
        val info: CharacterInfo?,
        val isHidden: Boolean,
    ) {
        override fun toString(): String {
            return "LocalCharacter(${info?.name ?: characterId})"
        }
    }

    data class CharacterInfo(
        val name: String,
        val characterId: Int,
        val corporationRoles: List<String>,
        val corporationId: Int,
        val corporationName: String,
        val allianceId: Int?,
        val allianceName: String?,
        val birthday: Instant,
    )

    private val _characters = MutableStateFlow<List<LocalCharacter>>(emptyList())
    val characters = stateFlow(
        getValue = { _characters.value.filterNot(LocalCharacter::isHidden) },
        flow = _characters.map { it.filterNot(LocalCharacter::isHidden) },
    )

    /**
     * Includes hidden characters
     */
    val allCharacters = _characters.asStateFlow()

    /**
     * Watch settings, in case the list of authenticated characters changes.
     * Then update the local characters to reflect the authentication status, or remove if a local character
     * was authenticated-only (no local settings file)
     */
    suspend fun start() {
        settings.updateFlow
            .map { it.authenticatedCharacters to it.hiddenCharacterIds.toSet() }
            .collect { (authenticatedCharacters, hiddenCharacterIds) ->
                val scopes = authenticatedCharacters.mapValues { (_, authentication) ->
                    ScopeGroups.getByIds(authentication.scopes)
                }
                withContext(MainUIDispatcher) {
                    _characters.value = _characters.value.mapNotNull { character ->
                        val newCharacter = character.copy(
                            scopes = scopes[character.characterId] ?: emptyList(),
                            isHidden = character.characterId in hiddenCharacterIds,
                        )
                        if (newCharacter.scopes.isEmpty() && newCharacter.settingsFiles.isEmpty()) return@mapNotNull null
                        newCharacter
                    }
                }
            }
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        val characters = loadLocalCharacters()
        loadEsiCharacters(characters)
    }

    /**
     * Loads and returns local characters from settings files and authenticated characters
     */
    private fun loadLocalCharacters(): List<LocalCharacter> {
        val directory = settings.eveSettingsDirectory
        val authenticatedCharacterIds = settings.authenticatedCharacters.keys
        val scopes = settings.authenticatedCharacters.mapValues { (_, authentication) ->
            ScopeGroups.getByIds(authentication.scopes)
        }
        val hiddenCharacterIds = settings.hiddenCharacterIds.toSet()
        val charactersFromFiles = if (directory != null) {
            getEveCharactersSettingsUseCase(directory)
                .groupBy { file ->
                    file.nameWithoutExtension.substringAfterLast("_").toIntOrNull()
                }
                .mapNotNull { (characterId, files) ->
                    characterId ?: return@mapNotNull null
                    val settingsFiles = files.associateBy { file ->
                        file.parent.name.substringAfter("settings_")
                    }
                    LocalCharacter(
                        characterId = characterId,
                        settingsFiles = settingsFiles,
                        scopes = scopes[characterId] ?: emptyList(),
                        info = null,
                        isHidden = characterId in hiddenCharacterIds,
                    )
                }
        } else {
            emptyList()
        }
        val characterIds = charactersFromFiles.map { it.characterId }

        val ssoOnlyCharacters = authenticatedCharacterIds
            .filter { it !in characterIds }
            .map { characterId ->
                LocalCharacter(
                    characterId = characterId,
                    settingsFiles = emptyMap(),
                    scopes = scopes[characterId] ?: emptyList(),
                    info = null,
                    isHidden = characterId in hiddenCharacterIds,
                )
            }

        return (charactersFromFiles + ssoOnlyCharacters).distinctBy { it.characterId }
    }

    private suspend fun loadEsiCharacters(characters: List<LocalCharacter>) = coroutineScope {
        val characterIds = characters.map { it.characterId }
        val affiliations = characterAffiliationRepository.getCharacterAffiliations(Originator.LocalCharacters, characterIds)

        for (localCharacter in characters) {
            launch {
                val characterInfo = combine(
                    async {
                        esiApi.getCharactersId(Originator.LocalCharacters, localCharacter.characterId)
                    },
                    async {
                        if (ScopeGroups.readRoles in localCharacter.scopes) {
                            esiApi.getCharactersIdRoles(Originator.LocalCharacters, localCharacter.characterId).map { it.roles }
                        } else {
                            Result.Success(emptyList())
                        }
                    },
                ) { details, roles ->
                    val corporationId = affiliations[localCharacter.characterId]?.corporationId ?: details.corporationId
                    val allianceId = affiliations[localCharacter.characterId]?.allianceId ?: details.allianceId
                    val corporationDeferred = async { esiApi.getCorporationsId(Originator.LocalCharacters, corporationId) }
                    val allianceDeferred = if (allianceId != null) async { esiApi.getAlliancesId(Originator.LocalCharacters, allianceId) } else null
                    val corporation = corporationDeferred.await()
                    val alliance = allianceDeferred?.await()
                    CharacterInfo(
                        name = details.name,
                        characterId = localCharacter.characterId,
                        corporationRoles = roles,
                        corporationId = corporationId,
                        corporationName = corporation.success?.name ?: "?",
                        allianceId = allianceId,
                        allianceName = if (alliance != null) alliance.success?.name ?: "?" else null,
                        birthday = details.birthday,
                    )
                }.success

                val updatedCharacter = localCharacter.copy(info = characterInfo ?: localCharacter.info)
                withContext(MainUIDispatcher) {
                    var isExistingCharacterUpdated = false
                    val characters = _characters.value.map { current ->
                        if (current.characterId == localCharacter.characterId) {
                            isExistingCharacterUpdated = true
                            updatedCharacter
                        } else {
                            current
                        }
                    }
                    if (!isExistingCharacterUpdated) {
                        _characters.value = (characters + updatedCharacter).sort()
                    } else {
                        _characters.value = characters
                    }
                }
            }
        }
    }

    private fun List<LocalCharacter>.sort(): List<LocalCharacter> {
        return sortedWith(
            compareBy(
                { it.scopes.isEmpty() },
                { it.settingsFiles.values.maxOfOrNull { it.getLastModifiedTime().toMillis() }?.let { -it } ?: 0L },
            ),
        )
    }
}
