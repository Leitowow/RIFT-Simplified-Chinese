package dev.nohus.rift.startupwarning

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class IsRunningOldVersionUseCase(
    private val settings: Settings,
) {

    operator fun invoke(): Boolean {
        return settings.newVersionSeenTimestamp != null
    }
}
