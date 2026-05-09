package dev.nohus.rift.repositories.character

import dev.nohus.rift.database.local.CharacterAffiliations
import dev.nohus.rift.database.local.LocalDatabase
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.CharactersAffiliation
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.utils.plusAssign
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single
import java.time.Instant
import kotlin.collections.chunked
import kotlin.time.Duration.Companion.hours

@Single
class CharacterAffiliationRepository(
    private val esiApi: EsiApi,
    private val localDatabase: LocalDatabase,
) {

    /**
     * Character affiliations are a POST request that can't be cached, so we need a custom cache here
     */
    private val cache = Cache.Builder<Int, CharactersAffiliation>()
        .expireAfterAccess(24.hours)
        .maximumCacheSize(10_000)
        .build()

    suspend fun getCharacterAffiliations(originator: Originator, characterIds: List<Int>): Map<Int, CharactersAffiliation> = coroutineScope {
        val cachedCharacters = characterIds.mapNotNull { characterId ->
            characterId to (cache.get(characterId) ?: return@mapNotNull null)
        }.toMap()
        val uncachedCharacters = characterIds.filter { it !in cachedCharacters.keys }

        val savedCharacters = if (uncachedCharacters.isNotEmpty()) {
            getFromDatabase(uncachedCharacters)
                .also { cache += it }
        } else {
            emptyMap()
        }
        val unsavedCharacters = uncachedCharacters.filter { it !in savedCharacters.keys }

        val esiCharacters = unsavedCharacters.chunked(1000).map { chunk ->
            async {
                esiApi.getCharactersAffiliation(originator, chunk).success ?: emptyList()
            }
        }.awaitAll().flatten().associateBy { it.characterId }
            .also { saveToDatabase(it.values) }
            .also { cache += it }

        esiCharacters + savedCharacters + cachedCharacters
    }

    suspend fun getCharacterAffiliation(originator: Originator, characterId: Int): CharactersAffiliation? {
        return getCharacterAffiliations(originator, listOf(characterId))[characterId]
    }

    private suspend fun getFromDatabase(characterIds: List<Int>): Map<Int, CharactersAffiliation> = withContext(Dispatchers.IO) {
        val currentTime = Instant.now().toEpochMilli()
        localDatabase.transaction {
            CharacterAffiliations.selectAll().where {
                CharacterAffiliations.characterId inList characterIds and (
                    CharacterAffiliations.checkTimestamp greater (currentTime - expiryDuration)
                    )
            }.associate { row ->
                row[CharacterAffiliations.characterId] to CharactersAffiliation(
                    characterId = row[CharacterAffiliations.characterId],
                    corporationId = row[CharacterAffiliations.corporationId],
                    allianceId = row[CharacterAffiliations.allianceId],
                    factionId = row[CharacterAffiliations.factionId],
                )
            }
        }
    }

    private suspend fun saveToDatabase(affiliations: Collection<CharactersAffiliation>) = withContext(Dispatchers.IO) {
        val currentTime = Instant.now().toEpochMilli()
        localDatabase.transaction {
            CharacterAffiliations.batchUpsert(affiliations) {
                this[CharacterAffiliations.characterId] = it.characterId
                this[CharacterAffiliations.corporationId] = it.corporationId
                this[CharacterAffiliations.allianceId] = it.allianceId
                this[CharacterAffiliations.factionId] = it.factionId
                this[CharacterAffiliations.checkTimestamp] = currentTime
            }
        }
    }

    companion object {
        val expiryDuration = 24.hours.inWholeMilliseconds
    }
}
