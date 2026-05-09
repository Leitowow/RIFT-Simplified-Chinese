package dev.nohus.rift.wallet.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.utils.formatNumberCompact
import dev.nohus.rift.utils.plural
import dev.nohus.rift.wallet.TransactionGroup
import dev.nohus.rift.wallet.TransferDirection
import dev.nohus.rift.wallet.WalletViewModel.Segment
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.sqrt

private const val MIN_SEGMENT_RATIO = 0.015

// https://issuetracker.google.com/issues/432262806
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun WalletPieChart(
    group: TransactionGroup?,
    days: Int,
    shownBreakdown: TransferDirection,
    segments: List<Segment>,
    onSegmentClick: (Segment) -> Unit,
) {
    var segments = segments
        .map { it.copy(ratio = it.ratio.coerceAtLeast(MIN_SEGMENT_RATIO)) }
    while (true) {
        val ratioSum = segments.sumOf { it.ratio }
        if (ratioSum <= 1.0) break
        segments = segments.map { segment ->
            val newRatio = (segment.ratio / ratioSum).coerceAtLeast(MIN_SEGMENT_RATIO)
            segment.copy(ratio = newRatio)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val canvasSize = minOf(maxWidth, maxHeight)
        var hoveredSegment: Segment? by remember { mutableStateOf(null) }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .pointerInput(segments) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach {
                                hoveredSegment = getSegmentAtPosition(canvasSize, it.position, segments)
                            }
                        }
                    }
                }
                .pointerInput(segments) {
                    detectTapGestures(
                        onTap = {
                            val segment = getSegmentAtPosition(canvasSize, it, segments)
                            onSegmentClick(segment ?: return@detectTapGestures)
                        },
                    )
                }
                .size(canvasSize),
        ) {
            val animation = remember(segments) { Animatable(0f) }
            LaunchedEffect(segments) {
                animation.animateTo(1f, animationSpec = tween(600))
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                val strokeWidth = 32.dp.toPx()
                val shadowStrokeWidth = 8.dp.toPx()
                val segmentGap = 8.dp.toPx()
                val diameter = minOf(size.width, size.height)
                val circumference = diameter * Math.PI
                val segmentGapAngle = ((segmentGap / circumference) * 360f).toFloat()

                var startAngle = 0f
                segments
                    .sortedByDescending { it.value.absoluteValue }
                    .forEach { segment ->
                        val sweepAngle = -(segment.ratio * 360f).toFloat()
                        val shadowDiameter = diameter - shadowStrokeWidth
                        drawArc(
                            color = segment.color.copy(alpha = 0.5f),
                            startAngle = startAngle,
                            sweepAngle = (sweepAngle + segmentGapAngle) * animation.value,
                            useCenter = false,
                            topLeft = Offset(shadowStrokeWidth / 2, shadowStrokeWidth / 2),
                            size = Size(shadowDiameter, shadowDiameter),
                            style = Stroke(width = shadowStrokeWidth, cap = StrokeCap.Butt),
                        )
                        val segmentDiameter = diameter - (shadowStrokeWidth * 2) - strokeWidth
                        val offset = shadowStrokeWidth + strokeWidth / 2
                        drawArc(
                            color = segment.color,
                            startAngle = startAngle,
                            sweepAngle = (sweepAngle + segmentGapAngle) * animation.value,
                            useCenter = false,
                            topLeft = Offset(offset, offset),
                            size = Size(segmentDiameter, segmentDiameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        )
                        startAngle += sweepAngle
                    }
            }

            val total = hoveredSegment?.value ?: segments.sumOf { it.value }
            val name = hoveredSegment?.name ?: group?.name
            CenterInfo(
                total = total,
                name = name,
                days = days,
                shownBreakdown = shownBreakdown,
            )
        }
    }
}

private fun Density.getSegmentAtPosition(
    canvasSize: Dp,
    position: Offset,
    segments: List<Segment>,
): Segment? {
    val radius = canvasSize.toPx() / 2
    val x = position.x - radius
    val y = position.y - radius
    val pointerAngle = 360 - ((Math.toDegrees(atan2(y.toDouble(), x.toDouble())) + 360) % 360)
    val pointerDistance = sqrt(x * x + y * y)

    if (pointerDistance in (radius - 40.dp.toPx())..radius) {
        var currentAngle = 0.0
        for (segment in segments.sortedByDescending { it.value.absoluteValue }) {
            val sweepAngle = (segment.ratio * 360.0)
            if (pointerAngle >= currentAngle && pointerAngle <= currentAngle + sweepAngle) {
                return segment
            }
            currentAngle += sweepAngle
        }
    }
    return null
}

@Composable
private fun CenterInfo(
    total: Double,
    name: String?,
    days: Int,
    shownBreakdown: TransferDirection,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatNumberCompact(total),
            style = RiftTheme.typography.displayHighlighted,
        )
        Spacer(Modifier.height(Spacing.medium))
        AnimatedVisibility(name != null) {
            Text(
                text = name ?: "",
                style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
            )
        }
        Spacer(Modifier.height(Spacing.small))
        Text(
            text = when (shownBreakdown) {
                TransferDirection.Income -> "$days Day${days.plural} Income"
                TransferDirection.Expense -> "$days Day${days.plural} Expenses"
            },
            style = RiftTheme.typography.bodySecondary,
        )
    }
}
