package dev.nohus.rift.contacts

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.repositories.IdRanges.isNpcAgent
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.StandingUtils.getStandingLevel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import dev.nohus.rift.network.esi.Contact as ContactDto
import dev.nohus.rift.network.esi.ContactType as ContactTypeDto

private val logger = KotlinLogging.logger {}

@Single
class ContactsRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
) {

    data class Contacts(
        val contacts: List<Contact> = emptyList(),
        val labels: Map<Entity, List<Label>> = emptyMap(),
        val isLoading: Boolean = true,
    )

    data class Contact(
        val entity: Entity,
        val owner: Entity,
        val labels: List<String> = emptyList(),
        val isBlocked: Boolean,
        val isWatched: Boolean,
        val standing: Float,
        val standingLevel: Standing,
    )

    data class Entity(
        val id: Int,
        val name: String,
        val type: EntityType,
    )

    enum class EntityType {
        Character,
        Corporation,
        Alliance,
        Faction,
    }

    data class Label(
        val id: Long,
        val name: String,
    )

    data class ContactsResponse(
        val contacts: List<Contact>,
        val labels: Map<Entity, List<Label>>,
    )

    private val reloadEventFlow = MutableSharedFlow<Unit>()
    private var lastUpdated: Instant = Instant.EPOCH
    private var originalContacts: List<Contact> = emptyList()
    private val _contacts = MutableStateFlow(Contacts())
    val contacts = _contacts.asStateFlow()

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            localCharactersRepository.characters.debounce(500).collect {
                reloadEventFlow.emit(Unit)
            }
        }
        launch {
            while (true) {
                delay(2.minutes)
                if (Duration.between(lastUpdated, Instant.now()) > Duration.ofMinutes(15)) {
                    reloadEventFlow.emit(Unit)
                }
            }
        }
        launch {
            reloadEventFlow.collectLatest {
                updateContacts()
            }
        }
    }

    suspend fun reload() {
        reloadEventFlow.emit(Unit)
    }

    fun isCharacterContact(id: Int): Boolean {
        return id in _contacts.value.contacts
            .filter { it.owner.type == EntityType.Character }
            .map { it.entity.id }
    }

    suspend fun editContact(
        characterId: Int,
        labels: List<String>,
        standing: Float,
        isWatched: Boolean?,
        entity: Entity,
    ) {
        val labelIds = getLabelIds(characterId, labels).takeIf { it.isNotEmpty() }
        val existingContact = _contacts.value.contacts
            .firstOrNull { it.owner.id == characterId && it.entity.id == entity.id }

        val response = if (existingContact != null) {
            // Edit existing contact
            esiApi.putCharactersIdContacts(
                characterId = characterId,
                labelIds = labelIds,
                standing = standing,
                watched = isWatched,
                contactIds = listOf(entity.id),
            )
        } else {
            // Add new contact
            esiApi.postCharactersIdContacts(
                characterId = characterId,
                labelIds = labelIds,
                standing = standing,
                watched = isWatched,
                contactIds = listOf(entity.id),
            )
        }

        if (response.isSuccess) {
            val newContacts = if (existingContact != null) {
                logger.info { "Successfully updated contact" }
                _contacts.value.contacts.map { contact ->
                    if (contact == existingContact) {
                        contact.copy(
                            labels = labels,
                            isWatched = isWatched ?: contact.isWatched,
                            standing = standing,
                            standingLevel = getStandingLevel(standing),
                        )
                    } else {
                        contact
                    }
                }
            } else {
                logger.info { "Successfully added contact" }
                val ownerName = localCharactersRepository.characters.value
                    .firstOrNull { it.characterId == characterId }
                    ?.info?.success?.name ?: "Unknown"
                _contacts.value.contacts + Contact(
                    entity = entity,
                    owner = Entity(characterId, ownerName, EntityType.Character),
                    labels = labels,
                    isBlocked = false,
                    isWatched = isWatched ?: false,
                    standing = standing,
                    standingLevel = getStandingLevel(standing),
                )
            }
            _contacts.update { it.copy(contacts = newContacts) }
        } else {
            logger.error { "Could not edit contacts: ${response.failure?.message}" }
        }
    }

    suspend fun deleteContact(
        characterId: Int,
        contactId: Int,
    ) {
        val response = esiApi.deleteCharactersIdContacts(
            characterId = characterId,
            contactIds = listOf(contactId),
        )
        if (response.isSuccess) {
            logger.info { "Successfully deleted contact" }
            val newContacts = _contacts.value.contacts.filterNot {
                it.owner.id == characterId && it.entity.id == contactId
            }
            _contacts.update { it.copy(contacts = newContacts) }
        } else {
            logger.error { "Could not delete contact: ${response.failure?.message}" }
        }
    }

    private fun getLabelIds(characterId: Int, labelNames: List<String>): List<Long> {
        val characterLabels = _contacts.value.labels.entries
            .firstOrNull { it.key.id == characterId }
            ?.value
            ?.associate { it.name to it.id }
            ?: return emptyList()
        return labelNames.mapNotNull { label -> characterLabels[label] }
    }

    private suspend fun updateContacts() {
        _contacts.update { it.copy(isLoading = true) }
        val validCharacters =
            localCharactersRepository.characters.value.filter { it.isAuthenticated && it.info.success != null }
        if (validCharacters.isEmpty()) {
            _contacts.update { it.copy(isLoading = false) }
            return
        }
        val contactsResponse = getContacts(validCharacters)
        if (contactsResponse == null) {
            _contacts.update { it.copy(isLoading = false) }
            return
        }
        if (contacts != originalContacts) {
            originalContacts = contactsResponse.contacts
            _contacts.update { it.copy(contacts = contactsResponse.contacts, labels = contactsResponse.labels, isLoading = false) }
            logger.info { "Updated contacts" }
        } else {
            // The contacts didn't change since the last time they were fetched, and we might have updated them locally
            // so we don't want to replace the list
            _contacts.update { it.copy(isLoading = false) }
            logger.info { "Checked contacts, no changes" }
        }
    }

    private suspend fun getContacts(characters: List<LocalCharacter>): ContactsResponse? {
        val allianceIds = mutableMapOf<Int, Int>()
        val corporationIds = mutableMapOf<Int, Int>()
        val characterIds = mutableListOf<Int>()
        characters.forEach { character ->
            character.info.success?.let { details ->
                if (details.allianceId != null) allianceIds[details.allianceId] = character.characterId
                corporationIds[details.corporationId] = character.characterId
                characterIds += character.characterId
            }
        }
        return getContacts(allianceIds, corporationIds, characterIds)
    }

    private suspend fun getContacts(
        allianceIds: Map<Int, Int>,
        corporationIds: Map<Int, Int>,
        characterIds: List<Int>,
    ): ContactsResponse? = coroutineScope {
        // Request contacts and labels
        val allianceContactsLabelsDeferred = allianceIds.map { (allianceId, characterId) ->
            async { allianceId to esiApi.getAlliancesIdContactsLabels(characterId, allianceId) }
        }
        val corporationContactsLabelsDeferred = corporationIds.map { (corporationId, characterId) ->
            async { corporationId to esiApi.getCorporationsIdContactsLabels(characterId, corporationId) }
        }
        val characterContactsLabelsDeferred = characterIds.map { characterId ->
            async { characterId to esiApi.getCharactersIdContactsLabels(characterId) }
        }
        val allianceContactsDeferred = allianceIds.map { (allianceId, characterId) ->
            async { allianceId to esiApi.getAlliancesIdContacts(characterId, allianceId) }
        }
        val corporationContactsDeferred = corporationIds.map { (corporationId, characterId) ->
            async { corporationId to esiApi.getCorporationsIdContacts(characterId, corporationId) }
        }
        val characterContactsDeferred = characterIds.map { characterId ->
            async { characterId to esiApi.getCharactersIdContacts(characterId) }
        }

        // Await contacts
        val allianceContactsList = allianceContactsDeferred.awaitAll().associate { (allianceId, result) ->
            allianceId to (result.success ?: return@coroutineScope null)
        }
        val corporationContactsList = corporationContactsDeferred.awaitAll().associate { (corporationId, result) ->
            corporationId to (result.success ?: return@coroutineScope null)
        }
        val characterContactsList = characterContactsDeferred.awaitAll().associate { (characterId, result) ->
            characterId to (result.success ?: return@coroutineScope null)
        }

        // Request contact and owner names
        val ownerIds = allianceContactsList.keys + corporationContactsList.keys + characterContactsList.keys
        val contactIds = (allianceContactsList.values + corporationContactsList.values + characterContactsList.values)
            .flatMap { it.map { it.contactId } }
        val names = (ownerIds + contactIds).distinct().chunked(1000).flatMap { ids ->
            esiApi.postUniverseNames(ids).success
                ?.map { it.id to it.name } ?: return@coroutineScope null
        }.toMap()

        // Await labels
        val allianceLabels = allianceContactsLabelsDeferred.awaitAll().associate { (allianceId, result) ->
            val labels = result.success ?: return@coroutineScope null
            Entity(allianceId, names[allianceId]!!, EntityType.Alliance) to labels.map { Label(it.labelId, it.labelName) }
        }
        val corporationLabels = corporationContactsLabelsDeferred.awaitAll().associate { (corporationId, result) ->
            val labels = result.success ?: return@coroutineScope null
            Entity(corporationId, names[corporationId]!!, EntityType.Corporation) to labels.map { Label(it.labelId, it.labelName) }
        }
        val characterLabels = characterContactsLabelsDeferred.awaitAll().associate { (characterId, result) ->
            val labels = result.success ?: return@coroutineScope null
            Entity(characterId, names[characterId]!!, EntityType.Character) to labels.map { Label(it.labelId, it.labelName) }
        }

        // Process into models
        val allianceContacts = allianceContactsList.flatMap { (allianceId, list) ->
            val labels = allianceLabels.entries
                .firstOrNull { it.key.id == allianceId }
                ?.value
                ?.associate { it.id to it.name } ?: emptyMap()
            list.mapNotNull { it.toContact(allianceId, EntityType.Alliance, labels, names) }
        }
        val corporationContacts = corporationContactsList.flatMap { (corporationId, list) ->
            val labels = corporationLabels.entries
                .firstOrNull { it.key.id == corporationId }
                ?.value
                ?.associate { it.id to it.name } ?: emptyMap()
            list.mapNotNull { it.toContact(corporationId, EntityType.Corporation, labels, names) }
        }
        val characterContacts = characterContactsList.flatMap { (characterId, list) ->
            val labels = characterLabels.entries
                .firstOrNull { it.key.id == characterId }
                ?.value
                ?.associate { it.id to it.name } ?: emptyMap()
            list.mapNotNull { it.toContact(characterId, EntityType.Character, labels, names) }
        }

        val contacts = allianceContacts + corporationContacts + characterContacts
        val labels = characterLabels + corporationLabels + allianceLabels
        return@coroutineScope ContactsResponse(contacts, labels)
    }

    private fun ContactDto.toContact(ownerId: Int, ownerType: EntityType, labels: Map<Long, String>, names: Map<Int, String>): Contact? {
        if (isNpcAgent(contactId)) return null
        val entity = Entity(
            id = contactId,
            name = names[contactId] ?: contactId.toString(), // TODO
            type = when (contactType) {
                ContactTypeDto.Character -> EntityType.Character
                ContactTypeDto.Corporation -> EntityType.Corporation
                ContactTypeDto.Alliance -> EntityType.Alliance
                ContactTypeDto.Faction -> EntityType.Faction
            },
        )
        val owner = Entity(
            id = ownerId,
            name = names[ownerId] ?: ownerId.toString(), // TODO
            type = ownerType,
        )
        return Contact(
            entity = entity,
            owner = owner,
            labels = labelIds?.map { labels[it] ?: it.toString() } ?: emptyList(), // TODO
            isBlocked = isBlocked == true,
            isWatched = isWatched == true,
            standing = standing,
            standingLevel = getStandingLevel(standing),
        )
    }
}
