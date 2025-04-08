package dev.nohus.rift.contacts

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.contacts.ContactsExternalControl.ContactsExternalControlEvent
import dev.nohus.rift.contacts.ContactsRepository.Contact
import dev.nohus.rift.contacts.ContactsRepository.Entity
import dev.nohus.rift.contacts.ContactsRepository.EntityType
import dev.nohus.rift.contacts.SearchRepository.SearchCategory
import dev.nohus.rift.contacts.SearchRepository.SearchResult
import dev.nohus.rift.get
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.network.AsyncResource.Ready
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiErrorException
import dev.nohus.rift.network.esi.EsiErrorResponse
import dev.nohus.rift.network.toResource
import dev.nohus.rift.repositories.NamesRepository
import dev.nohus.rift.standings.Standing
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.lang.Exception

private val logger = KotlinLogging.logger {}

@Single
class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val localCharactersRepository: LocalCharactersRepository,
    private val namesRepository: NamesRepository,
    private val contactsExternalControl: ContactsExternalControl,
    private val searchRepository: SearchRepository,
) : ViewModel() {

    data class UiState(
        val selectedTab: ContactsTab = ContactsTab.Contacts,
        val contacts: List<Contact> = emptyList(),
        val filteredContacts: List<Contact> = emptyList(),
        val ownerLabels: List<Pair<Entity, List<String>>> = emptyList(),
        val filter: Filter = Filter.All,
        val contactSearch: String = "",
        val editDialog: EditContactDialog? = null,
        val isContactsLoading: Boolean = false,
        val characters: List<LocalCharacter> = emptyList(),
        val search: String = "",
        val searchCategories: List<SearchCategory> = SearchCategory.entries,
        val searchResults: AsyncResource<Map<SearchCategory, List<SearchResult>>> = Ready(emptyMap()),
    )

    sealed interface Filter {
        data object All : Filter
        data class Standings(val level: Standing) : Filter
        data class Owner(val owner: Entity) : Filter
        data class Unlabeled(val owner: Entity) : Filter
        data class Label(val owner: Entity, val label: String) : Filter
    }

    data class EditContactDialog(
        val entity: Entity,
        val ownerCharacters: List<Entity>,
        val ownerStandings: Map<Int, Standing>,
        val ownerLabels: Map<Int, List<String>>,
        val ownerWatched: Map<Int, Boolean>,
        val characters: List<LocalCharacter>,
        val labels: Map<Int, List<String>>,
    )

    data class UpdateContactRequest(
        val characterId: Int,
        val entity: Entity,
        val standing: Standing,
        val labels: List<String>,
        val isWatched: Boolean?,
    )

    data class DeleteContactRequest(
        val characterId: Int,
        val entity: Entity,
    )

    enum class ContactsTab {
        Contacts, Search
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            contactsRepository.contacts.collect { contacts ->
                val owners = contacts.contacts
                    .map { it.owner }
                    .distinct()
                    .sortedWith(compareBy({ it.type }, { it.name }))
                val labels = owners
                    .associateWith { owner ->
                        contacts.labels[owner]
                            ?.map { it.name }
                            ?: emptyList()
                    }
                    .map { it.key to it.value.distinct() }
                    .sortedWith(compareBy({ it.first.type }, { it.first.name }))
                val sortedContacts = contacts.contacts.sortedBy { it.entity.name }
                _state.update {
                    it.copy(
                        contacts = sortedContacts,
                        filteredContacts = sortedContacts.filter(_state.value.filter, _state.value.contactSearch),
                        ownerLabels = labels,
                        isContactsLoading = contacts.isLoading,
                    )
                }
            }
        }
        viewModelScope.launch {
            localCharactersRepository.characters.collect { characters ->
                _state.update { it.copy(characters = characters) }
            }
        }
        viewModelScope.launch {
            contactsExternalControl.event.collect {
                when (val event = it.get()) {
                    is ContactsExternalControlEvent.Edit -> onEdit(event.id, event.type)
                    null -> {}
                }
            }
        }
    }

    fun onTabSelected(tab: ContactsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun onFilterClick(filter: Filter) {
        _state.update {
            it.copy(
                filteredContacts = _state.value.contacts.filter(filter, _state.value.contactSearch),
                filter = filter,
            )
        }
    }

    fun onContactSearchChange(search: String) {
        _state.update {
            it.copy(
                filteredContacts = _state.value.contacts.filter(_state.value.filter, search),
                contactSearch = search,
            )
        }
    }

    fun onEdit(entity: Entity, owner: Entity?) {
        val existingContacts = _state.value.contacts
            .filter { it.entity == entity && it.owner.type == EntityType.Character }
        val ownerCharacters = existingContacts
            .map { it.owner }
        val ownerStandings = existingContacts
            .associate { it.owner.id to it.standingLevel }
        val ownerLabels = existingContacts
            .associate { it.owner.id to it.labels }
        val ownerWatched = existingContacts
            .associate { it.owner.id to it.isWatched }

        val ownerOrDefault = owner ?: ownerCharacters.firstOrNull()
        val characters = localCharactersRepository.characters.value
            .sortedBy { it.characterId != ownerOrDefault?.id }
        if (characters.isEmpty()) {
            logger.warn { "Cannot edit contact because there are no characters detected" }
            return
        }
        val labels = _state.value.ownerLabels
            .filter { it.first.type == EntityType.Character }
            .associate { it.first.id to it.second }

        _state.update {
            it.copy(
                editDialog = EditContactDialog(
                    entity = entity,
                    ownerCharacters = ownerCharacters,
                    ownerStandings = ownerStandings,
                    ownerLabels = ownerLabels,
                    ownerWatched = ownerWatched,
                    characters = characters,
                    labels = labels,
                ),
            )
        }
    }

    fun onEditDismiss() {
        _state.update { it.copy(editDialog = null) }
    }

    fun onEditConfirmClick(request: UpdateContactRequest) {
        viewModelScope.launch {
            _state.update { it.copy(editDialog = null) }
            contactsRepository.editContact(
                characterId = request.characterId,
                labels = request.labels,
                standing = when (request.standing) {
                    Standing.Terrible -> -10f
                    Standing.Bad -> -5f
                    Standing.Neutral -> 0f
                    Standing.Good -> 5f
                    Standing.Excellent -> 10f
                },
                isWatched = request.isWatched,
                entity = request.entity,
            )
        }
    }

    fun onDeleteClick(request: DeleteContactRequest) {
        viewModelScope.launch {
            _state.update { it.copy(editDialog = null) }
            contactsRepository.deleteContact(
                characterId = request.characterId,
                contactId = request.entity.id,
            )
        }
    }

    fun onReloadClick() {
        viewModelScope.launch {
            contactsRepository.reload()
        }
    }

    fun onSearchCategoriesChange(categories: List<SearchCategory>) {
        _state.update { it.copy(searchCategories = categories) }
    }

    fun onSearchChange(search: String) {
        _state.update { it.copy(search = search) }
    }

    fun onSearchConfirm(character: LocalCharacter?) {
        _state.update { it.copy(searchResults = AsyncResource.Loading) }
        if (character != null) {
            viewModelScope.launch {
                var attempt = 0
                while (true) {
                    attempt++
                    val result = searchRepository.search(character.characterId, _state.value.searchCategories, _state.value.search).mapResult {
                        if (it.values.flatten().isEmpty()) Result.Failure(EsiErrorException(EsiErrorResponse("No results"), 0)) else Result.Success(it)
                    }
                    if (result.isFailure && attempt < 3 && result.failure !is EsiErrorException) continue
                    _state.update { it.copy(searchResults = result.toResource()) }
                    break
                }
            }
        } else {
            _state.update { it.copy(searchResults = AsyncResource.Error(Exception("No character selected"))) }
        }
    }

    private fun onEdit(id: Int, type: EntityType) {
        viewModelScope.launch {
            namesRepository.resolveNames(listOf(id))
            val name = namesRepository.getName(id) ?: id.toString()
            onEdit(Entity(id, name, type), null)
        }
    }

    private fun List<Contact>.filter(filter: Filter, search: String): List<Contact> {
        val filtered = when (filter) {
            Filter.All -> this
            is Filter.Standings -> filter { filter.level == it.standingLevel }
            is Filter.Owner -> filter { filter.owner == it.owner }
            is Filter.Unlabeled -> filter { filter.owner == it.owner && it.labels.isEmpty() }
            is Filter.Label -> filter { filter.owner == it.owner && filter.label in it.labels }
        }
        return if (search.isNotBlank()) {
            val lowercase = search.lowercase()
            return filtered.filter { lowercase in it.entity.name.lowercase() }
        } else {
            filtered
        }
    }
}
