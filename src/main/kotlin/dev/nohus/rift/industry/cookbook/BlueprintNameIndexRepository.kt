package dev.nohus.rift.industry.cookbook

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class BlueprintNameIndexRepository(
    private val typesRepository: TypesRepository,
) {

    data class BlueprintNameEntry(
        val typeId: Int,
        val enName: String,
        val zhName: String? = null,
        val aliases: Set<String>,
    )

    private data class ChineseAliasData(
        val zhName: String,
        val aliases: List<String>,
    )

    private data class IndexedEntry(
        val entry: BlueprintNameEntry,
        val normalizedTerms: Set<String>,
    )

    private val indexedByTypeId: MutableMap<Int, IndexedEntry> = mutableMapOf()
    private val exactTermToTypeIds: MutableMap<String, MutableSet<Int>> = mutableMapOf()
    private val chineseAliasByTypeId: Map<Int, ChineseAliasData> = loadChineseAliases()

    init {
        buildInitialIndex()
    }

    /**
     * Registers additional aliases for type IDs.
     * This allows adding Chinese names or corp-specific shorthand without rebuilding the full index.
     */
    @Synchronized
    fun registerAliases(aliasesByTypeId: Map<Int, List<String>>) {
        aliasesByTypeId.forEach { (typeId, aliases) ->
            val existing = indexedByTypeId[typeId] ?: return@forEach
            val mergedAliases = existing.entry.aliases + aliases.filter { it.isNotBlank() }
            val mergedTerms = setOf(existing.entry.enName) + mergedAliases
            reindexEntry(
                BlueprintNameEntry(
                    typeId = typeId,
                    enName = existing.entry.enName,
                    zhName = existing.entry.zhName,
                    aliases = mergedAliases,
                ),
                mergedTerms,
            )
        }
    }

    @Synchronized
    fun search(query: String, maxResults: Int = 10): List<BlueprintNameEntry> {
        val normalizedQuery = query.normalizeTerm()
        if (normalizedQuery.isBlank()) return emptyList()

        val exactIds = exactTermToTypeIds[normalizedQuery].orEmpty()
        if (exactIds.isNotEmpty()) {
            return exactIds
                .mapNotNull { indexedByTypeId[it]?.entry }
                .sortedBy { it.enName }
                .take(maxResults)
        }

        return indexedByTypeId.values
            .mapNotNull { indexed ->
                val score = indexed.normalizedTerms.maxOfOrNull { term ->
                    when {
                        term == normalizedQuery -> 100
                        term.startsWith(normalizedQuery) -> 80
                        normalizedQuery.length >= 2 && term.contains(normalizedQuery) -> 60
                        else -> 0
                    }
                } ?: 0
                if (score > 0) indexed.entry to score else null
            }
            .sortedWith(compareByDescending<Pair<BlueprintNameEntry, Int>> { it.second }.thenBy { it.first.enName })
            .map { it.first }
            .take(maxResults)
    }

    @Synchronized
    fun getByTypeId(typeId: Int): BlueprintNameEntry? {
        return indexedByTypeId[typeId]?.entry
    }

    private fun buildInitialIndex() {
        val uniqueNames = typesRepository.getAllTypeNames().distinct()
        uniqueNames.forEach { name ->
            val type = typesRepository.getType(name) ?: return@forEach
            if (!isLikelyBlueprint(type)) return@forEach
            val chinese = chineseAliasByTypeId[type.id]
            val generatedAliases = buildSet {
                val en = type.name
                if (en.endsWith(" Blueprint", ignoreCase = true)) {
                    add(en.removeSuffix(" Blueprint"))
                }
                if (en.endsWith(" Blueprint Copy", ignoreCase = true)) {
                    add(en.removeSuffix(" Blueprint Copy"))
                }
                chinese?.aliases?.forEach { add(it) }
            }
            val entry = BlueprintNameEntry(
                typeId = type.id,
                enName = type.name,
                zhName = chinese?.zhName,
                aliases = generatedAliases,
            )
            reindexEntry(entry, setOf(entry.enName) + generatedAliases)
        }
    }

    /**
     * Prefer robust name-based check, with group fallback for compatibility
     * across SDE variants where group IDs may differ.
     */
    private fun isLikelyBlueprint(type: Type): Boolean {
        return "Blueprint" in type.name || type.groupId == 9
    }

    private fun reindexEntry(
        entry: BlueprintNameEntry,
        terms: Set<String>,
    ) {
        indexedByTypeId[entry.typeId]?.let { previous ->
            previous.normalizedTerms.forEach { term ->
                exactTermToTypeIds[term]?.remove(entry.typeId)
                if (exactTermToTypeIds[term].isNullOrEmpty()) {
                    exactTermToTypeIds.remove(term)
                }
            }
        }

        val normalizedTerms = terms
            .map { it.normalizeTerm() }
            .filter { it.isNotBlank() }
            .toSet()

        normalizedTerms.forEach { term ->
            exactTermToTypeIds.getOrPut(term) { mutableSetOf() }.add(entry.typeId)
        }

        indexedByTypeId[entry.typeId] = IndexedEntry(entry = entry, normalizedTerms = normalizedTerms)
    }

    private fun String.normalizeTerm(): String {
        return trim()
            .lowercase()
            .replace("　", " ")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
    }

    private fun loadChineseAliases(): Map<Int, ChineseAliasData> {
        return runCatching {
            val text = runBlocking {
                String(Res.readBytes("files/blueprints_zh.tsv"))
            }
            text.lineSequence()
                .mapNotNull { line ->
                    if (line.isBlank() || line.startsWith("#")) return@mapNotNull null
                    val parts = line.split('\t')
                    if (parts.size < 3) return@mapNotNull null
                    val typeId = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val zhName = parts[2].trim()
                    if (zhName.isBlank()) return@mapNotNull null
                    val aliases = buildSet {
                        add(zhName)
                        if (zhName.endsWith("蓝图")) {
                            add(zhName.removeSuffix("蓝图").trim())
                        }
                    }.toList().distinct()
                    typeId to ChineseAliasData(
                        zhName = zhName,
                        aliases = aliases,
                    )
                }
                .toMap()
        }.onFailure {
            logger.warn { "Could not load blueprint chinese aliases: ${it.message}" }
        }.getOrDefault(emptyMap())
    }
}
