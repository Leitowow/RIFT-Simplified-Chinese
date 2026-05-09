package dev.nohus.rift.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class ResetSparkleUpdateCheckUseCase(
    private val operatingSystem: OperatingSystem,
    private val commandRunner: CommandRunner,
) {

    /**
     * Force Sparkle to recheck for updates on the next start
     */
    operator fun invoke() {
        if (operatingSystem == OperatingSystem.MacOs) {
            try {
                commandRunner.run("defaults", "delete", "dev.nohus.rift", "SULastCheckTime")
            } catch (e: IllegalStateException) {
                logger.warn { "Failed to reset Sparkle update check: ${e.message}" }
            }
        }
    }
}
