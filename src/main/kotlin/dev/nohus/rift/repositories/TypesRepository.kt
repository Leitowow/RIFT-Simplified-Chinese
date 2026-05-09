package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.database.static.TypeCategories
import dev.nohus.rift.database.static.TypeDogmas
import dev.nohus.rift.database.static.TypeGroups
import dev.nohus.rift.database.static.Types
import dev.nohus.rift.network.requests.Originator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class TypesRepository(
    staticDatabase: StaticDatabase,
    private val namesRepository: NamesRepository,
) {

    data class Type(
        val id: Int,
        val groupId: Int,
        val categoryId: Int,
        val name: String,
        val volume: Float,
        val radius: Float?,
        val repackagedVolume: Float?,
        val iconId: Int,
        val dogmas: Dogmas,
    )

    data class Dogmas(
        val entityOverviewShipGroupId: Int?,
    )

    private val scope = CoroutineScope(Job())

    /**
     * Names resolved from ESI for types not in the SDE
     */
    private lateinit var types: Map<Int, Type>
    private lateinit var typeIds: Map<String, Int>
    private lateinit var groupNames: Map<Int, String>
    private lateinit var categoryNames: Map<Int, String>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            val rows = staticDatabase.transaction {
                Types.selectAll().toList()
            }
            val dogmaRows = staticDatabase.transaction {
                TypeDogmas.selectAll().toList()
            }.associateBy { it[TypeDogmas.typeId] }
            types = rows.associate {
                val id = it[Types.typeId]
                id to Type(
                    id = id,
                    groupId = it[Types.groupId],
                    categoryId = it[Types.categoryId],
                    name = it[Types.typeName],
                    volume = it[Types.volume],
                    radius = it[Types.radius],
                    repackagedVolume = it[Types.repackagedVolume],
                    iconId = it[Types.iconId] ?: it[Types.typeId],
                    dogmas = Dogmas(
                        entityOverviewShipGroupId = dogmaRows[id]?.get(TypeDogmas.entityOverviewShipGroupId),
                    ),
                )
            }
            val groupRows = staticDatabase.transaction {
                TypeGroups.selectAll().toList()
            }
            groupNames = groupRows.associate {
                it[TypeGroups.groupId] to it[TypeGroups.groupName]
            }
            val categoryRows = staticDatabase.transaction {
                TypeCategories.selectAll().toList()
            }
            categoryNames = categoryRows.associate {
                it[TypeCategories.categoryId] to it[TypeCategories.categoryName]
            }
            typeIds = rows.groupBy { it[Types.typeName] }.map { (name, rows) ->
                name to if (rows.size == 1) {
                    rows.single()[Types.typeId]
                } else {
                    // Duplicate type names
                    rows.maxByOrNull {
                        // Prefer ships
                        it[Types.categoryId] in listOf(6)
                    }!![Types.typeId]
                }
            }.toMap()
            hasLoaded.complete(Unit)
        }
    }

    private fun blockUntilLoaded() {
        runBlocking {
            hasLoaded.await()
        }
    }

    private fun getTypes(): Map<Int, Type> {
        blockUntilLoaded()
        return types
    }

    private fun getTypeIds(): Map<String, Int> {
        blockUntilLoaded()
        return typeIds
    }

    fun getAllTypeNames(): List<String> {
        return getTypes().values.map { it.name }
    }

    fun getTypeId(name: String): Int? {
        return getTypeIds()[name]
    }

    fun getTypeName(id: Int): String? {
        return getType(id)?.name ?: namesRepository.getName(id)
    }

    fun getType(name: String): Type? {
        return getTypeId(name)?.let { getType(it) }
    }

    fun getType(id: Int): Type? {
        return getTypes()[id]
    }

    fun getTypeOrPlaceholder(id: Int): Type {
        return getType(id) ?: Type(
            id = id,
            groupId = -1,
            categoryId = -1,
            name = namesRepository.getName(id) ?: "Unknown",
            volume = 0f,
            radius = null,
            repackagedVolume = null,
            iconId = -1,
            dogmas = Dogmas(null),
        )
    }

    fun getGroupName(id: Int): String? {
        blockUntilLoaded()
        return groupNames[id]
    }

    fun getCategoryName(id: Int): String? {
        blockUntilLoaded()
        return categoryNames[id]
    }

    /**
     * Tries to find a type name in arbitrary text
     */
    fun findTypeInText(message: String): Type? {
        return findTypesInText(message).firstOrNull()
    }

    /**
     * Tries to find type names in arbitrary text
     */
    fun findTypesInText(message: String): Sequence<Type> {
        return sequence {
            val words = message.split("[\\s,.;]+".toRegex())
            outer@for (startIndex in words.indices) {
                val longest = words.drop(startIndex).takeWhile { it.isNotEmpty() && !it[0].isLowerCase() }
                if (longest.isEmpty()) continue
                for (length in longest.size downTo 1) {
                    val candidate = longest.take(length).joinToString(" ")
                    val type = getType(candidate)
                    if (type != null) {
                        yield(type)
                        continue@outer
                    }
                }
            }
        }
    }

    suspend fun resolveNamesFromEsi(originator: Originator, ids: List<Int>) {
        namesRepository.resolveNames(originator, ids)
    }
}
