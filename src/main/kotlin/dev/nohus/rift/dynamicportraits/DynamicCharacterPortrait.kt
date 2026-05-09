package dev.nohus.rift.dynamicportraits

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.di.koin
import dev.nohus.rift.settings.persistence.CharacterPortraitsParallaxStrength
import dev.nohus.rift.settings.persistence.CharacterPortraitsStandingsTargets
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.getColor
import dev.nohus.rift.standings.isFriendly
import dev.nohus.rift.standings.isHostile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import kotlin.math.sqrt

/**
 * @param enterTimestamp if provided will add an entrance transition. Stagger in lists.
 * @param pointerInteractionStateHolder if provided will add a hover effect
 */
@Composable
fun DynamicCharacterPortraitParallax(
    characterId: Int,
    size: Dp,
    enterTimestamp: Instant?,
    pointerInteractionStateHolder: PointerInteractionStateHolder?,
    modifier: Modifier = Modifier,
) {
    val settings: Settings = remember { koin.get() }
    val parallaxStrength by settings.updateFlow.map { it.characterPortraits.parallaxStrength }.collectAsState(initial = CharacterPortraitsParallaxStrength.Normal)
    val backgroundSize = if (parallaxStrength == CharacterPortraitsParallaxStrength.None) {
        size
    } else {
        size + size / 4
    }
    DynamicPortraitContainer(characterId, size, backgroundSize, modifier) { dynamicPortrait ->
        ParallaxEffect(dynamicPortrait, size, backgroundSize, parallaxStrength, enterTimestamp, pointerInteractionStateHolder)
    }
}

@Composable
fun DynamicCharacterPortraitStandings(
    characterId: Int,
    size: Dp,
    standingLevel: Standing,
    isAnimated: Boolean,
    enterTimestamp: Instant = Instant.now(),
) {
    DynamicPortraitContainer(characterId, size) { dynamicPortrait ->
        StandingsEffect(dynamicPortrait, size, standingLevel, isAnimated, enterTimestamp)
    }
}

@Composable
private fun DynamicPortraitContainer(
    characterId: Int,
    size: Dp,
    backgroundSize: Dp = size,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(DynamicPortrait) -> Unit,
) {
    val portrait = getDynamicPortrait(characterId, size, backgroundSize)
    Box(
        modifier
            .clipToBounds()
            .size(size),
    ) {
        Crossfade(portrait) {
            it?.let {
                content(it)
            }
        }
    }
}

@Composable
private fun getDynamicPortrait(
    characterId: Int,
    size: Dp,
    backgroundSize: Dp = size,
): DynamicPortrait? {
    val dynamicPortraitRepository: DynamicPortraitRepository = remember { koin.get() }
    val sizePx = LocalDensity.current.run { size.toPx().toInt() }
    val backgroundSizePx = LocalDensity.current.run { backgroundSize.toPx().toInt() }
    var dynamicPortrait by remember(characterId, sizePx, backgroundSizePx) {
        mutableStateOf<DynamicPortrait?>(null)
    }
    LaunchedEffect(characterId, sizePx, backgroundSizePx) {
        dynamicPortrait = dynamicPortraitRepository.getCharacterPortrait(characterId, sizePx, backgroundSizePx)
    }
    return dynamicPortrait
}

/**
 * @param enterTimestamp if provided will add an entrance transition. Stagger in lists.
 * @param pointerInteractionStateHolder if provided will add a hover effect
 */
@Composable
private fun BoxScope.ParallaxEffect(
    dynamicPortrait: DynamicPortrait,
    size: Dp,
    backgroundSize: Dp,
    parallaxStrength: CharacterPortraitsParallaxStrength,
    enterTimestamp: Instant?,
    pointerInteractionStateHolder: PointerInteractionStateHolder?,
) {
    val portraitEnterOffset = remember { Animatable(-1f) }
    val duration = when (parallaxStrength) {
        CharacterPortraitsParallaxStrength.None -> 8_000
        CharacterPortraitsParallaxStrength.Reduced -> 16_000
        CharacterPortraitsParallaxStrength.Normal -> 8_000
    }
    val loop = rememberSynchronizedAnimationLoop(durationMillis = duration)

    LaunchedEffect(dynamicPortrait, size, backgroundSize, enterTimestamp) {
        if (enterTimestamp == null) {
            portraitEnterOffset.snapTo(0f)
            return@LaunchedEffect
        }
        val enterDuration = Duration.ofMillis(1000)
        val timeToStart = Duration.between(Instant.now(), enterTimestamp).toMillis()
        if (timeToStart > 0) {
            delay(timeToStart)
            portraitEnterOffset.animateTo(0f, animationSpec = tween(enterDuration.toMillis().toInt(), easing = LinearOutSlowInEasing))
        } else {
            portraitEnterOffset.snapTo(0f)
        }
    }

    val portraitSize = LocalDensity.current.run { size.toPx().toInt() }
    val offset = size / 8
    val offsetPx = LocalDensity.current.run { offset.toPx() }
    val blur by animateDpAsState(
        targetValue = if (pointerInteractionStateHolder?.isHovered ?: false) 2.dp else 0.dp,
        animationSpec = tween(500),
    )
    val alpha by animateFloatAsState(
        targetValue = if (pointerInteractionStateHolder?.isHovered ?: false) 0.5f else 1f,
        animationSpec = tween(500),
    )
    Image(
        bitmap = dynamicPortrait.background,
        contentDescription = null,
        modifier = Modifier
            .blur(blur)
            .modifyIf(parallaxStrength != CharacterPortraitsParallaxStrength.None) {
                graphicsLayer {
                    translationX = offsetPx * loop
                    this.alpha = alpha
                }
            }
            .align(alignment = Alignment.BottomCenter)
            .requiredSize(backgroundSize),
    )
    Image(
        bitmap = dynamicPortrait.portrait,
        contentDescription = null,
        modifier = Modifier
            .graphicsLayer {
                translationX = portraitSize * portraitEnterOffset.value
            }
            .size(size),
    )
}

@Composable
private fun StandingsEffect(
    dynamicPortrait: DynamicPortrait,
    size: Dp,
    standingLevel: Standing,
    isAnimated: Boolean,
    enterTimestamp: Instant,
) {
    val color = standingLevel.getColor() ?: Color.White
    Image(
        bitmap = dynamicPortrait.background,
        contentDescription = null,
        modifier = Modifier
            .size(size),
    )

    val settings: Settings = remember { koin.get() }
    val targets by settings.updateFlow.map { it.characterPortraits.standingsTargets }.collectAsState(initial = CharacterPortraitsStandingsTargets.All)
    val isEnabled = when (targets) {
        CharacterPortraitsStandingsTargets.All -> true
        CharacterPortraitsStandingsTargets.OnlyFriendly -> standingLevel.isFriendly
        CharacterPortraitsStandingsTargets.OnlyHostile -> standingLevel.isHostile
        CharacterPortraitsStandingsTargets.OnlyNonNeutral -> standingLevel != Standing.Neutral
        CharacterPortraitsStandingsTargets.None -> false
    }
    if (isEnabled) {
        val standingsEffectStrength by settings.updateFlow.map { it.characterPortraits.standingsEffectStrength }.collectAsState(initial = 1f)
        val alphaTarget = 0.3f
        val alpha = remember { Animatable(if (isAnimated) 1f else alphaTarget) }
        LaunchedEffect(dynamicPortrait, size, standingLevel, enterTimestamp, alphaTarget) {
            if (isAnimated && Duration.between(enterTimestamp, Instant.now()) < Duration.ofMillis(500)) {
                alpha.animateTo(alphaTarget, animationSpec = tween(1000, easing = FastOutSlowInEasing))
            } else {
                alpha.snapTo(alphaTarget)
            }
        }
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        0f to color.copy(alpha = standingsEffectStrength),
                        0.3f to color.copy(alpha = alpha.value * standingsEffectStrength),
                        0.7f to color.copy(alpha = alpha.value * standingsEffectStrength),
                        1f to color.copy(alpha = standingsEffectStrength),
                    ),
                )
                .requiredSize(size * sqrt(2f)),
        )
    }

    Image(
        bitmap = dynamicPortrait.portrait,
        contentDescription = null,
        modifier = Modifier
            .size(size),
    )
}

@Composable
private fun BlurEffect(dynamicPortrait: DynamicPortrait, size: Dp) {
    Image(
        bitmap = dynamicPortrait.background,
        contentDescription = null,
        modifier = Modifier
            .blur(8.dp)
            .size(size),
    )
    Image(
        bitmap = dynamicPortrait.portrait,
        contentDescription = null,
        modifier = Modifier.size(size),
    )
}

private val globalStartTimeNanos = System.nanoTime()

@Composable
private fun rememberSynchronizedAnimationLoop(durationMillis: Int): Float {
    var animation by remember { mutableStateOf(-1f) }

    LaunchedEffect(durationMillis) {
        while (true) {
            withFrameNanos { now ->
                val elapsedMillis = (now - globalStartTimeNanos).toFloat() / 1_000_000f
                val halfMillis = durationMillis / 2f
                val progressMillis = elapsedMillis % durationMillis

                animation = if (progressMillis < halfMillis) {
                    lerp(-1f, 1f, FastOutSlowInEasing.transform(progressMillis / halfMillis))
                } else {
                    lerp(1f, -1f, FastOutSlowInEasing.transform((progressMillis - halfMillis) / halfMillis))
                }
            }
        }
    }

    return animation
}
