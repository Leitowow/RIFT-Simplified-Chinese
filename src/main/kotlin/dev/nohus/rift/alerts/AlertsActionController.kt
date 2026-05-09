package dev.nohus.rift.alerts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import dev.nohus.rift.alerts.AlertsTriggerController.AlertLocationMatch
import dev.nohus.rift.contacts.ContactsRepository
import dev.nohus.rift.gamelogs.GameLogAction
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.logs.parse.ChannelChatMessage
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.notifications.NotificationsController
import dev.nohus.rift.notifications.NotificationsController.Notification
import dev.nohus.rift.notifications.system.SendNotificationUseCase
import dev.nohus.rift.pings.FormupLocation
import dev.nohus.rift.pings.PapType
import dev.nohus.rift.pings.PingModel
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.push.PushNotificationController
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
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
import java.util.Locale
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
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val windowManager: WindowManager,
    private val contactsRepository: ContactsRepository,
) {

    private val scope = CoroutineScope(Job())
    private var lastSoundTimestamp = Instant.EPOCH

    fun triggerIntelAlert(
        alert: Alert,
        matchingEntities: List<Pair<IntelReportType, List<SystemEntity>>>,
        entities: List<SystemEntity>,
        locationMatch: AlertLocationMatch,
        solarSystem: MapSolarSystem,
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
        val iconUrl = if (type != null) {
            "https://images.evetech.net/types/${type.id}/icon"
        } else {
            null
        }
        triggerAlert(alert, notification, title, message.toString(), iconUrl)
    }

    fun triggerChatMessageAlert(alert: Alert, chatMessage: ChannelChatMessage, highlight: String?) {
        scope.launch {
            val characterId = charactersRepository.getCharacterId(Originator.Alerts, chatMessage.chatMessage.author)
            val standing = if (characterId != null) {
                characterDetailsRepository.getCharacterDetails(Originator.Alerts, characterId)?.standingLevel
            } else {
                null
            }
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
                        senderStanding = standing,
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

    fun triggerJabberPingAlert(alert: Alert, ping: PingModel) {
        val (title, message) = when (ping) {
            is PingModel.FleetPing -> {
                val title = buildString {
                    val prefix = when (ping.papType) {
                        PapType.Peacetime -> "Peacetime fleet"
                        PapType.Strategic -> "Strategic fleet"
                        is PapType.Text -> "${ping.papType.text} fleet"
                        else -> "Fleet"
                    }
                    appendLine("$prefix under ${ping.fleetCommander.name}")
                }
                val message = buildString {
                    if (ping.formupLocations.isNotEmpty()) {
                        val formup = ping.formupLocations.joinToString {
                            when (it) {
                                is FormupLocation.System -> solarSystemsRepository.getSystemName(it.id) ?: "${it.id}"
                                is FormupLocation.Text -> it.text
                            }
                        }
                        appendLine("Formup: $formup")
                    }
                    if (ping.doctrine != null) {
                        appendLine("Doctrine: ${ping.doctrine.text}")
                    }
                    appendLine(ping.description)
                }
                title to message
            }
            is PingModel.PlainText -> {
                "Announcement from ${ping.sender}" to ping.text
            }
        }
        val iconUrl = (ping as? PingModel.FleetPing)?.fleetCommander?.id?.let { id ->
            "https://images.evetech.net/characters/$id/portrait"
        }
        triggerAlert(alert, null, title, message, iconUrl)
    }

    fun triggerInactiveChannelAlert(alert: Alert, triggeredInactiveChannels: List<String>) {
        val styleTag = Notification.TextNotification.STYLE_TAG
        val styleValue = Notification.TextNotification.STYLE_VALUE
        val message = if (triggeredInactiveChannels.size == 1) {
            buildAnnotatedString {
                append("Channel ")
                withAnnotation(styleTag, styleValue) {
                    append(triggeredInactiveChannels.single())
                }
                append(" appears inactive")
            }
        } else {
            buildAnnotatedString {
                append("Channels ")
                withAnnotation(styleTag, styleValue) {
                    append(triggeredInactiveChannels.joinToString())
                }
                append(" appear inactive")
            }
        }
        val title = "Not receiving intel"
        val notification = Notification.TextNotification(title, message, null, null)
        triggerAlert(alert, notification, title, message.toString())
    }

    fun triggerPlanetaryIndustryAlert(alert: Alert, colonyItem: ColonyItem) {
        val duration = Duration.between(Instant.now(), colonyItem.ffwdColony.currentSimTime)
        val isInFuture = duration >= Duration.ofMinutes(5)
        val title = if (isInFuture) {
            "Your colony will need attention"
        } else {
            "Your colony needs attention"
        }
        val styleTag = Notification.TextNotification.STYLE_TAG
        val styleValue = Notification.TextNotification.STYLE_VALUE
        val riftMessage = buildAnnotatedString {
            append("Planet ")
            withAnnotation(styleTag, styleValue) {
                append(colonyItem.colony.planet.name)
            }
            if (isInFuture) {
                append(" will need attention in ")
                withAnnotation(styleTag, styleValue) {
                    append(formatDurationLong(duration))
                }
            }
        }
        val systemMessage = buildString {
            if (colonyItem.characterName != null) {
                append(colonyItem.characterName)
                append(": ")
            }
            append("Planet ")
            append(colonyItem.colony.planet.name)
            if (isInFuture) {
                append(" will need attention in ${formatDurationLong(duration)}")
            }
        }
        val planetType = typesRepository.getType(colonyItem.colony.planet.type.typeId)
        val notification = Notification.TextNotification(title, riftMessage, colonyItem.colony.characterId, planetType)
        triggerAlert(alert, notification, title, systemMessage)
    }

    private fun triggerAlert(alert: Alert, notification: Notification?, title: String, message: String, iconUrl: String? = null) {
        alert.actions.forEach { action ->
            when (action) {
                AlertAction.RiftNotification -> if (notification != null) sendRiftNotification(notification)
                AlertAction.SystemNotification -> sendSystemNotification(title, message)
                AlertAction.PushNotification -> sendPushNotification(title, message, iconUrl)
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
        return matchingEntities.let {
            if (it.any { it.first is IntelReportType.LabeledContacts }) {
                val (labeledContacts, entities) = it.first { it.first is IntelReportType.LabeledContacts }
                val expectedLabels = (labeledContacts as IntelReportType.LabeledContacts).labels
                val matchedLabels = entities.filterIsInstance<SystemEntity.Character>().map { character ->
                    val ids = listOfNotNull(character.characterId, character.details.corporationId, character.details.allianceId)
                    contactsRepository.getLabels(ids).filter { label ->
                        expectedLabels.any { expectedLabel -> label.owner.id == expectedLabel.ownerId && label.id == expectedLabel.id }
                    }
                }.flatten().map { it.name }.distinct()
                val labelsText = matchedLabels.joinToString(", ")
                "Hostile labeled \"$labelsText\" reported"
            } else if (it.any { it.first is IntelReportType.SpecificCharacters }) {
                "Specific hostile reported"
            } else if (it.any { it.first is IntelReportType.SpecificShipClasses }) {
                "Specific ship class reported"
            } else if (it.any { it.first is IntelReportType.AnyCharacter }) {
                "Hostile reported"
            } else if (it.any { it.first is IntelReportType.GateCamp }) {
                "Gate camp reported"
            } else if (it.any { it.first is IntelReportType.AnyShip }) {
                "Hostile ship reported"
            } else if (it.any { it.first is IntelReportType.Bubbles }) {
                "Bubbles reported"
            } else if (it.any { it.first is IntelReportType.Wormhole }) {
                "Wormhole reported"
            } else {
                "Intel alert"
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
                    0 -> "In system"
                    1 -> "1 jump away from"
                    else -> "$distance jumps away from"
                }
                "$distanceText ${locationMatch.system.name}"
            }

            is AlertLocationMatch.Character -> {
                when (val distance = locationMatch.distance) {
                    0 -> "In your system"
                    1 -> "1 jump away"
                    else -> "$distance jumps away"
                }
            }
        }
        return message
    }

    private fun getNotificationTitle(action: GameLogAction): String {
        return when (action) {
            is GameLogAction.UnderAttack -> "Under attack"
            is GameLogAction.Attacking -> "Attacking"
            is GameLogAction.BeingWarpScrambled -> "Warp scrambled"
            is GameLogAction.Decloaked -> "Decloaked"
            is GameLogAction.CombatStopped -> "Combat stopped"
            GameLogAction.CloneJumping -> throw IllegalStateException("Not used")
            is GameLogAction.RanOutOfCharges -> "Module out of charges"
            is GameLogAction.Generic -> action.type.replaceFirstChar { it.titlecase(Locale.US) }
        }
    }

    private fun getNotificationMessage(action: GameLogAction): AnnotatedString {
        val styleTag = Notification.TextNotification.STYLE_TAG
        val styleValue = Notification.TextNotification.STYLE_VALUE
        return when (action) {
            is GameLogAction.UnderAttack -> buildAnnotatedString {
                append("Attacker is ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            is GameLogAction.Attacking -> buildAnnotatedString {
                append("Target is ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            is GameLogAction.BeingWarpScrambled -> buildAnnotatedString {
                append("Tackled by ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            is GameLogAction.Decloaked -> buildAnnotatedString {
                withAnnotation(styleTag, styleValue) {
                    append(action.by)
                }
                append(" is too close")
            }
            is GameLogAction.CombatStopped -> buildAnnotatedString {
                append("Last target was ")
                withAnnotation(styleTag, styleValue) {
                    append(action.target)
                }
            }
            GameLogAction.CloneJumping -> throw IllegalStateException("Not used")
            is GameLogAction.RanOutOfCharges -> buildAnnotatedString {
                withAnnotation(styleTag, styleValue) {
                    append(action.module)
                }
                append(" needs to reload")
            }
            is GameLogAction.Generic -> buildAnnotatedString {
                append(action.message)
            }
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
            is GameLogAction.RanOutOfCharges -> typesRepository.getType(action.module)
            is GameLogAction.Generic -> typesRepository.findTypeInText(action.message)
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

    private fun sendPushNotification(title: String, message: String, iconUrl: String?) {
        scope.launch {
            pushNotificationController.sendPushNotification(title, message, iconUrl)
        }
    }
}
