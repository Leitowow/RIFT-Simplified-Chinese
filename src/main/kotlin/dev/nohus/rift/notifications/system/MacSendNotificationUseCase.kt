package dev.nohus.rift.notifications.system

import dev.nohus.rift.utils.CommandRunner
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class MacSendNotificationUseCase(
    private val commandRunner: CommandRunner,
) : SendNotificationUseCase {

    override fun invoke(appName: String, iconPath: String, summary: String, body: String, timeout: Int) {
        try {
            commandRunner.run(
                "osascript",
                "-e",
                """display notification "$body" with title "$appName" subtitle "$summary"""",
            )
        } catch (e: IllegalStateException) {
            logger.warn { "Could not send osascript notification: ${e.message}" }
        }
    }
}
