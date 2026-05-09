package dev.nohus.rift.dynamicportraits

import dev.nohus.rift.database.static.Backdrops
import dev.nohus.rift.database.static.StaticDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class BackdropsRepository(
    staticDatabase: StaticDatabase,
) {

    data class Backdrop(
        val name: String,
        val bytes: ByteArray,
    )

    private val scope = CoroutineScope(Job())

    private lateinit var backdrops: List<Backdrop>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            val rows = staticDatabase.transaction {
                Backdrops.selectAll().toList()
            }
            backdrops = rows.map {
                Backdrop(
                    name = it[Backdrops.name],
                    bytes = it[Backdrops.bytes],
                )
            }
            hasLoaded.complete(Unit)
        }
    }

    private fun blockUntilLoaded() {
        runBlocking {
            hasLoaded.await()
        }
    }

    fun getBackdrops(): List<Backdrop> {
        blockUntilLoaded()
        return backdrops
    }
}
