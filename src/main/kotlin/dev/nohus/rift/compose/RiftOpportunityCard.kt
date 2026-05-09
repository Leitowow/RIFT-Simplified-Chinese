package dev.nohus.rift.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nohus.rift.compose.RiftOpportunityCardTopRight.RiftOpportunityCardCharacter
import dev.nohus.rift.compose.RiftOpportunityCardTopRight.RiftOpportunityCardCorporation
import dev.nohus.rift.compose.RiftOpportunityCardTopRight.RiftOpportunityCardProgressGauge
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.careerpaths_enforcer_16px
import dev.nohus.rift.generated.resources.careerpaths_enforcer_flair
import dev.nohus.rift.generated.resources.careerpaths_explorer_16px
import dev.nohus.rift.generated.resources.careerpaths_explorer_flair
import dev.nohus.rift.generated.resources.careerpaths_industrialist_16px
import dev.nohus.rift.generated.resources.careerpaths_industrialist_flair
import dev.nohus.rift.generated.resources.careerpaths_sof_flair
import dev.nohus.rift.generated.resources.careerpaths_soldier_of_fortune_16px
import dev.nohus.rift.generated.resources.careerpaths_unclassified_16px
import dev.nohus.rift.generated.resources.careerpaths_unclassified_flair
import dev.nohus.rift.network.esi.models.OpportunityState
import dev.nohus.rift.repositories.SolarSystemChipState
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import java.time.Instant
import java.time.ZoneId

sealed class RiftOpportunityCardCategory(
    val icon: DrawableResource,
    val name: String,
    val flair: DrawableResource,
) {
    data object Enforcer : RiftOpportunityCardCategory(
        icon = Res.drawable.careerpaths_enforcer_16px,
        name = "Enforcer",
        flair = Res.drawable.careerpaths_enforcer_flair,
    )
    data object SoldierOfFortune : RiftOpportunityCardCategory(
        icon = Res.drawable.careerpaths_soldier_of_fortune_16px,
        name = "Soldier of Fortune",
        flair = Res.drawable.careerpaths_sof_flair,
    )
    data object Industrialist : RiftOpportunityCardCategory(
        icon = Res.drawable.careerpaths_industrialist_16px,
        name = "Industrialist",
        flair = Res.drawable.careerpaths_industrialist_flair,
    )
    data object Explorer : RiftOpportunityCardCategory(
        icon = Res.drawable.careerpaths_explorer_16px,
        name = "Explorer",
        flair = Res.drawable.careerpaths_explorer_flair,
    )
    data object Unclassified : RiftOpportunityCardCategory(
        icon = Res.drawable.careerpaths_unclassified_16px,
        name = "Unclassified",
        flair = Res.drawable.careerpaths_unclassified_flair,
    )
}

sealed interface RiftOpportunityCardTopRight {
    data class RiftOpportunityCardCharacter(
        val name: String,
        val id: Int?,
    ) : RiftOpportunityCardTopRight

    data class RiftOpportunityCardCorporation(
        val name: String,
        val id: Int,
        val progressGauge: RiftOpportunityCardProgressGauge,
    ) : RiftOpportunityCardTopRight

    data class RiftOpportunityCardProgressGauge(
        val currentProgress: Long,
        val desiredProgress: Long,
        val ownProgress: Long,
        val participationLimit: Long?,
        val state: OpportunityState,
    ) : RiftOpportunityCardTopRight
}

data class RiftOpportunityCardButton(
    val resource: DrawableResource,
    val colorTint: Color? = null,
    val isAlwaysVisible: Boolean = true,
    val tooltipContent: @Composable (() -> Unit)? = null,
    val action: (() -> Unit)?,
) {
    constructor(
        resource: DrawableResource,
        colorTint: Color? = null,
        isAlwaysVisible: Boolean = true,
        tooltip: String,
        action: (() -> Unit)?,
    ) : this(
        resource = resource,
        colorTint = colorTint,
        isAlwaysVisible = isAlwaysVisible,
        tooltipContent = {
            Text(
                text = tooltip,
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier.padding(Spacing.large),
            )
        },
        action = action,
    )
}

sealed class RiftOpportunityCardBottomContent {
    data class Timestamp(val text: String?, val timestamp: Instant, val displayTimezone: ZoneId) : RiftOpportunityCardBottomContent()
    data class Text(val icon: DrawableResource? = null, val text: AnnotatedString, val tooltip: AnnotatedString? = null) : RiftOpportunityCardBottomContent()
    data object None : RiftOpportunityCardBottomContent()
}

data class RiftOpportunityCardType(
    val text: AnnotatedString,
    val icon: DrawableResource? = null,
    val tooltip: String? = null,
) {
    constructor(
        text: String,
        icon: DrawableResource? = null,
        tooltip: String? = null,
    ) : this(AnnotatedString(text), icon, tooltip)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun RiftOpportunityCard(
    category: RiftOpportunityCardCategory,
    type: RiftOpportunityCardType,
    solarSystemChipState: SolarSystemChipState?,
    topRight: RiftOpportunityCardTopRight?,
    bottomContent: RiftOpportunityCardBottomContent,
    buttons: List<RiftOpportunityCardButton>,
    modifier: Modifier = Modifier,
    isEnforcingHeight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    Row(
        modifier = modifier
            .pointerInteraction(pointerInteractionStateHolder)
            .modifyIfNotNull(onClick) {
                onClick { it() }
                    .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            }
            .modifyIf(isEnforcingHeight) {
                height(176.dp)
            }
            .height(IntrinsicSize.Min),
    ) {
        LeftGlowLine(pointerInteractionStateHolder)
        val background by animateColorAsState(
            if (pointerInteractionStateHolder.isHovered) {
                RiftTheme.colors.backgroundPrimary
            } else {
                RiftTheme.colors.backgroundPrimaryDark
            },
        )
        Surface(
            color = background,
            shape = CutCornerShape(bottomEnd = 15.dp),
        ) {
            Box {
                RiftOpportunityContainerFlair(
                    isHovered = pointerInteractionStateHolder.isHovered,
                    image = category.flair,
                    offset = DpOffset(24.dp, 20.dp),
                )

                val corporationStandardWidth = 48.dp + (Spacing.large * 2)
                CorporationColorsSwatch(topRight, corporationStandardWidth, pointerInteractionStateHolder)

                Column {
                    Column(
                        modifier = Modifier
                            .modifyIf(isEnforcingHeight) {
                                weight(1f)
                            }
                            .padding(horizontal = Spacing.large, vertical = Spacing.mediumLarge),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clipToBounds()
                                    .fadingRightEdge()
                                    .wrapContentWidth(align = Alignment.Start, unbounded = true),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(bottom = Spacing.medium),
                                ) {
                                    RiftOpportunityTypeIcon(category.icon, category.name)
                                    if (type.icon != null) {
                                        Spacer(Modifier.width(Spacing.small))
                                        RiftOpportunityTypeIcon(type.icon, type.tooltip)
                                    }
                                    Spacer(Modifier.width(Spacing.medium))
                                    Text(
                                        text = type.text,
                                        style = RiftTheme.typography.headerSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible,
                                        softWrap = false,
                                    )
                                }
                                if (solarSystemChipState != null) {
                                    RiftSolarSystemChip(solarSystemChipState)
                                }
                            }
                            when (topRight) {
                                is RiftOpportunityCardCharacter -> RiftCircularCharacterPortrait(
                                    characterId = topRight.id,
                                    name = topRight.name,
                                    hasPadding = true,
                                    size = 48.dp,
                                )
                                is RiftOpportunityCardCorporation -> {
                                    RiftOpportunityCardSmallProgressGauge(topRight.progressGauge)
                                    Spacer(Modifier.width(Spacing.large * 2))
                                    ClickableCorporation(topRight.id) {
                                        RiftTooltipArea(
                                            text = topRight.name,
                                        ) {
                                            AsyncCorporationLogo(
                                                corporationId = topRight.id,
                                                size = 64,
                                                modifier = Modifier.size(48.dp),
                                            )
                                        }
                                    }
                                }
                                is RiftOpportunityCardProgressGauge -> RiftOpportunityCardProgressGauge(topRight)
                                null -> {}
                            }
                        }
                        content()
                    }
                    Box(
                        modifier = Modifier
                            .height(IntrinsicSize.Min),
                    ) {
                        Box(
                            Modifier
                                .modifyIf(topRight is RiftOpportunityCardCorporation) {
                                    padding(end = corporationStandardWidth)
                                }
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(Color.White.copy(alpha = 0.05f)),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = Spacing.large, end = Spacing.medium)
                                    .padding(vertical = Spacing.mediumLarge),
                            ) {
                                when (bottomContent) {
                                    is RiftOpportunityCardBottomContent.Timestamp -> {
                                        val now = getNow()
                                        val age = key(now) { getRelativeTime(bottomContent.timestamp, bottomContent.displayTimezone) }
                                        val text = buildAnnotatedString {
                                            if (bottomContent.text != null) {
                                                withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                                                    append(bottomContent.text)
                                                }
                                                append(", ")
                                            }
                                            append(age)
                                        }
                                        Text(
                                            text = text,
                                            style = RiftTheme.typography.headerSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    is RiftOpportunityCardBottomContent.Text -> {
                                        RiftTooltipArea(
                                            text = bottomContent.tooltip,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                if (bottomContent.icon != null) {
                                                    Icon(
                                                        painter = painterResource(bottomContent.icon),
                                                        contentDescription = null,
                                                        tint = RiftTheme.colors.textSecondary,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                    Spacer(Modifier.width(Spacing.medium))
                                                }
                                                Text(
                                                    text = bottomContent.text,
                                                    style = RiftTheme.typography.headerPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                    RiftOpportunityCardBottomContent.None -> {
                                        Text(
                                            text = "",
                                            style = RiftTheme.typography.headerSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }

                            if (buttons.isNotEmpty()) {
                                Box(
                                    modifier = Modifier,
                                ) {
                                    if (topRight is RiftOpportunityCardCorporation) {
                                        val width = 24.dp * buttons.count { it.isAlwaysVisible || pointerInteractionStateHolder.isHovered }
                                        val animatedWidth by animateDpAsState(width)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .clip(RoundedCornerShape(100))
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .modifyIf(width.value > 0) {
                                                    padding(start = 8.dp, end = 4.dp)
                                                }
                                                .height(24.dp)
                                                .width(animatedWidth),
                                        ) {}
                                    }
                                    Row(
                                        modifier = Modifier
                                            .padding(start = 8.dp, end = 4.dp),
                                    ) {
                                        buttons.forEach { button ->
                                            AnimatedVisibility(
                                                visible = button.isAlwaysVisible || pointerInteractionStateHolder.isHovered,
                                                enter = fadeIn(),
                                                exit = fadeOut(),
                                            ) {
                                                RiftTooltipArea(
                                                    tooltip = button.tooltipContent,
                                                ) {
                                                    RiftImageButton(
                                                        resource = button.resource,
                                                        size = 16.dp,
                                                        iconPadding = 8.dp,
                                                        onClick = button.action,
                                                        tint = button.colorTint ?: if (button.action == null) RiftTheme.colors.textSecondary else null,
                                                        isFullAlpha = button.colorTint != null,
                                                        highlightModifier = 0.5f,
                                                        modifier = Modifier.size(24.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CorporationColorsSwatch(
    topRight: RiftOpportunityCardTopRight?,
    corporationStandardWidth: Dp,
    pointerInteractionStateHolder: PointerInteractionStateHolder,
) {
    if (topRight is RiftOpportunityCardCorporation) {
        val colors by produceCorporationColors(topRight.id)
        val primaryColor by animateColorAsState(colors.primary)
        val secondaryColor by animateColorAsState(colors.secondary)
        val isActive = pointerInteractionStateHolder.isHovered
        val alpha by animateFloatAsState(if (isActive) 0.5f else 0.1f)
        val blur by animateFloatAsState(if (isActive) 4f else 0.5f)
        val extent by animateFloatAsState(if (isActive) 4f else 2f)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(corporationStandardWidth)
                .fillMaxHeight()
                .background(primaryColor),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                    .clip(CutCornerShape(topStartPercent = 100))
                    .size((48 + extent).dp)
                    .background(secondaryColor.copy(alpha = alpha)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CutCornerShape(topStartPercent = 100))
                    .size(48.dp)
                    .background(secondaryColor),
            )
        }
    }
}

@Composable
private fun LeftGlowLine(pointerInteractionStateHolder: PointerInteractionStateHolder) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxHeight()
            .width(7.dp)
            .offset(4.dp)
            .zIndex(1f),
    ) {
        val alpha by animateFloatAsState(if (pointerInteractionStateHolder.isHovered) 0.5f else 0.1f)
        val blur by animateFloatAsState(if (pointerInteractionStateHolder.isHovered) 4f else 0.5f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(5.dp)
                .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                .background(RiftTheme.colors.primary.copy(alpha = alpha)),
        ) {}
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(RiftTheme.colors.primary.copy(alpha = 0.5f)),
        ) {}
    }
}

@Composable
fun RiftOpportunityTypeIcon(
    icon: DrawableResource,
    tooltip: String?,
) {
    RiftTooltipArea(
        text = tooltip,
    ) {
        Surface(
            color = RiftTheme.colors.primary.copy(alpha = 0.4f),
            shape = CircleShape,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = RiftTheme.colors.textSecondary,
                modifier = Modifier
                    .padding(2.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
fun RiftOpportunityContainerFlair(
    isHovered: Boolean,
    image: DrawableResource,
    offset: DpOffset,
    alpha: Float = 0.5f,
) {
    val animation = remember { Animatable(0f) }
    val target = -360f
    if (isHovered) {
        LaunchedEffect(Unit) {
            while (isActive) {
                val remaining = (target - animation.value) / target
                animation.animateTo(target, animationSpec = tween((80_000 * remaining).toInt(), easing = LinearEasing))
                animation.snapTo(0f)
            }
        }
    }

    val bitmap = imageResource(image)
    val size = 500
    val color = RiftTheme.colors.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        translate(offset.x.toPx(), offset.y.toPx()) {
            rotate(animation.value, pivot = Offset.Zero) {
                translate(-size / 2f, -size / 2f) {
                    drawImage(
                        image = bitmap,
                        dstSize = IntSize(size, size),
                        alpha = alpha,
                        colorFilter = ColorFilter.tint(color),
                    )
                }
            }
        }
    }
}
