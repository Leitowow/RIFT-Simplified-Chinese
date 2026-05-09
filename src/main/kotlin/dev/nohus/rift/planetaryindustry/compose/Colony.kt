package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftSlider
import dev.nohus.rift.compose.RiftSolarSystemChip
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.clock_16
import dev.nohus.rift.generated.resources.fastforward
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.SeekingColony
import dev.nohus.rift.planetaryindustry.models.Colony
import dev.nohus.rift.planetaryindustry.models.ColonyStatus
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Extracting
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Idle
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NeedsAttention
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NotSetup
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Producing
import dev.nohus.rift.planetaryindustry.models.Pin
import dev.nohus.rift.planetaryindustry.models.PinStatus.ExtractorExpired
import dev.nohus.rift.planetaryindustry.models.PinStatus.ExtractorInactive
import dev.nohus.rift.planetaryindustry.models.PinStatus.StorageFull
import dev.nohus.rift.planetaryindustry.models.getName
import dev.nohus.rift.utils.formatDateTime
import dev.nohus.rift.utils.formatDurationCompact
import dev.nohus.rift.utils.invertedPlural
import dev.nohus.rift.utils.plural
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant

/**
 * Colony title row
 */
@Composable
fun ColonyTitle(
    item: ColonyItem,
    isExpanded: Boolean,
    isViewingFastForward: Boolean,
    onViewFastForwardChange: (Boolean) -> Unit,
    onSetSeekingColony: (SeekingColony?) -> Unit,
    scrollState: ScrollState? = null,
    colonyIconModifier: Modifier = Modifier,
    onDetailsClick: () -> Unit,
) {
    val colony = item.colony
    val borderColor = RiftTheme.colors.borderPrimaryDark
    val borderAlpha by animateFloatAsState(if (scrollState?.canScrollBackward == true) 1f else 0f)
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color.Transparent, RiftTheme.colors.backgroundPrimaryDark))),
        ) {
            ColonyIcon(colony, rememberInfiniteTransition(), colonyIconModifier)
            Column(
                modifier = Modifier.padding(vertical = Spacing.small),
            ) {
                ColonyOwner(colony, item.characterName)
                RiftSolarSystemChip(item.location)
            }
            Spacer(Modifier.weight(1f))
            ExpiresIn(item, isViewingFastForward, onViewFastForwardChange)
            RiftButton(
                text = if (isExpanded) "Return" else "Details",
                onClick = onDetailsClick,
            )
        }
        AnimatedVisibility(isViewingFastForward) {
            ViewingFastForward(item, onSetSeekingColony, onReturnClick = { onViewFastForwardChange(false) })
        }
        AnimatedVisibility(scrollState != null) {
            Box(
                Modifier
                    .modifyIf(isViewingFastForward) { padding(top = Spacing.medium) }
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(borderColor.copy(alpha = borderAlpha)),
            )
        }
    }
}

@Composable
private fun ViewingFastForward(
    item: ColonyItem,
    onSetSeekingColony: (SeekingColony?) -> Unit,
    onReturnClick: () -> Unit,
) {
    val maxSeekHours = Duration.between(item.colony.currentSimTime, item.ffwdColony.currentSimTime).toHours().toInt() + 1
    var seekHours by remember { mutableStateOf(maxSeekHours) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.mediumLarge),
        modifier = Modifier
            .padding(top = Spacing.small)
            .background(RiftTheme.colors.windowBackgroundSecondary)
            .padding(vertical = Spacing.medium),
    ) {
        RiftButton(
            text = "Now",
            icon = Res.drawable.clock_16,
            type = ButtonType.Primary,
            cornerCut = ButtonCornerCut.BottomLeft,
            isCompact = false,
            onClick = onReturnClick,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "Time fast-forward",
                style = RiftTheme.typography.headerPrimary,
            )

            LaunchedEffect(seekHours) {
                val seekTimestamp = item.colony.currentSimTime + Duration.ofHours(seekHours.toLong())
                if (seekTimestamp >= item.ffwdColony.currentSimTime) {
                    // Seeked to the expiry time, stop seeking, which will show the expired colony
                    onSetSeekingColony(null)
                } else {
                    onSetSeekingColony(SeekingColony(item.colony.id, seekTimestamp))
                }
            }
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
            ) {
                RiftSlider(
                    width = maxWidth,
                    range = 0..maxSeekHours,
                    currentValue = seekHours,
                    onValueChange = { seekHours = it },
                    getValueName = { null },
                    isPreciseScroll = true,
                    isImmediate = true,
                )
            }
            DisposableEffect(Unit) {
                onDispose {
                    onSetSeekingColony(null)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.mediumLarge),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = Spacing.medium),
            ) {
                val colony = item.seekColony ?: item.ffwdColony
                Text(
                    text = formatDateTime(colony.currentSimTime),
                    style = RiftTheme.typography.headerPrimary,
                )
                getFutureColonyStatusDescription(colony.status)?.let {
                    Text(
                        text = it,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            }
        }

        RiftButton(
            text = "Expires",
            icon = Res.drawable.fastforward,
            type = ButtonType.Primary,
            cornerCut = ButtonCornerCut.BottomRight,
            isCompact = false,
            isEnabled = seekHours < maxSeekHours,
            onClick = { seekHours = maxSeekHours },
        )
    }
}

private fun getFutureColonyStatusDescription(status: ColonyStatus): String? {
    val state = when (status) {
        is NeedsAttention -> {
            val expired = status.pins.count { it.status == ExtractorExpired }
            val inactive = status.pins.count { it.status == ExtractorInactive }
            val full = status.pins.filter { it.status == StorageFull }
            buildList {
                if (expired > 0) add("Extractor${expired.plural} expire${expired.invertedPlural}")
                if (inactive > 0) add("Extractor${inactive.plural} become${inactive.invertedPlural} inactive")
                full.forEach { add("${it.getName()} becomes full") }
            }.joinToString()
        }
        is Idle -> "All production stops"
        is NotSetup -> "Not setup"
        is Producing -> "Producing"
        is Extracting -> "Extracting"
    }
    return state
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpiresIn(
    item: ColonyItem,
    isViewingFastForward: Boolean,
    onViewFastForwardChange: (Boolean) -> Unit,
) {
    val expiresIn = Duration.between(getNow(), item.ffwdColony.currentSimTime)
    if (expiresIn.toSeconds() > 0) {
        RiftTooltipArea(
            tooltip = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(Spacing.large),
                ) {
                    Image(
                        painter = painterResource(if (isViewingFastForward) Res.drawable.clock_16 else Res.drawable.fastforward),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (isViewingFastForward) "Return to present" else "Fast-forward",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
            },
        ) {
            val pointerInteraction = rememberPointerInteractionStateHolder()
            val borderAlpha by animateFloatAsState(if (pointerInteraction.isHovered) 1f else 0f)
            Box(
                modifier = Modifier
                    .pointerInteraction(pointerInteraction)
                    .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                    .background(RiftTheme.colors.backgroundHovered.copy(alpha = borderAlpha))
                    .border(1.dp, RiftTheme.colors.borderGreyLight.copy(alpha = borderAlpha))
                    .padding(Spacing.small)
                    .onClick { onViewFastForwardChange(!isViewingFastForward) },
            ) {
                TitledText(
                    title = "Expires in",
                    text = formatDurationCompact(expiresIn),
                )
            }
        }
    }
}

/**
 * List of pin spheres
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColonyOverview(
    colony: Colony,
    now: Instant,
    isAdvancingTime: Boolean,
    onRequestSimulation: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.medium),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            colony.pins.sort().forEach { pin ->
                key(pin.id) {
                    PinSphere(colony, pin, 64.dp, now, isAdvancingTime, onRequestSimulation)
                }
            }
        }
    }
}

/**
 * List of pins with details
 */
@Composable
fun ColonyPins(
    colony: Colony,
    now: Instant,
    isAdvancingTime: Boolean,
    onRequestSimulation: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        colony.pins.sort().forEach { pin ->
            Pin(pin, colony, now, isAdvancingTime, onRequestSimulation)
        }
    }
}

@Composable
private fun ColonyOwner(
    colony: Colony,
    characterName: String?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = 200.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f)),
        ) {
            DynamicCharacterPortraitParallax(
                characterId = colony.characterId,
                size = 32.dp,
                enterTimestamp = null,
                pointerInteractionStateHolder = null,
            )
        }
        Text(
            text = characterName ?: "Loading…",
            style = RiftTheme.typography.headerPrimary,
        )
    }
}

private fun List<Pin>.sort(): List<Pin> {
    val order = listOf(
        Pin.CommandCenter::class,
        Pin.Launchpad::class,
        Pin.Storage::class,
        Pin.Extractor::class,
        Pin.Factory::class,
    )
    return sortedWith(
        compareBy(
            {
                order.indexOf(it::class)
            },
            {
                (it as? Pin.Factory)?.schematic?.outputType?.id ?: 0
            },
            {
                it.designator
            },
        ),
    )
}
