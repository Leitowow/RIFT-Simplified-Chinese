package dev.nohus.rift.about

import dev.nohus.rift.about.UpdateController.UpdateAvailability.NOT_PACKAGED
import dev.nohus.rift.about.UpdateController.UpdateAvailability.NO_UPDATE
import dev.nohus.rift.about.UpdateController.UpdateAvailability.UNKNOWN
import dev.nohus.rift.about.UpdateController.UpdateAvailability.UPDATE_AUTOMATIC
import dev.nohus.rift.about.UpdateController.UpdateAvailability.UPDATE_MANUAL
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.time.Instant

@Single
class CheckForUpdatesUseCase(
    private val updateController: UpdateController,
    private val settings: Settings,
) {

    suspend operator fun invoke() {
        val updateAvailability = updateController.isUpdateAvailable()
        val isUpdateAvailable = when (updateAvailability) {
            NOT_PACKAGED, UNKNOWN -> null
            NO_UPDATE -> false
            UPDATE_MANUAL, UPDATE_AUTOMATIC -> true
        }
        if (isUpdateAvailable != null) {
            val newVersionSeen = settings.newVersionSeenTimestamp
            if (newVersionSeen == null && isUpdateAvailable) {
                // There is a newer version available, remember it
                settings.newVersionSeenTimestamp = Instant.now()
            } else if (newVersionSeen != null && !isUpdateAvailable) {
                // There was an update remembered before, but we are on the latest version now
                settings.newVersionSeenTimestamp = null
            }
        }
    }
}
