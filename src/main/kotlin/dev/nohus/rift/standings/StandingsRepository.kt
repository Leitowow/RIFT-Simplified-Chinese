package dev.nohus.rift.standings

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.CharacterInfo
import dev.nohus.rift.contacts.ContactsRepository
import dev.nohus.rift.contacts.ContactsRepository.Contact
import dev.nohus.rift.contacts.ContactsRepository.EntityType
import dev.nohus.rift.repositories.IdRanges
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.standings.StandingUtils.getStandingLevel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Single
class StandingsRepository(
    private val charactersRepository: LocalCharactersRepository,
    private val contactsRepository: ContactsRepository,
    private val settings: Settings,
) {

    @Serializable
    data class Standings(
        val alliance: Map<Int, Float> = emptyMap(),
        val corporation: Map<Int, Float> = emptyMap(),
        val character: Map<Int, Float> = emptyMap(),
        val friendlyAlliances: Set<Int> = emptySet(),
        val selfCharacters: Set<Int> = emptySet(),
        val memberCorporations: Set<Int> = emptySet(),
        val memberAlliances: Set<Int> = emptySet(),
    )

    private val standings get() = settings.standings

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            contactsRepository.contacts.collect {
                updateStandings()
            }
        }
        launch {
            charactersRepository.characters.debounce(1000).collect {
                updateStandings()
            }
        }
    }

    fun getStandingLevel(allianceId: Int?, corporationId: Int?, characterId: Int?): Standing {
        return if (characterId in standings.selfCharacters) {
            Standing.Self
        } else if (corporationId in standings.memberCorporations) {
            Standing.Corporation
        } else if (allianceId in standings.memberAlliances) {
            Standing.Alliance
        } else {
            val standing = getStanding(allianceId, corporationId, characterId)
            if (standing != null) {
                getStandingLevel(standing)
            } else {
                Standing.Neutral
            }
        }
    }

    fun getStanding(allianceId: Int?, corporationId: Int?, characterId: Int?): Float? {
        getStanding(characterId)?.let { return it }
        getStanding(corporationId)?.let { return it }
        getStanding(allianceId)?.let { return it }
        return null
    }

    private fun getStanding(id: Int?): Float? {
        if (id == null) return null
        standings.character[id]?.let { return it }
        standings.corporation[id]?.let { return it }
        standings.alliance[id]?.let { return it }
        return null
    }

    fun getFriendlyAllianceIds(): Set<Int> {
        return standings.friendlyAlliances
    }

    private fun updateStandings() {
        val contacts = contactsRepository.contacts.value.contacts
        val characterDetails = charactersRepository.characters.value.mapNotNull { it.info }
        val standings = getStandings(contacts, characterDetails)
        settings.standings = standings
    }

    private fun getStandings(contacts: List<Contact>, characterDetails: List<CharacterInfo>): Standings {
        val allianceStandings = mutableMapOf<Int, Float>()
        val corporationStandings = mutableMapOf<Int, Float>()
        val characterStandings = mutableMapOf<Int, Float>()
        val friendlyAlliances = mutableSetOf<Int>()

        contacts.forEach { contact ->
            if (contact.standing == 0f) return@forEach
            when (contact.owner.type) {
                EntityType.Character -> characterStandings[contact.entity.id] = contact.standing
                EntityType.Corporation -> corporationStandings[contact.entity.id] = contact.standing
                EntityType.Alliance -> allianceStandings[contact.entity.id] = contact.standing
                EntityType.Faction -> {}
            }
            if (contact.entity.type == EntityType.Alliance && contact.standingLevel.isFriendly) {
                friendlyAlliances += contact.entity.id
            }
        }

        val selfCharacters = characterDetails.map { it.characterId }.toSet()
        val memberCorporations = characterDetails.mapNotNull { it.corporationId.takeUnless { IdRanges.isNpcCorporation(it) } }.toSet()
        val memberAlliances = characterDetails.mapNotNull { it.allianceId }.toSet()

        return Standings(
            allianceStandings,
            corporationStandings,
            characterStandings,
            friendlyAlliances + memberAlliances,
            selfCharacters,
            memberCorporations,
            memberAlliances,
        )
    }
}
