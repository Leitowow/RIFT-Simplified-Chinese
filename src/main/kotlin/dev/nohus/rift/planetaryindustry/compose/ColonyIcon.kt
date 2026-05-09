package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nohus.rift.assets.PlanetaryIndustryCommoditiesRepository
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.scale
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.pi_disc_shadow
import dev.nohus.rift.generated.resources.pi_ecu_top
import dev.nohus.rift.generated.resources.pi_gauge_15px
import dev.nohus.rift.generated.resources.pi_needsattention
import dev.nohus.rift.generated.resources.pi_processor
import dev.nohus.rift.generated.resources.pi_processoradvanced
import dev.nohus.rift.generated.resources.pi_processorhightech
import dev.nohus.rift.generated.resources.planet_barren_128
import dev.nohus.rift.generated.resources.planet_gas_128
import dev.nohus.rift.generated.resources.planet_ice_128
import dev.nohus.rift.generated.resources.planet_lava_128
import dev.nohus.rift.generated.resources.planet_ocean_128
import dev.nohus.rift.generated.resources.planet_plasma_128
import dev.nohus.rift.generated.resources.planet_storm_128
import dev.nohus.rift.generated.resources.planet_temperate_128
import dev.nohus.rift.network.esi.models.PlanetType
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.planetaryindustry.models.Colony
import dev.nohus.rift.planetaryindustry.models.ColonyOverview
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Extracting
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Idle
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NeedsAttention
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NotSetup
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Producing
import dev.nohus.rift.repositories.TypesRepository.Type
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val colonyIconSize = 72.dp
private val colonyIconPlanetSize = 64.dp

/**
 * Grid/Rows view planet icon
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColonyPlanetSnippet(
    item: ColonyItem,
    isShowingCharacter: Boolean,
    transition: InfiniteTransition,
    colonyIconModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    onExpandClick: () -> Unit,
) {
    val colony = item.colony
    val pointerInteraction = rememberPointerInteractionStateHolder()
    Box(
        modifier = modifier
            .pointerInteraction(pointerInteraction)
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .onClick { onExpandClick() }
            .modifyIf(isShowingCharacter) { Modifier.size(colonyIconSize + 16.dp) },
    ) {
        Box {
            val hoverAlpha by animateFloatAsState(if (pointerInteraction.isHovered) 1f else 0f)
            Image(
                painter = painterResource(Res.drawable.pi_disc_shadow),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .requiredSize(colonyIconSize)
                    .scale(1.2f)
                    .alpha(hoverAlpha)
                    .align(Alignment.TopStart),
            )
            ColonyIcon(
                colony = colony,
                transition = transition,
                modifier = colonyIconModifier
                    .requiredSize(colonyIconSize)
                    .align(Alignment.TopStart),
            )
        }
        if (isShowingCharacter) {
            val animatable = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(200)
                animatable.animateTo(1f, tween(500))
            }
            Box(
                modifier = Modifier
                    .scale(animatable.value)
                    .align(Alignment.BottomEnd)
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
        }
    }
}

/**
 * Planet icon with production status
 */
@Composable
fun ColonyIcon(
    colony: Colony,
    transition: InfiniteTransition,
    modifier: Modifier = Modifier,
) {
    val type = colony.type
    RiftTooltipArea(
        tooltip = { ColonyTooltip(colony) },
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(colonyIconSize),
        ) {
            ColonyFillLevel(colony.overview)
            PlanetIcon(type, colony)
            StatusIcon(colony, transition)
        }
    }
}

@Composable
private fun PlanetIcon(
    type: PlanetType,
    colony: Colony,
) {
    val icon = when (type) {
        PlanetType.Temperate -> Res.drawable.planet_temperate_128
        PlanetType.Barren -> Res.drawable.planet_barren_128
        PlanetType.Oceanic -> Res.drawable.planet_ocean_128
        PlanetType.Ice -> Res.drawable.planet_ice_128
        PlanetType.Gas -> Res.drawable.planet_gas_128
        PlanetType.Lava -> Res.drawable.planet_lava_128
        PlanetType.Storm -> Res.drawable.planet_storm_128
        PlanetType.Plasma -> Res.drawable.planet_plasma_128
    }
    val colorFilter = if (colony.status is NeedsAttention) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.25f) })
    } else {
        null
    }
    Image(
        painter = painterResource(icon),
        contentDescription = null,
        colorFilter = colorFilter,
        modifier = Modifier.size(colonyIconPlanetSize),
    )
}

@Composable
private fun ColonyTooltip(
    colony: Colony,
) {
    Column(
        modifier = Modifier.padding(Spacing.large),
    ) {
        when (colony.status) {
            is Extracting -> Text("Extracting", fontWeight = FontWeight.Bold, color = RiftTheme.colors.textGreen)
            is Producing -> Text("Producing", fontWeight = FontWeight.Bold, color = RiftTheme.colors.textGreen)
            is NotSetup -> Text("Not setup", fontWeight = FontWeight.Bold, color = RiftTheme.colors.textRed)
            is NeedsAttention -> Text("Needs attention", fontWeight = FontWeight.Bold, color = RiftTheme.colors.textRed)
            is Idle -> Text("Idle", fontWeight = FontWeight.Bold)
        }
        val finalProducts = colony.overview.finalProducts
        if (finalProducts.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(vertical = Spacing.small),
            ) {
                finalProducts.forEach { product ->
                    AsyncTypeIcon(
                        type = product,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Column(
                    modifier = Modifier.padding(start = Spacing.small),
                ) {
                    Text(
                        text = "Producing",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    Text(
                        text = finalProducts.joinToString { it.name },
                        style = RiftTheme.typography.bodySecondary,
                    )
                }
            }

            AnnotatedProgressBar(
                title = "Product storage",
                percentage = colony.overview.finalProductsUsedCapacity.toFloat() / colony.overview.capacity,
                secondaryPercentage = colony.overview.otherUsedCapacity.toFloat() / colony.overview.capacity,
                description = String.format("%.0f/%.0f m3", colony.overview.finalProductsUsedCapacity, colony.overview.capacity.toFloat()),
                color = RiftTheme.colors.progressBarProgress,
                secondaryColor = Color(0xFFed9935),
                modifier = Modifier.padding(bottom = Spacing.small),
            )
        }
        Text(
            text = colony.planet.name,
            style = RiftTheme.typography.bodyPrimary,
        )
        Text(
            text = "${colony.type.name} planet",
            style = RiftTheme.typography.bodySecondary,
        )
    }
}

@Composable
private fun StatusIcon(colony: Colony, transition: InfiniteTransition) {
    Box(contentAlignment = Alignment.Center) {
        if (colony.status is Extracting) {
            ExtractionAnimation(transition)
        }
        if (colony.status is Producing) {
            ProductionAnimation(transition, colony.overview.finalProducts, isWorking = true)
        }
        if (colony.status is Idle) {
            ProductionAnimation(transition, colony.overview.finalProducts, isWorking = false)
        }
        ProductIcon(colony, modifier = Modifier)
        if (colony.status is NotSetup || colony.status is NeedsAttention) {
            NeedsAttentionAnimation(transition)
        }
    }
}

@Composable
private fun NeedsAttentionAnimation(transition: InfiniteTransition) {
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), repeatMode = RepeatMode.Reverse),
    )
    Image(
        painter = painterResource(Res.drawable.pi_needsattention),
        contentDescription = null,
        colorFilter = ColorFilter.tint(Color(0xFFFF0000)),
        alpha = alpha,
        modifier = Modifier.size(41.dp),
    )
}

@Composable
private fun ProductionAnimation(
    transition: InfiniteTransition,
    products: Set<Type>,
    isWorking: Boolean,
) {
    if (products.isEmpty()) return
    val repository: PlanetaryIndustryCommoditiesRepository = remember { koin.get() }
    val tier = remember(products) { products.maxOfOrNull { repository.getTier(it) ?: 0 } ?: 0 }
    val processorImage = when (tier) {
        4 -> Res.drawable.pi_processorhightech
        3, 2 -> Res.drawable.pi_processoradvanced
        else -> Res.drawable.pi_processor
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(colonyIconPlanetSize),
    ) {
        repeat(4) { index ->
            val animation by transition.animateFloat(
                0f,
                1f,
                infiniteRepeatable(
                    animation = tween(15_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
            val pxOffset = LocalDensity.current.run { 16.dp.toPx() }
            val naturalRotation = 30f * index
            val animatedRotation = (360f * animation).let { if (index % 2 == 0) it else -it } + naturalRotation
            val rotation = if (isWorking) animatedRotation else naturalRotation
            Image(
                painter = painterResource(processorImage),
                contentDescription = null,
                modifier = Modifier
                    .rotate(index * 90f)
                    .graphicsLayer {
                        translationY = -pxOffset
                        alpha = if (isWorking) 1f else 0.5f
                    }
                    .size(32.dp)
                    .rotate(rotation),
            )
        }
    }
}

@Composable
private fun ExtractionAnimation(transition: InfiniteTransition) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(colonyIconPlanetSize),
    ) {
        repeat(4) {
            val duration = 4_000
            val animation by transition.animateFloat(
                0f,
                1f,
                infiniteRepeatable(
                    animation = tween(duration),
                    repeatMode = RepeatMode.Restart,
                ),
            )
            val animatedAlpha by transition.animateFloat(
                1.5f,
                0f,
                infiniteRepeatable(
                    animation = tween(duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
            val pxOffset = LocalDensity.current.run { (16 * animation).dp.toPx() }
            Image(
                painter = painterResource(Res.drawable.pi_ecu_top),
                contentDescription = null,
                modifier = Modifier
                    .rotate(it * 90f)
                    .graphicsLayer {
                        translationY = -pxOffset
                        scaleX = animation.coerceAtLeast(0.5f)
                        scaleY = animation.coerceAtLeast(0.5f)
                        alpha = animatedAlpha.coerceAtMost(1f)
                    }
                    .size(32.dp),
            )
        }
    }
}

/**
 * Type icon of the colony final product
 * Shows one product if multiple
 */
@Composable
private fun ProductIcon(
    colony: Colony,
    modifier: Modifier = Modifier,
) {
    val product = colony.overview.finalProducts.firstOrNull() ?: return
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(Res.drawable.pi_disc_shadow),
            contentDescription = null,
            alpha = 0.5f,
            modifier = Modifier.size(32.dp),
        )
        AsyncTypeIcon(
            type = product,
            modifier = Modifier.size(32.dp),
        )
    }
}

private val saveLayerPaint = Paint()
private val fillLevelGaugeTint = ColorFilter.tint(Color(0xFF0a4366), BlendMode.Modulate)
private val fillLevelGaugeUsedTint = ColorFilter.tint(Color(0xFF00a9f4), BlendMode.Modulate)
private val fillLevelGaugeOtherUsedTint = ColorFilter.tint(Color(0xFFed9935), BlendMode.Modulate)

@Composable
private fun ColonyFillLevel(overview: ColonyOverview) {
    val size = colonyIconSize
    val pxSize = LocalDensity.current.run { size.toPx().toInt() }
    val gauge = imageResource(Res.drawable.pi_gauge_15px, pxSize)
    Canvas(modifier = Modifier.size(size)) {
        drawImage(
            image = gauge,
            colorFilter = fillLevelGaugeTint,
        )

        val finalProductsPercentageUsed = overview.finalProductsUsedCapacity.toFloat() / overview.capacity
        val otherPercentageUsed = overview.otherUsedCapacity.toFloat() / overview.capacity
        drawContext.canvas.withSaveLayer(this.size.toRect(), paint = saveLayerPaint) {
            drawImage(
                image = gauge,
                colorFilter = fillLevelGaugeUsedTint,
            )

            val percentageFree = 1 - finalProductsPercentageUsed
            drawArc(
                color = Color.Transparent,
                startAngle = -90f + (finalProductsPercentageUsed * 360f),
                sweepAngle = percentageFree * 360f,
                useCenter = true,
                blendMode = BlendMode.Clear,
            )
        }

        drawContext.canvas.withSaveLayer(this.size.toRect(), paint = saveLayerPaint) {
            drawImage(
                image = gauge,
                colorFilter = fillLevelGaugeOtherUsedTint,
            )

            val percentageFree = 1 - otherPercentageUsed
            drawArc(
                color = Color.Transparent,
                startAngle = -90f + (otherPercentageUsed * 360f) + (finalProductsPercentageUsed * 360f),
                sweepAngle = percentageFree * 360f,
                useCenter = true,
                blendMode = BlendMode.Clear,
            )
        }
    }
}

private val imageCache = mutableMapOf<Pair<DrawableResource, Int>, ImageBitmap>()

/**
 * Version of imageResource that will do a high quality rescale of the image and cache the result
 */
@Composable
private fun imageResource(resource: DrawableResource, sizeToScale: Int): ImageBitmap {
    val original = org.jetbrains.compose.resources.imageResource(resource)
    val scaled = remember {
        imageCache[resource to sizeToScale]?.let { return@remember it }
        original.scale(sizeToScale, sizeToScale).also { imageCache[resource to sizeToScale] = it }
    }
    return scaled
}
