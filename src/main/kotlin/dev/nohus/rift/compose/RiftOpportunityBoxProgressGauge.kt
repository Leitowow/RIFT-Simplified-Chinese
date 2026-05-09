package dev.nohus.rift.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.RiftOpportunityCardTopRight.RiftOpportunityCardProgressGauge
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.corporation_project_state_checkmark_16px
import dev.nohus.rift.generated.resources.corporation_project_state_close_16px
import dev.nohus.rift.generated.resources.corporation_project_state_time_16px
import dev.nohus.rift.network.esi.models.OpportunityState
import dev.nohus.rift.utils.formatNumber
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val saveLayerPaint = Paint()

private data class ProgressConfiguration(
    val outerGauge: Color,
    val innerGauge: Color?,
    val iconColor: Color?,
    val iconResource: DrawableResource?,
)

@Composable
fun RiftOpportunityCardProgressGauge(
    gauge: RiftOpportunityCardProgressGauge,
) {
    RiftTooltipArea(
        tooltip = { ProgressGaugeTooltip(gauge) },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f)),
        ) {
            val diameter = 50.dp
            val diameterPx = LocalDensity.current.run { diameter.toPx().toInt() }
            val gaugeWidthPx = diameterPx * 0.1f
            val outerGaugeDiameterPx = (diameterPx * 0.73).roundToInt()
            val outerGaugeProgress = gauge.currentProgress.toFloat() / gauge.desiredProgress
            val innerGaugeDiameterPx = (diameterPx * 0.38).roundToInt()
            val innerGaugeProgress = gauge.ownProgress.toFloat() / (gauge.participationLimit ?: gauge.desiredProgress)

            val configuration = when (gauge.state) {
                OpportunityState.Completed -> ProgressConfiguration(
                    outerGauge = EveColors.successGreen,
                    innerGauge = null,
                    iconColor = EveColors.platinumGrey,
                    iconResource = Res.drawable.corporation_project_state_checkmark_16px,
                )
                OpportunityState.Active, OpportunityState.Unspecified -> {
                    val hasPersonallyCompleted = innerGaugeProgress >= 1f
                    ProgressConfiguration(
                        outerGauge = Color(0xFFA9DBE9),
                        innerGauge = if (hasPersonallyCompleted) EveColors.successGreen else Color(0xFFA9DBE9),
                        iconColor = if (hasPersonallyCompleted) EveColors.platinumGrey else null,
                        iconResource = if (hasPersonallyCompleted) Res.drawable.corporation_project_state_checkmark_16px else null,
                    )
                }
                OpportunityState.Closed, OpportunityState.Deleted -> ProgressConfiguration(
                    outerGauge = EveColors.warningOrange,
                    innerGauge = null,
                    iconColor = EveColors.warningOrange,
                    iconResource = Res.drawable.corporation_project_state_close_16px,
                )
                OpportunityState.Expired -> ProgressConfiguration(
                    outerGauge = EveColors.dangerRed,
                    innerGauge = null,
                    iconColor = EveColors.dangerRed,
                    iconResource = Res.drawable.corporation_project_state_time_16px,
                )
            }

            Canvas(
                modifier = Modifier.size(diameter, diameter),
            ) {
                drawGauge(outerGaugeDiameterPx, gaugeWidthPx, outerGaugeProgress, configuration.outerGauge)
                if (configuration.innerGauge != null && innerGaugeProgress > 0f) {
                    drawGauge(innerGaugeDiameterPx, gaugeWidthPx, innerGaugeProgress, configuration.innerGauge)
                }
            }

            if (configuration.iconResource != null && configuration.iconColor != null) {
                Icon(
                    painter = painterResource(configuration.iconResource),
                    contentDescription = null,
                    tint = configuration.iconColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun RiftOpportunityCardSmallProgressGauge(
    gauge: RiftOpportunityCardProgressGauge,
) {
    RiftTooltipArea(
        tooltip = { ProgressGaugeTooltip(gauge) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(50.dp),
        ) {
            val progressPercent = (gauge.currentProgress.toFloat() * 100 / gauge.desiredProgress).toInt()
            Text(
                text = "$progressPercent%",
                style = RiftTheme.typography.detailSecondary,
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f)),
            ) {
                val diameter = 32.dp
                val diameterPx = LocalDensity.current.run { diameter.toPx().toInt() }
                val gaugeWidthPx = diameterPx * 0.15f
                val outerGaugeDiameterPx = (diameterPx * 0.6).roundToInt()
                val outerGaugeProgress = gauge.currentProgress.toFloat() / gauge.desiredProgress
                val innerGaugeDiameterPx = (diameterPx * 0.25).roundToInt()
                val innerGaugeProgress =
                    gauge.ownProgress.toFloat() / (gauge.participationLimit ?: gauge.desiredProgress)

                val configuration = when (gauge.state) {
                    OpportunityState.Completed -> ProgressConfiguration(
                        outerGauge = EveColors.successGreen,
                        innerGauge = null,
                        iconColor = EveColors.platinumGrey,
                        iconResource = Res.drawable.corporation_project_state_checkmark_16px,
                    )

                    OpportunityState.Active, OpportunityState.Unspecified -> {
                        val hasPersonallyCompleted = innerGaugeProgress >= 1f
                        ProgressConfiguration(
                            outerGauge = Color(0xFFA9DBE9),
                            innerGauge = if (hasPersonallyCompleted) null else Color(0xFFA9DBE9),
                            iconColor = if (hasPersonallyCompleted) EveColors.platinumGrey else null,
                            iconResource = if (hasPersonallyCompleted) Res.drawable.corporation_project_state_checkmark_16px else null,
                        )
                    }

                    OpportunityState.Closed, OpportunityState.Deleted -> ProgressConfiguration(
                        outerGauge = EveColors.warningOrange,
                        innerGauge = null,
                        iconColor = EveColors.warningOrange,
                        iconResource = Res.drawable.corporation_project_state_close_16px,
                    )

                    OpportunityState.Expired -> ProgressConfiguration(
                        outerGauge = EveColors.dangerRed,
                        innerGauge = null,
                        iconColor = EveColors.dangerRed,
                        iconResource = Res.drawable.corporation_project_state_time_16px,
                    )
                }

                Canvas(
                    modifier = Modifier.size(diameter, diameter),
                ) {
                    drawGauge(outerGaugeDiameterPx, gaugeWidthPx, outerGaugeProgress, configuration.outerGauge)
                    if (configuration.innerGauge != null && innerGaugeProgress > 0f) {
                        drawPieSliceGauge(innerGaugeDiameterPx, gaugeWidthPx, innerGaugeProgress, configuration.innerGauge)
                    }
                }

                if (configuration.iconResource != null && configuration.iconColor != null) {
                    Icon(
                        painter = painterResource(configuration.iconResource),
                        contentDescription = null,
                        tint = configuration.iconColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawPieSliceGauge(
    diameterPx: Int,
    widthPx: Float,
    progress: Float,
    color: Color,
) {
    val canvasRadius = size.width / 2
    val radiusPx = diameterPx.toFloat() / 2
    val radiusStartPercent = (radiusPx - widthPx / 2) / canvasRadius
    val widthPercent = widthPx / canvasRadius

    drawCircle(
        brush = Brush.radialGradient(
            radiusStartPercent to EveColors.black,
            radiusStartPercent + (widthPercent * 0.7f) to EveColors.black,
            radiusStartPercent + widthPercent to Color.Transparent,
            radius = canvasRadius,
        ),
    )

    drawContext.canvas.withSaveLayer(size.toRect(), paint = saveLayerPaint) {
        drawCircle(
            brush = Brush.radialGradient(
                radiusStartPercent to color,
                radiusStartPercent + (widthPercent * 0.7f) to color,
                radiusStartPercent + widthPercent to Color.Transparent,
                radius = canvasRadius,
            ),
        )

        drawArc(
            color = Color.Transparent,
            startAngle = -90f + (progress * 360f),
            sweepAngle = (1f - progress) * 360f,
            useCenter = true,
            blendMode = BlendMode.Clear,
        )
    }
}

private fun DrawScope.drawGauge(
    diameterPx: Int,
    widthPx: Float,
    progress: Float,
    color: Color,
) {
    val canvasRadius = size.width / 2
    val radiusPx = diameterPx.toFloat() / 2
    val radiusStartPercent = (radiusPx - widthPx / 2) / canvasRadius
    val widthPercent = widthPx / canvasRadius

    drawCircle(
        brush = Brush.radialGradient(
            radiusStartPercent to Color.Transparent,
            radiusStartPercent + (widthPercent * 0.3f) to EveColors.black,
            radiusStartPercent + (widthPercent * 0.7f) to EveColors.black,
            radiusStartPercent + widthPercent to Color.Transparent,
            radius = canvasRadius,
        ),
    )

    drawContext.canvas.withSaveLayer(size.toRect(), paint = saveLayerPaint) {
        drawCircle(
            brush = Brush.radialGradient(
                radiusStartPercent to Color.Transparent,
                radiusStartPercent + (widthPercent * 0.3f) to color,
                radiusStartPercent + (widthPercent * 0.7f) to color,
                radiusStartPercent + widthPercent to Color.Transparent,
                radius = canvasRadius,
            ),
        )

        drawArc(
            color = Color.Transparent,
            startAngle = -90f + (progress * 360f),
            sweepAngle = (1f - progress) * 360f,
            useCenter = true,
            blendMode = BlendMode.Clear,
        )
    }
}

@Composable
private fun ProgressGaugeTooltip(gauge: RiftOpportunityCardProgressGauge) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier.padding(Spacing.large),
    ) {
        if (gauge.participationLimit != null) {
            Text(
                text = "This project has a contribution limit",
                style = RiftTheme.typography.bodyDisabled,
            )
        }

        val progressPercent = (gauge.currentProgress.toFloat() * 100 / gauge.desiredProgress).toInt()
        Text(
            text = buildAnnotatedString {
                append("Overall Progress ")
                withStyle(RiftTheme.typography.bodyDisabled.toSpanStyle()) {
                    append("$progressPercent%")
                }
                appendLine()
                withStyle(RiftTheme.typography.bodyPrimary.toSpanStyle()) {
                    append("${formatNumber(gauge.currentProgress)} / ${formatNumber(gauge.desiredProgress)}")
                }
            },
            style = RiftTheme.typography.bodySecondary,
        )

        val ownMaxProgress = gauge.participationLimit ?: gauge.desiredProgress
        val ownProgressPercent = (gauge.ownProgress.toFloat() * 100 / ownMaxProgress).toInt()
        Text(
            text = buildAnnotatedString {
                append("Your Progress ")
                withStyle(RiftTheme.typography.bodyDisabled.toSpanStyle()) {
                    append("$ownProgressPercent%")
                }
                appendLine()
                withStyle(RiftTheme.typography.bodyPrimary.toSpanStyle()) {
                    append("${formatNumber(gauge.ownProgress)} / ${formatNumber(ownMaxProgress)}")
                }
            },
            style = RiftTheme.typography.bodySecondary,
        )
    }
}
