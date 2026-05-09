package dev.nohus.rift.startupwarning

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

@Single
class IsRunningOldVersionUseCase(
    private val settings: Settings,
) {

    private val minAgeDuration = Duration.ofDays(7)

    operator fun invoke(): Boolean {
        val newVersionSeen = settings.newVersionSeenTimestamp
        return newVersionSeen != null && newVersionSeen.isBefore(Instant.now() - minAgeDuration)
    }
}
