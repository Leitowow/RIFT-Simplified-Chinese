package dev.nohus.rift.repositories

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.UniverseNamesCategory
import dev.nohus.rift.network.requests.Originator
import org.koin.core.annotation.Single

@Single
class NamesRepository(
    private val esiApi: EsiApi,
) {

    private var names: Map<Long, String> = emptyMap()
    private val categories = mutableMapOf<Long, UniverseNamesCategory>()

    /**
     * Returns an already known name for the ID
     */
    fun getName(id: Int): String? {
        return names[id.toLong()]
    }

    @JvmName("getNameLong")
    fun getName(id: Long): String? {
        return names[id]
    }

    fun getCategory(id: Int): UniverseNamesCategory? {
        return categories[id.toLong()]
    }

    @JvmName("getCategoryLong")
    fun getCategory(id: Long): UniverseNamesCategory? {
        return categories[id]
    }

    suspend fun resolveNames(originator: Originator, ids: List<Int>) {
        resolveNames(originator, ids.map { it.toLong() })
    }

    @JvmName("resolveNamesLong")
    suspend fun resolveNames(originator: Originator, ids: List<Long>) {
        resolveNames(originator, ids.toSet())
    }

    suspend fun resolveNames(originator: Originator, ids: Set<Long>) {
        val newNames = mutableMapOf<Long, String>()
        ids
            .filter { it !in names }
            .filterNot { IdRanges.isSpawnedItem(it) }
            .chunked(1000)
            .flatMap { typeIds ->
                when (val result = esiApi.postUniverseNames(originator, typeIds)) {
                    is Result.Success -> result.data
                    is Result.Failure -> emptyList()
                }
            }
            .forEach {
                newNames += it.id to it.name
                categories += it.id to it.category
            }
        if (newNames.isNotEmpty()) names += newNames
    }
}
