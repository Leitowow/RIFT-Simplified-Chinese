package dev.nohus.rift.repositories.character

import dev.nohus.rift.network.imageserver.ImageServerApi
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

    suspend fun getActivityStatus(characterId: Int): CharacterStatus.Exists {
        val zKillboardActiveCharacters = zkillboardRecentActivityRepository.activeCharacterIds
        if (zKillboardActiveCharacters != null) {
            if (characterId in zKillboardActiveCharacters) return Active(characterId)
        }
        val emptyEtag = getEmptyPortraitEtag() ?: return Active(characterId) // Cannot check, assume active
        val etag = getPortraitEtag(characterId) ?: return Active(characterId) // Cannot check, assume active
        return if (etag != emptyEtag) {
            if (zKillboardActiveCharacters != null) {
                Inactive(characterId)
            } else {
                Active(characterId) // Assume active if we don't have zKillboard data
            }
        } else {
            Dormant(characterId)
        }
    }

    private suspend fun getEmptyPortraitEtag(): String? {
        var emptyEtag = emptyPortraitEtag
        if (emptyEtag == null) {
            emptyEtag = getPortraitEtag(1).also { emptyPortraitEtag = it }
        }
        return emptyEtag
    }

    private suspend fun getPortraitEtag(characterId: Int): String? {
        try {
            val response = imageServerApi.getCharacterPortrait(characterId)
            return response.takeIf { it.isSuccessful }?.headers()?.get("etag")
        } catch (e: IOException) {
            logger.error(e) { "Unable to get portrait etag" }
            return null
        }
    }
}
