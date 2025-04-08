package dev.nohus.rift.alerts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import dev.nohus.rift.alerts.AlertsTriggerController.AlertLocationMatch
import dev.nohus.rift.gamelogs.GameLogAction
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.logging.analytics.Analytics
import dev.nohus.rift.logs.parse.ChannelChatMessage
import dev.nohus.rift.notifications.NotificationsController
import dev.nohus.rift.notifications.NotificationsController.Notification
import dev.nohus.rift.notifications.system.SendNotificationUseCase
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.push.PushNotificationController
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.repositories.character.CharactersRepository
import dev.nohus.rift.utils.formatDurationLong
import dev.nohus.rift.utils.sound.SoundPlayer
import dev.nohus.rift.utils.sound.SoundsRepository
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.absolutePathString

@Single
class AlertsActionController(
    private val sendNotificationUseCase: SendNotificationUseCase,
    private val pushNotificationController: PushNotificationController,
    private val soundPlayer: SoundPlayer,
    private val soundsRepository: SoundsRepository,
    private val notificationsController: NotificationsController,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
    private val charactersRepository: CharactersRepository,
    private val windowManager: WindowManager,
    private val analytics: Analytics,
) {

    private val scope = CoroutineScope(Job())
    private var lastSoundTimestamp = Instant.EPOCH

    fun triggerIntelAlert(
        alert: Alert,
        matchingEntities: List<Pair<IntelReportType, List<SystemEntity>>>,
        entities: List<SystemEntity>,
        locationMatch: AlertLocationMatch,
        solarSystem: String,
    ) {
        val title = getNotificationTitle(matchingEntities)
        val message = getNotificationMessage(locationMatch)
        val notification = Notification.IntelNotification(title, locationMatch, entities, solarSystem)
        triggerAlert(alert, notification, title, message)
    }

    fun triggerGameActionAlert(alert: Alert, action: GameLogAction, characterId: Int) {
        val title = getNotificationTitle(action)
        val message = getNotificationMessage(action)
        val type = getNotificationItemType(action)
        val notification = Notification.TextNotification(title, message, characterId, type)
        triggerAlert(alert, notification, title, message.toString())
    }

    fun triggerChatMessageAlert(alert: Alert, chatMessage: ChannelChatMessage, highlight: String?) {
        scope.launch {
            val characterId = charactersRepository.getCharacterId(chatMessage.chatMessage.author)
            val title = "Chat message in ${chatMessage.metadata.channelName}"
            val message = "${chatMessage.chatMessage.author}: ${chatMessage.chatMessage.message}"
            val notification = Notification.ChatMessageNotification(
                channel = chatMessage.metadata.channelName,
                messages = listOf(
                    Notification.ChatMessage(
                        message = chatMessage.chatMessage.message,
                        highlight = highlight,
                        sender = chatMessage.chatMessage.author,
                        senderCharacterId = characterId,
                    ),
                ),
            )
            triggerAlert(alert, notification, title, message)
        }
    }

    fun triggerJabberMessageAlert(alert: Alert, chat: String, sender: String, message: String, highlight: String?) {
        scope.launch {
            val title = "Jabber message in $chat"
            val notification = Notification.JabberMessageNotification(
                chat = chat,
                message = message,
                highlight = highlight,
                sender = sender,
            )
            triggerAlert(alert, notification, title, message)
        }
    }

    fun triggerJabberPingAlert(alert: Alert) {
        triggerAlert(alert, null, "", "")
    }

    @OptIn(ExperimentalTextApi::class)
    fun triggerInactiveChannelAlert(alert: Alert, triggeredInactiveChannels: List<String>) {
        val styleTag = Notification.TextNotification.styleTag
        val styleValue = Notification.TextNotification.styleValue
        val message = if (triggeredInactiveChannels.size == 1) {
            buildAnnotatedString {
                append("频道 ")
                withAnnotation(styleTag, styleValue) {
                    append(triggeredInactiveChannels.single())
                }
                append(" 似乎处于非活动状态")
            }
        } else {
            buildAnnotatedString {
                append("频道 ")
                withAnnotation(styleTag, styleValue) {
                    append(triggeredInactiveChannels.joinToString())
                }
                append(" 似乎处于非活动状态")
            }
        }
        val title = "未收到情报"
        val notification = Notification.TextNotification(title, message, null, null)
        triggerAlert(alert, notification, title, message.toString())
    }

    @OptIn(ExperimentalTextApi::class)
    fun triggerPlanetaryIndustryAlert(alert: Alert, colonyItem: ColonyItem) {
        val duration = Duration.between(Instant.now(), colonyItem.ffwdColony.currentSimTime)
        val isInFuture = duration >= Duration.ofMinutes(5)
        val title = if (isInFuture) {
            "你的殖民地将需要关注"
        } else {
            "你的殖民地需要关注"
        }
        val styleTag = Notification.TextNotification.styleTag
        val styleValue = Notification.TextNotification.styleValue
        val riftMessage = buildAnnotatedString {
            append("行星 ")
            withAnnotation(styleTag, styleValue) {
                append(colonyItem.colony.planet.name)
            }
            if (isInFuture) {
                append(" 将在 ")
                withAnnotation(styleTag, styleValue) {
                    append(formatDurationLong(duration))
                }
                append(" 后需要关注")
            }
        }
        val systemMessage = buildString {
            if (colonyItem.characterName != null) {
                append(colonyItem.characterName)
                append(": ")
            }
            append("行星 ")
            append(colonyItem.colony.planet.name)
            if (isInFuture) {
                append(" 将在 ${formatDurationLong(duration)} 后需要关注")
            }
        }
        val planetType = typesRepository.getType(colonyItem.colony.planet.type.typeId)
        val notification = Notification.TextNotification(title, riftMessage, colonyItem.colony.characterId, planetType)
        triggerAlert(alert, notification, title, systemMessage)
    }

    private fun triggerAlert(alert: Alert, notification: Notification?, title: String, message: String) {
        analytics.alertTriggered()
        alert.actions.forEach { action ->
            when (action) {
                AlertAction.RiftNotification -> if (notification != null) sendRiftNotification(notification)
                AlertAction.SystemNotification -> sendSystemNotification(title, message)
                AlertAction.PushNotification -> sendPushNotification(title, message)
                is AlertAction.Sound -> {
                    withSoundCooldown {
                        val sound = soundsRepository.getSounds().firstOrNull { it.id == action.id } ?: return@withSoundCooldown
                        soundPlayer.play(sound.resource)
                    }
                }
                is AlertAction.CustomSound -> {
                    withSoundCooldown {
                        soundPlayer.playFile(action.path)
                    }
                }
                AlertAction.ShowPing -> windowManager.onWindowOpen(WindowManager.RiftWindow.Pings)
                AlertAction.ShowColonies -> windowManager.onWindowOpen(WindowManager.RiftWindow.PlanetaryIndustry)
            }
        }
    }

    private fun withSoundCooldown(block: () -> Unit) {
        val duration = Duration.between(lastSoundTimestamp, Instant.now())
        if (duration >= Duration.ofMillis(200)) {
            lastSoundTimestamp = Instant.now()
            block()
        }
    }

    private fun getNotificationTitle(matchingEntities: List<Pair<IntelReportType, List<SystemEntity>>>): String {
        return matchingEntities.map { it.first }.let {
            if (IntelReportType.GateCamp in it) {
                "报告了门神"
            } else if (IntelReportType.AnyCharacter in it) {
                "报告了敌对"
            } else if (IntelReportType.AnyShip in it) {
                "报告了敌对舰船"
            } else if (IntelReportType.Bubbles in it) {
                "报告了泡泡"
            } else if (IntelReportType.Wormhole in it) {
                "报告了虫洞"
            } else {
                "预警提示"
            }
        }
    }

    /**
     * Only used for system notifications which cannot show rich content
     */
    private fun getNotificationMessage(locationMatch: AlertLocationMatch): String {
        val message = when (locationMatch) {
            is AlertLocationMatch.System -> {
                val distanceText = when (val distance = locationMatch.distance) {
                    0 -> "在星系内"
                    1 -> "距离1跳"
                    else -> "距离${distance}跳"
                }
                val systemName = solarSystemsRepository.getSystemName(locationMatch.systemId) ?: "${locationMatch.systemId}"
                "$distanceText $systemName"
            }

            is AlertLocationMatch.Character -> {
                when (val distance = locationMatch.distance) {
                    0 -> "在你的星系内"
                    1 -> "距离1跳"
                    else -> "距离${distance}跳"
                }
            }
        }
        return message
    }

    private fun getNotificationTitle(action: GameLogAction): String {
        return when (action) {
            is GameLogAction.UnderAttack -> "你有危险"
            is GameLogAction.Attacking -> "你开始攻击"
            is GameLogAction.BeingWarpScrambled -> "你被反跳"
            is GameLogAction.Decloaked -> "你被破隐"
            is GameLogAction.CombatStopped -> "你离开了战斗"
            GameLogAction.CloneJumping -> throw IllegalStateException("Not used")
        }
    }

    @OptIn(ExperimentalTextApi::class)
    private fun getNotificationMessage(action: GameLogAction): AnnotatedString {
        val styleTag = Notification.TextNotification.styleTag
        val styleValue = Notification.TextNotification.styleValue
        return when (action) {
            is GameLogAction.UnderAttack -> buildAnnotatedString {
                append("攻击者是 ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            is GameLogAction.Attacking -> buildAnnotatedString {
                append("目标是 ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            is GameLogAction.BeingWarpScrambled -> buildAnnotatedString {
                append("被 ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
                append(" 扰断")
            }
            is GameLogAction.Decloaked -> buildAnnotatedString {
                withAnnotation(styleTag, styleValue) {
                    append(action.by)
                }
                append(" 距离过近")
            }
            is GameLogAction.CombatStopped -> buildAnnotatedString {
                append("Last target was ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            GameLogAction.CloneJumping -> throw IllegalStateException("Not used")
        }
    }

    private fun getNotificationItemType(action: GameLogAction): Type? {
        return when (action) {
            is GameLogAction.UnderAttack -> typesRepository.getType(action.target)
            is GameLogAction.Attacking -> typesRepository.getType(action.target)
            is GameLogAction.BeingWarpScrambled -> typesRepository.getType(action.target)
            is GameLogAction.Decloaked -> typesRepository.getType(action.by)
            is GameLogAction.CombatStopped -> typesRepository.getType(action.target)
            GameLogAction.CloneJumping -> throw IllegalStateException("Not used")
        }
    }

    private fun sendRiftNotification(notification: Notification) {
        notificationsController.show(notification)
    }

    private fun sendSystemNotification(title: String, message: String) {
        sendNotificationUseCase(
            appName = "RIFT Intel Fusion Tool",
            iconPath = Path.of("icon/icon-512.png").absolutePathString(),
            summary = title,
            body = message,
            timeout = 8,
        )
    }

    private fun sendPushNotification(title: String, message: String) {
        scope.launch {
            pushNotificationController.sendPushNotification(title, message)
        }
    }
}
