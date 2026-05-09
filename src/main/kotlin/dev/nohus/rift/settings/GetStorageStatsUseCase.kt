package dev.nohus.rift.settings

import dev.nohus.rift.utils.directories.AppDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.walk

private val logger = KotlinLogging.logger {}

@Single
class GetStorageStatsUseCase(
    private val appDirectories: AppDirectories,
) {

    data class StorageStats(
        val dataDirectory: Path?,
        val cacheDirectory: Path?,
        val dataSize: Long?,
        val zkillCacheSize: Long?,
        val esiCacheSize: Long?,
        val httpCacheSize: Long?,
        val portraitsSize: Long?,
        val portraitsCount: Int?,
        val otherCacheSize: Long?,
    )

    operator fun invoke(): Flow<StorageStats> = channelFlow {
        val dataDirectory = appDirectories.getAppDataDirectory()
        val cacheDirectory = appDirectories.getAppCacheDirectory()
        send(StorageStats(dataDirectory, cacheDirectory, null, null, null, null, null, null, null))

        coroutineScope {
            val mutex = Mutex()
            var currentStats = StorageStats(dataDirectory, cacheDirectory, null, null, null, null, null, null, null)

            suspend fun updateAndEmit(update: StorageStats.() -> StorageStats) {
                mutex.withLock {
                    currentStats = currentStats.update()
                    send(currentStats)
                }
            }

            val dataSizeDeferred = async(Dispatchers.IO) {
                val size = getFullSize(dataDirectory)
                updateAndEmit { copy(dataSize = size) }
            }

            val cacheSizeDeferred = async(Dispatchers.IO) {
                getFullSize(cacheDirectory)
            }

            val zkillCacheSizeDeferred = async(Dispatchers.IO) {
                val size = getFullSize(cacheDirectory.resolve("zkill-cache"))
                updateAndEmit { copy(zkillCacheSize = size) }
                size
            }

            val esiCacheSizeDeferred = async(Dispatchers.IO) {
                val size = getFullSize(cacheDirectory.resolve("esi-cache"))
                updateAndEmit { copy(esiCacheSize = size) }
                size
            }

            val httpCacheSizeDeferred = async(Dispatchers.IO) {
                val size = getFullSize(cacheDirectory.resolve("http-cache"))
                updateAndEmit { copy(httpCacheSize = size) }
                size
            }

            val portraitsSizeDeferred = async(Dispatchers.IO) {
                val size = getFullSize(cacheDirectory.resolve("portraits"))
                updateAndEmit { copy(portraitsSize = size) }
                size
            }

            val portraitsCountDeferred = async(Dispatchers.IO) {
                val count = cacheDirectory.resolve("portraits")
                    .listDirectoryEntries()
                    .count { it.isDirectory() }
                updateAndEmit { copy(portraitsCount = count) }
            }

            dataSizeDeferred.await()
            portraitsCountDeferred.await()

            val cacheSize = cacheSizeDeferred.await()
            val zkillCacheSize = zkillCacheSizeDeferred.await()
            val esiCacheSize = esiCacheSizeDeferred.await()
            val httpCacheSize = httpCacheSizeDeferred.await()
            val portraitsSize = portraitsSizeDeferred.await()

            val otherCacheSize = cacheSize - zkillCacheSize - esiCacheSize - httpCacheSize - portraitsSize
            updateAndEmit { copy(otherCacheSize = otherCacheSize) }
        }
    }

    private fun getFullSize(path: Path): Long {
        return try {
            path.walk().sumOf {
                try {
                    it.fileSize()
                } catch (e: IOException) {
                    logger.error { "Failed getting file size for: $it, ${e.message}" }
                    0L
                }
            }
        } catch (e: IOException) {
            logger.error { "Failed getting size of directory: $path, ${e.message}" }
            0L
        }
    }
}
