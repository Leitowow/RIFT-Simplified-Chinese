package dev.nohus.rift.push

import dev.nohus.rift.ViewModel
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.util.Locale

@Factory
class PushViewModel(
    private val pushNotificationController: PushNotificationController,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val dialogMessage: DialogMessage? = null,
        val pushoverApiToken: String,
        val pushoverApiKey: String,
        val ntfyTopic: String,
        val isLoading: Boolean = false,
    )

    private val _state = MutableStateFlow(
        UiState(
            pushoverApiToken = settings.pushover.apiToken ?: "",
            pushoverApiKey = settings.pushover.userKey ?: "",
            ntfyTopic = settings.ntfy.topic ?: "",
        ),
    )
    val state = _state.asStateFlow()

    fun onPushoverApiTokenChanged(token: String) {
        _state.update { it.copy(pushoverApiToken = token) }
        settings.pushover = settings.pushover.copy(apiToken = token)
    }

    fun onPushoverUserKeyChanged(key: String) {
        _state.update { it.copy(pushoverApiKey = key) }
        settings.pushover = settings.pushover.copy(userKey = key)
    }

    fun onNtfyTopicChanged(topic: String) {
        _state.update { it.copy(ntfyTopic = topic) }
        settings.ntfy = settings.ntfy.copy(topic = topic)
    }

    private fun showDialog(title: String, message: String) {
        _state.update {
            it.copy(
                dialogMessage = DialogMessage(
                    title = title,
                    message = message,
                    type = MessageDialogType.Info,
                ),
            )
        }
    }

    fun onNtfySendTest() {
        if (_state.value.isLoading) return

        val topic = settings.ntfy.topic
        if (topic.isNullOrEmpty()) {
            showDialog("缺少主题", "You need to enter a ntfy topic name.")
            return
        }

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = pushNotificationController.sendNtfyNotification(
                title = "RIFT Intel Fusion Tool",
                message = "Congratulations, RIFT is setup correctly for push notifications.",
                iconUrl = null,
            )

            val response = result.success
            if (response != null) {
                showDialog("成功", "Notification sent successfully!")
            } else {
                showDialog("发送失败", result.failure?.message ?: "Unknown error")
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onPushoverSendTest() {
        if (_state.value.isLoading) return

        val token = settings.pushover.apiToken
        val key = settings.pushover.userKey
        if (token.isNullOrEmpty()) {
            showDialog("缺少 API Token", "You need to enter a Pushover API Token.")
            return
        }
        if (key.isNullOrEmpty()) {
            showDialog("缺少 User Key", "You need to enter your Pushover User Key.")
            return
        }

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = pushNotificationController.sendPushoverNotification(
                title = "RIFT Intel Fusion Tool",
                message = "Congratulations, RIFT is setup correctly for push notifications.",
            )

            val response = result.success
            if (response != null) {
                if (response.status == 1) {
                    showDialog("成功", "Notification sent successfully!")
                } else {
                    val reason = response.errors
                        ?.joinToString("\n") { it.replaceFirstChar { it.titlecase(Locale.US) } }
                        ?: "Unknown error"
                    showDialog("发送失败", reason)
                }
            } else {
                showDialog("发送失败", result.failure?.message ?: "Unknown error")
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialogMessage = null) }
    }
}
