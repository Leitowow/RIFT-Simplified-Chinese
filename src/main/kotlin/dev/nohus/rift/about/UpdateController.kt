package dev.nohus.rift.about

import dev.nohus.rift.about.UpdateController.UpdateAvailability.NOT_PACKAGED
import org.koin.core.annotation.Single

@Single
class UpdateController {

    enum class UpdateAvailability {
        NOT_PACKAGED,
        UPDATE_MANUAL,
        UPDATE_AUTOMATIC,
    }

    suspend fun isUpdateAvailable(): UpdateAvailability = NOT_PACKAGED
}
