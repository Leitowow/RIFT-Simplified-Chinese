package dev.nohus.rift.about

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.network.HttpGetUseCase
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.whatsnew.VersionUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class CheckForUpdatesUseCase(
    private val settings: Settings,
    private val httpGetUseCase: HttpGetUseCase,
    @Named("network") private val json: Json,
) {

    suspend operator fun invoke() {
        when (val result = httpGetUseCase(Originator.DataPreloading, latestReleaseApiUrl)) {
            is Result.Success -> {
                val latestVersion = parseLatestVersion(result.data)
                val currentVersion = normalizeVersion(BuildConfig.version)
                if (latestVersion == null || currentVersion == null) {
                    logger.warn { "Failed to parse app version for update check; current=$currentVersion latest=$latestVersion" }
                    return
                }
                settings.newVersionSeenTimestamp = if (VersionUtils.isNewer(currentVersion, latestVersion)) {
                    settings.newVersionSeenTimestamp ?: Instant.now()
                } else {
                    null
                }
            }

            is Result.Failure -> {
                logger.debug { "Version check failed: ${result.cause?.message}" }
            }
        }
    }

    private fun parseLatestVersion(payload: String): String? {
        val rawTag = runCatching {
            json.parseToJsonElement(payload)
                .jsonObject["tag_name"]
                ?.jsonPrimitive
                ?.content
        }.getOrNull()
        return normalizeVersion(rawTag)
    }

    private fun normalizeVersion(version: String?): String? {
        val match = version?.trim()?.let { versionRegex.find(it) } ?: return null
        return match.groupValues[1]
    }

    companion object {
        private const val latestReleaseApiUrl = "https://api.github.com/repos/Leitowow/RIFT-Simplified-Chinese/releases/latest"
        private val versionRegex = Regex("""^v?(\d+\.\d+\.\d+)""")
    }
}
