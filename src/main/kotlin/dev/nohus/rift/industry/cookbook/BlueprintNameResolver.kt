package dev.nohus.rift.industry.cookbook

import org.koin.core.annotation.Single

@Single
class BlueprintNameResolver(
    private val blueprintNameIndexRepository: BlueprintNameIndexRepository,
) {

    data class Match(
        val typeId: Int,
        val enName: String,
        val zhName: String? = null,
    ) {
        val displayName: String get() = enName
    }

    sealed interface ResolveResult {
        data class Resolved(val match: Match) : ResolveResult
        data class Ambiguous(val matches: List<Match>) : ResolveResult
        data object NotFound : ResolveResult
    }

    fun resolve(query: String, maxResults: Int = 8): ResolveResult {
        val matches = search(query, maxResults)
        return when (matches.size) {
            0 -> ResolveResult.NotFound
            1 -> ResolveResult.Resolved(matches.single())
            else -> ResolveResult.Ambiguous(matches)
        }
    }

    fun search(query: String, maxResults: Int = 8): List<Match> {
        return blueprintNameIndexRepository
            .search(query = query, maxResults = maxResults)
            .map {
                Match(
                    typeId = it.typeId,
                    enName = it.enName,
                    zhName = it.zhName,
                )
            }
    }

    fun getByTypeId(typeId: Int): Match? {
        return blueprintNameIndexRepository.getByTypeId(typeId)?.let {
            Match(
                typeId = it.typeId,
                enName = it.enName,
                zhName = it.zhName,
            )
        }
    }

    /**
     * Register aliases such as Chinese item names:
     * mapOf(2047 to listOf("暴风级蓝图"))
     */
    fun registerAliases(aliasesByTypeId: Map<Int, List<String>>) {
        blueprintNameIndexRepository.registerAliases(aliasesByTypeId)
    }
}
