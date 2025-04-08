package dev.nohus.rift.push

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.ntfy.Ntfy
import dev.nohus.rift.network.ntfy.NtfyApi
import dev.nohus.rift.network.pushover.Messages
import dev.nohus.rift.network.pushover.MessagesResponse
import dev.nohus.rift.network.pushover.PushoverApi
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class PushNotificationController(
    private val pushoverApi: PushoverApi,
    private val ntfyApi: NtfyApi,
    private val settings: Settings,
) {

    suspend fun sendPushNotification(title: String, message: String) {
        var wasAttempted = false
        if (settings.ntfy.topic != null) {
            wasAttempted = true
            sendNtfyNotification(title, message).map { }
        }
        if (settings.pushover.apiToken != null && settings.pushover.userKey != null) {
            wasAttempted = true
            sendPushoverNotification(title, message).map { }
        }
        if (!wasAttempted) {
            logger.error { "Could not send push notification as no push service is setup" }
        }
    }

    suspend fun sendNtfyNotification(
        title: String,
        message: String,
    ): Result<Unit> {
        val topic = settings.ntfy.topic
        if (topic == null) {
            logger.error { "Cannot send push notification because Ntfy is not configured" }
            return Failure(null)
        }
        return withContext(Dispatchers.IO) {
            ntfyApi.post(
                Ntfy(
                    topic = topic,
                    title = title,
                    message = message,
                    icon = "https://riftforeve.online/download/icon.png",
                ),
            ).also {
                if (it.isSuccess) {
                    logger.info { "Sent ntfy notification" }
                } else {
                    logger.error { "Failed sending ntfy notification: ${it.failure}" }
                }
            }
        }
    }

    suspend fun sendPushoverNotification(
        title: String,
        message: String,
    ): Result<MessagesResponse> {
        val token = settings.pushover.apiToken
        val key = settings.pushover.userKey
        if (token == null || key == null) {
            logger.error { "Cannot send push notification because Pushover is not configured" }
            return Failure(null)
        }
        return withContext(Dispatchers.IO) {
            pushoverApi.postMessages(
                Messages(
                    token = token,
                    user = key,
                    title = title,
                    message = message,
                ),
            ).also {
                if (it.success?.status == 1) {
                    logger.info { "Sent Pushover notification" }
                } else {
                    logger.error { "Failed sending Pushover notification: $it" }
                }
            }
        }
    }
}
