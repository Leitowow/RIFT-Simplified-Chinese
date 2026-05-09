package dev.nohus.rift.repositories.character

import dev.nohus.rift.network.imageserver.ImageServerApi
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.character.CharacterStatus.Active
import dev.nohus.rift.repositories.character.CharacterStatus.Dormant
import dev.nohus.rift.repositories.character.CharacterStatus.Inactive
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Single
class CharacterActivityRepository(
    private val imageServerApi: ImageServerApi,
    private val zkillboardRecentActivityRepository: ZkillboardRecentActivityRepository,
) {

    private var emptyPortraitEtag: String? = null

    suspend fun getActivityStatus(originator: Originator, characterId: Int): CharacterStatus.Exists {
        val zKillboardActiveCharacters = zkillboardRecentActivityRepository.activeCharacterIds
        if (zKillboardActiveCharacters != null) {
            if (characterId in zKillboardActiveCharacters) return Active(characterId)
        }
        val emptyEtag = getEmptyPortraitEtag(originator) ?: return Active(characterId) // Cannot check, assume active
        val etag = getPortraitEtag(originator, characterId) ?: return Active(characterId) // Cannot check, assume active
        return if (etag != emptyEtag) {
            // Has logged in since Incarna (2011)
            if (zKillboardActiveCharacters != null) {
                // But hasn't appeared on a killmail in the last 90 days
                Inactive(characterId)
            } else {
                // Assume active if we don't have zKillboard data
                Active(characterId)
            }
        } else {
            // Not logged in since Incarna (2011)
            Dormant(characterId)
        }
    }

    private suspend fun getEmptyPortraitEtag(originator: Originator): String? {
        var emptyEtag = emptyPortraitEtag
        if (emptyEtag == null) {
            emptyEtag = getPortraitEtag(originator, 1).also { emptyPortraitEtag = it }
        }
        return emptyEtag
    }

    private suspend fun getPortraitEtag(originator: Originator, characterId: Int): String? {
        try {
            val response = imageServerApi.headCharacterPortrait(originator, characterId)
            return response.takeIf { it.isSuccessful }?.headers()?.get("etag")
        } catch (e: IOException) {
            logger.error(e) { "Unable to get portrait etag" }
            return null
        }
    }
}
