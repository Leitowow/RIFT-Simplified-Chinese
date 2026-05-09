package dev.nohus.rift.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitStandings
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.keywords_clear
import dev.nohus.rift.generated.resources.keywords_combat_probe
import dev.nohus.rift.generated.resources.keywords_ess
import dev.nohus.rift.generated.resources.keywords_gatecamp
import dev.nohus.rift.generated.resources.keywords_interdiction_probe
import dev.nohus.rift.generated.resources.keywords_killreport
import dev.nohus.rift.generated.resources.keywords_no_visual
import dev.nohus.rift.generated.resources.keywords_skyhook
import dev.nohus.rift.generated.resources.keywords_spike
import dev.nohus.rift.generated.resources.keywords_systems
import dev.nohus.rift.generated.resources.keywords_wormhole
import dev.nohus.rift.intel.ParsedChannelChatMessage
import dev.nohus.rift.intel.reports.IntelReportsSettings
import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType
import dev.nohus.rift.logs.parse.ChatMessageParser.Token
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Link
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.StarGatesRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.getColor
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ChatMessage(
    settings: IntelReportsSettings,
    message: ParsedChannelChatMessage,
    alertTriggerTimestamp: Instant?,
    enterAnimation: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
) {
    val time = ZonedDateTime.ofInstant(message.chatMessage.timestamp, settings.displayTimezone).toLocalTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val formattedTime = formatter.format(time)
    val pointerState = remember { PointerInteractionStateHolder() }
    val background by pointerState.animateBackgroundHover()

    var animationProgress by remember { mutableStateOf(0f) }
    val animationCurve = (if (animationProgress < 0.5f) animationProgress else 1f - animationProgress) * 2
    if (alertTriggerTimestamp != null) {
        LaunchedEffect(alertTriggerTimestamp) {
            val animationDuration = 1_500
            val timeElapsed = Duration.between(alertTriggerTimestamp, Instant.now()).toMillis()
            animate(
                initialValue = 0f + (timeElapsed / animationDuration),
                targetValue = 1f,
                animationSpec = tween(animationDuration, easing = FastOutSlowInEasing),
            ) { value, _ -> animationProgress = value }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .pointerInteraction(pointerState)
            .background(background)
            .drawWithContent {
                drawContent()
                if (animationProgress > 0f) {
                    val width = 50
                    val x = -width + (width + size.width) * animationProgress
                    val color = Color.White.copy(alpha = animationCurve.coerceIn(0f..1f))
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, color, Color.Transparent),
                            start = Offset(x, size.height * (1f - animationProgress)),
                            end = Offset(x + width, size.height * animationProgress),
                        ),
                    )
                }
            }
            .fillMaxWidth(),
    ) {
        Message(
            settings = settings,
            metadata = { MessageMetadata(settings, message, formattedTime, pointerState) },
            tokens = message.parsed,
            enterAnimation = enterAnimation,
        )
    }
}

@Composable
private fun MessageMetadata(
    settings: IntelReportsSettings,
    message: ParsedChannelChatMessage,
    formattedTime: String,
    pointerState: PointerInteractionStateHolder,
) {
    RiftTooltipArea(
        text = "Original message:\n${message.chatMessage.message}",
    ) {
        val background by pointerState.animateWindowBackgroundSecondaryHover()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .heightIn(min = settings.rowHeight)
                .border(1.dp, RiftTheme.colors.borderGrey)
                .background(background),
        ) {
            Text(
                text = formattedTime,
                modifier = Modifier.padding(4.dp),
            )
            if (settings.isShowingChannel) {
                VerticalDivider(color = RiftTheme.colors.borderGrey)
                Text(
                    text = message.metadata.channelName,
                    style = RiftTheme.typography.bodyHighlighted,
                    modifier = Modifier.padding(4.dp),
                )
            }
            if (settings.isShowingRegion) {
                VerticalDivider(color = RiftTheme.colors.borderGrey)
                Text(
                    text = message.channelRegions.joinToString("/"),
                    style = RiftTheme.typography.bodyHighlighted,
                    modifier = Modifier.padding(4.dp),
                )
            }
            if (settings.isShowingReporter) {
                VerticalDivider(color = RiftTheme.colors.borderGrey)
                Text(
                    text = message.chatMessage.author,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Message(
    settings: IntelReportsSettings,
    metadata: @Composable () -> Unit,
    tokens: List<Token>,
    enterAnimation: Animatable<Float, AnimationVector1D>,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        metadata()
        for (token in tokens) {
            val type = token.type
            val text = token.words.joinToString(" ")
            if (type != null) {
                when (type) {
                    is TokenType.Count -> TokenWithCount(settings.rowHeight, text)
                    is TokenType.Keyword -> TokenWithKeyword(settings.rowHeight, type.type)
                    is TokenType.Kill -> TokenWithKill(settings.rowHeight, type)
                    Link -> TokenWithText(settings.rowHeight, text, "Link")
                    is TokenType.Character -> TokenWithCharacter(settings.rowHeight, text, type)
                    is TokenType.Question -> TokenWithText(settings.rowHeight, text, "Question")
                    is TokenType.Ship -> TokenWithShip(settings.rowHeight, type)
                    is TokenType.System -> TokenWithSystem(settings.rowHeight, settings.isShowingSystemDistance, settings.isUsingJumpBridgesForDistance, type.system, enterAnimation)
                    TokenType.Url -> TokenWithUrl(settings.rowHeight, text)
                    is TokenType.Gate -> {
                        val fromSystem = tokens.firstNotNullOfOrNull { (it.type as? TokenType.System)?.system }
                        TokenWithGate(settings.rowHeight, fromSystem, type.system, type.isAnsiblex, enterAnimation)
                    }
                    is TokenType.Movement -> TokenWithMovement(settings.rowHeight, tokens, type)
                }
            } else {
                if (text.isNotBlank()) {
                    TokenWithPlainText(settings.rowHeight, text.trim())
                }
            }
        }
    }
}

@Composable
private fun TokenWithText(rowHeight: Dp, text: String, type: String) {
    BorderedToken(rowHeight) {
        Text(
            text = type,
            modifier = Modifier.padding(4.dp),
        )
        VerticalDivider(color = RiftTheme.colors.borderGreyLight)
        Text(
            text = text,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun TokenWithCount(rowHeight: Dp, text: String) {
    BorderedToken(rowHeight) {
        Text(
            text = text,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun TokenWithPlainText(rowHeight: Dp, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = rowHeight),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TokenWithUrl(rowHeight: Dp, text: String) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max).heightIn(min = rowHeight)
            .pointerHoverIcon(PointerIcon(Cursors.hand))
            .pointerInteraction(pointerInteractionStateHolder)
            .onClick { text.toURIOrNull()?.openBrowser() },
    ) {
        val underline = if (pointerInteractionStateHolder.current != PointerInteractionState.Normal) TextDecoration.Underline else TextDecoration.None
        Text(
            text = text,
            textDecoration = underline,
            style = RiftTheme.typography.bodyLink,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun TokenWithShip(rowHeight: Dp, ship: TokenType.Ship) {
    ClickableShip(ship.type) {
        BorderedToken(rowHeight) {
            AsyncTypeIcon(
                typeId = ship.type.id,
                modifier = Modifier.size(rowHeight),
            )
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
            val text = if (ship.count > 1) {
                "${ship.count}x ${ship.type.name}"
            } else if (ship.isPlural) {
                "${ship.type.name}s"
            } else {
                ship.type.name
            }
            Text(
                text = text,
                style = RiftTheme.typography.bodyHighlighted,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@Composable
private fun TokenWithSystem(
    rowHeight: Dp,
    isShowingSystemDistance: Boolean,
    isUsingJumpBridges: Boolean,
    system: MapSolarSystem,
    enterAnimation: Animatable<Float, AnimationVector1D>,
) {
    BorderedToken(rowHeight) {
        SystemDetails(
            system = system,
            rowHeight = rowHeight,
            isShowingSystemDistance = isShowingSystemDistance,
            isUsingJumpBridges = isUsingJumpBridges,
            enterAnimation = enterAnimation,
        )
    }
}

@Composable
private fun TokenWithGate(
    rowHeight: Dp,
    fromSystem: MapSolarSystem?,
    toSystem: MapSolarSystem,
    isAnsiblex: Boolean,
    enterAnimation: Animatable<Float, AnimationVector1D>,
) {
    val starGatesRepository: StarGatesRepository = remember { koin.get() }
    val gate = starGatesRepository.getGate(isAnsiblex, fromSystem?.id, toSystem.id)
    val gateText = if (isAnsiblex) "Ansiblex" else "Gate"
    val name = "${toSystem.name} $gateText"
    ClickableLocation(
        systemId = fromSystem?.id,
        locationId = gate.locationId,
        locationTypeId = gate.typeId,
        locationName = name,
    ) {
        BorderedToken(rowHeight) {
            AsyncTypeIcon(
                typeId = gate.typeId,
                modifier = Modifier.size(rowHeight),
            )
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
            SystemIllustrationIconSmall(
                solarSystemId = toSystem.id,
                size = rowHeight,
                animation = enterAnimation,
                modifier = Modifier.clipToBounds(),
            )
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
            Text(
                text = name,
                style = RiftTheme.typography.bodyLink,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@Composable
private fun TokenWithMovement(rowHeight: Dp, previousTokens: List<Token>, movement: TokenType.Movement) {
    BorderedToken(rowHeight) {
        Image(
            painter = painterResource(Res.drawable.keywords_systems),
            contentDescription = null,
            modifier = Modifier.size(rowHeight),
        )
        if (movement.isGate) {
            VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
            val systemFrom = previousTokens.firstNotNullOfOrNull { (it.type as? TokenType.System)?.system }
            val starGatesRepository: StarGatesRepository = remember { koin.get() }
            AsyncTypeIcon(
                typeId = starGatesRepository.getGate(false, systemFrom?.id, movement.toSystem.id).typeId,
                modifier = Modifier.size(rowHeight),
            )
        }
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
        SystemIllustrationIconSmall(
            solarSystemId = movement.toSystem.id,
            size = rowHeight,
        )
        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
        Text(
            text = movement.verb,
            modifier = Modifier.padding(vertical = 4.dp).padding(start = 4.dp),
        )
        Text(
            text = movement.toSystem.name,
            style = RiftTheme.typography.bodyLink,
            modifier = Modifier.padding(4.dp),
        )
        if (movement.isGate) {
            Text(
                text = "gate",
                modifier = Modifier.padding(vertical = 4.dp).padding(end = 4.dp),
            )
        }
    }
}

@Composable
private fun TokenWithKeyword(rowHeight: Dp, type: KeywordType) {
    BorderedToken(rowHeight) {
        when (type) {
            KeywordType.NoVisual -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_no_visual),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "No visual",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Clear -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_clear),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Clear",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Wormhole -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_wormhole),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Wormhole",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Spike -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_spike),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Spike",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Ess -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_ess),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "ESS",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Skyhook -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_skyhook),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Skyhook",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.GateCamp -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_gatecamp),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Gate Camp",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.CombatProbes -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_combat_probe),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Combat Probes",
                    modifier = Modifier.padding(4.dp),
                )
            }
            KeywordType.Bubbles -> {
                Image(
                    painter = painterResource(Res.drawable.keywords_interdiction_probe),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                Text(
                    text = "Bubbles",
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun TokenWithCharacter(rowHeight: Dp, name: String, character: TokenType.Character) {
    BorderedToken(rowHeight) {
        ClickableCharacter(character.characterId) {
            DynamicCharacterPortraitStandings(
                characterId = character.characterId,
                size = rowHeight,
                standingLevel = character.details?.standingLevel ?: Standing.Neutral,
                isAnimated = true,
            )
        }
        if (character.details != null) {
            ClickableCorporation(character.details.corporationId) {
                RiftTooltipArea(
                    text = character.details.corporationName ?: "",
                ) {
                    AsyncCorporationLogo(
                        corporationId = character.details.corporationId,
                        size = 32,
                        modifier = Modifier.size(rowHeight),
                    )
                }
            }
            if (character.details.allianceId != null) {
                ClickableAlliance(character.details.allianceId) {
                    RiftTooltipArea(
                        text = character.details.allianceName ?: "",
                    ) {
                        AsyncAllianceLogo(
                            allianceId = character.details.allianceId,
                            size = 32,
                            modifier = Modifier.size(rowHeight),
                        )
                    }
                }
            }
        }

        VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
        ClickableCharacter(character.characterId) {
            val ticker = buildString {
                character.details?.corporationTicker?.let { append("$it ") }
                character.details?.allianceTicker?.let { append(it) }
            }
            var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
            character.details?.standingLevel?.getColor()?.let { nameStyle = nameStyle.copy(color = it) }
            if (rowHeight < 32.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.padding(horizontal = Spacing.small),
                ) {
                    Text(
                        text = ticker,
                        style = RiftTheme.typography.bodySecondary,
                    )
                    Text(
                        text = name,
                        style = nameStyle,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.small),
                ) {
                    Text(
                        text = name,
                        style = nameStyle,
                    )
                    Text(
                        text = ticker,
                        style = RiftTheme.typography.detailSecondary,
                    )
                }
            }
        }

        if (character.details != null) {
            ContactLabelTag(
                details = character.details,
                modifier = Modifier.padding(end = Spacing.small),
            )
        }
    }
}

@Composable
private fun TokenWithKill(rowHeight: Dp, token: TokenType.Kill) {
    val repository: TypesRepository by koin.inject()
    RiftTooltipArea(
        text = "${token.name}\n${token.target}",
    ) {
        ClickableCharacter(token.characterId) {
            BorderedToken(rowHeight) {
                Image(
                    painter = painterResource(Res.drawable.keywords_killreport),
                    contentDescription = null,
                    modifier = Modifier.size(rowHeight),
                )
                VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
                if (token.characterId != null) {
                    DynamicCharacterPortraitStandings(
                        characterId = token.characterId,
                        size = rowHeight,
                        standingLevel = token.details?.standingLevel ?: Standing.Neutral,
                        isAnimated = true,
                    )
                    VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
                }
                val type = repository.getType(token.target)
                AsyncTypeIcon(
                    type = type,
                    modifier = Modifier.size(rowHeight),
                )
                VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
                Text(
                    text = token.name,
                    style = RiftTheme.typography.bodyHighlighted,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}
