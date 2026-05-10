package dev.nohus.rift.about

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class CheckForUpdatesUseCase(
    private val settings: Settings,
) {

    suspend operator fun invoke() {
        // Version checks intentionally disabled.
        settings.newVersionSeenTimestamp = null
    }
}
