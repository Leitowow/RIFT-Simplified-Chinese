package dev.nohus.rift.whatsnew

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.database.local.LocalDatabase
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.sso.authentication.EveSsoRepository
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import org.koin.core.annotation.Single

@Single
class WhatsNewController(
    private val windowManager: WindowManager,
    private val settings: Settings,
    private val eveSsoRepository: EveSsoRepository,
    private val localDatabase: LocalDatabase,
) {

    fun showIfRequired() {
        val lastShownVersion = settings.whatsNewVersion
        val currentVersion = BuildConfig.version
        if (lastShownVersion != null && lastShownVersion != currentVersion) {
            runMigrations(lastShownVersion)
            val hasChangelog = WhatsNew.getVersions().any {
                VersionUtils.isNewer(lastShownVersion, it.version)
            }
            if (hasChangelog) {
                windowManager.onWindowOpen(RiftWindow.WhatsNew)
            }
        }
        resetWhatsNewVersion()
    }

    fun resetWhatsNewVersion() {
        settings.whatsNewVersion = BuildConfig.version
    }

    private fun runMigrations(lastVersion: String) {
        val migrations = listOf(
            "4.8.0" to {
                // New scope added
                eveSsoRepository.removeAllAuthentications()
            },
            "4.13.0" to {
                localDatabase.dropOldTable()
            },
        )
        migrations.forEach { (version, migration) ->
            if (VersionUtils.isNewer(lastVersion, version)) {
                migration()
            }
        }
    }
}
