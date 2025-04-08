package dev.nohus.rift.standings

import dev.nohus.rift.contacts.ContactsRepository
import dev.nohus.rift.contacts.ContactsRepository.Contact
import dev.nohus.rift.contacts.ContactsRepository.EntityType
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.standings.StandingUtils.getStandingLevel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Single
class StandingsRepository(
    private val contactsRepository: ContactsRepository,
    private val settings: Settings,
) {

    @Serializable
    data class Standings(
        val alliance: Map<Int, Float> = emptyMap(),
        val corporation: Map<Int, Float> = emptyMap(),
        val character: Map<Int, Float> = emptyMap(),
        val friendlyAlliances: Set<Int> = emptySet(),
    )

    private val standings get() = settings.standings

    suspend fun start() = coroutineScope {
        launch {
            contactsRepository.contacts.collect {
                updateStandings(it.contacts)
            }
        }
    }

    fun getStandingLevel(allianceId: Int?, corporationId: Int?, characterId: Int?): Standing {
        return getStandingLevel(getStanding(allianceId, corporationId, characterId))
    }

    fun getStanding(allianceId: Int?, corporationId: Int?, characterId: Int?): Float {
        getStanding(characterId)?.let { return it }
        getStanding(corporationId)?.let { return it }
        getStanding(allianceId)?.let { return it }
        return 0f
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

    private fun updateStandings(contacts: List<Contact>) {
        val standings = getStandings(contacts)
        settings.standings = standings
    }

    private fun getStandings(contacts: List<Contact>): Standings {
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
        return Standings(allianceStandings, corporationStandings, characterStandings, friendlyAlliances)
    }
}
