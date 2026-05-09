package dev.nohus.rift.debug

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.network.requests.RequestStatisticsInterceptor
import java.time.Instant

@Composable
fun NetworkChart(
    modifier: Modifier,
    buckets: List<RequestStatisticsInterceptor.Bucket>,
) {
    var currentInstant by remember { mutableStateOf(Instant.now()) }
    val currentSecondAnimation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            currentInstant = Instant.now()
            currentSecondAnimation.snapTo(0f)
            currentSecondAnimation.animateTo(1f, animationSpec = tween(1_000, easing = LinearEasing))
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val legendStyle = RiftTheme.typography.bodySecondary

    val barWidth = 10.dp
    val barsPadding = 2.dp
    val responseWidth = 4.dp
    val yAxisLegendWidth = 20.dp
    val xAxisLegendHeight = 20.dp
    val barTotalWidth = barWidth + barsPadding

    if (buckets.isNotEmpty()) {
        Canvas(
            modifier = modifier.clipToBounds(),
        ) {
            val barsAvailableWidth = size.width - yAxisLegendWidth.toPx()
            val barsAvailableHeight = size.height - xAxisLegendHeight.toPx()
            val barsCount = (barsAvailableWidth / barTotalWidth.toPx()).toInt() + 1 // Overdraw one partial bar to fill the width
            val oldestVisibleEpochSecond = currentInstant.epochSecond - barsCount
            val visibleBuckets = buckets.takeLastWhile { it.epochSeconds >= oldestVisibleEpochSecond }
            val bucketsByEpochSecond = visibleBuckets.associateBy { it.epochSeconds }
            fun Int.roundUpToNext10() = if (this == 0) 10 else ((this - 1) / 10 + 1) * 10
            val yMax = (visibleBuckets.maxOfOrNull { it.requests.size } ?: 0).roundUpToNext10().coerceAtLeast(20)

            // Draw y-axis
            val yAxisLegend = List(5) {
                val yAmount = yMax / (5 - it)
                textMeasurer.measure(
                    text = "$yAmount",
                    style = legendStyle,
                )
            }
            val maxYAxisLegendWidth = yAxisLegend.maxOf { it.size.width }

            yAxisLegend.forEachIndexed { it, textLayoutResult ->
                val y = barsAvailableHeight - (it + 1) * (barsAvailableHeight / 5)
                drawLine(
                    color = EveColors.gunmetalGrey,
                    start = Offset(0f, y + xAxisLegendHeight.toPx()),
                    end = Offset(size.width, y + xAxisLegendHeight.toPx()),
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = size.width - maxYAxisLegendWidth,
                        y = y + xAxisLegendHeight.toPx() + 2,
                    ),
                )
            }

            // Draw x-axis
            val xAxisLegend = List(barsCount / 10 + 1) {
                val minusSeconds = 10 * it
                val text = if (minusSeconds == 0) "Now" else "${minusSeconds}s"
                textMeasurer.measure(
                    text = text,
                    style = legendStyle,
                )
            }

            xAxisLegend.forEachIndexed { it, textLayoutResult ->
                val x = barsAvailableWidth - (it * 10 * barTotalWidth.toPx()) - barTotalWidth.toPx() / 2

                drawLine(
                    color = EveColors.gunmetalGrey,
                    start = Offset(x, xAxisLegendHeight.toPx()),
                    end = Offset(x, size.height),
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = x - textLayoutResult.size.width / 2,
                        y = 0f,
                    ),
                )
            }

            // Draw bars
            repeat(barsCount) { barIndex ->
                val bucketEpochSecond = currentInstant.epochSecond - barIndex
                val bucket = bucketsByEpochSecond[bucketEpochSecond]
                val requests = bucket?.requests ?: emptyList()

                fun drawBarSegment(offsetCount: Int, requestCount: Int, color: Color, sideColor: Color?) {
                    val heightPerRequest = barsAvailableHeight / yMax.toFloat()
                    val height = requestCount * heightPerRequest
                    val offsetHeight = offsetCount * heightPerRequest
                    val animatedSecondFraction = (currentInstant.toEpochMilli() % 1000) / 1000f + currentSecondAnimation.value
                    val animationOffset = -(barTotalWidth.toPx() * animatedSecondFraction)
                    val responseWidth = if (sideColor != null) responseWidth else 0.dp

                    val x = barsAvailableWidth - (barIndex + 1) * barTotalWidth.toPx() + animationOffset
                    val y = xAxisLegendHeight.toPx() + barsAvailableHeight - height - offsetHeight
                    drawRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size((barWidth - responseWidth).toPx() - 1, height),
                    )
                    if (sideColor != null) {
                        drawRect(
                            color = sideColor.copy(alpha = 0.8f),
                            topLeft = Offset(x + (barWidth - responseWidth).toPx(), y),
                            size = Size((responseWidth).toPx() - 1, height),
                        )
                    }
                }

                if (requests.isNotEmpty()) {
                    var drawn = 0
                    requests.groupBy { it.originator }.forEach { (originator, requests) ->
                        val successCount = requests.count { it.response?.isSuccess == true }
                        val failureCount = requests.count { it.response?.isSuccess == false }
                        val pendingCount = requests.count { it.response == null }

                        drawBarSegment(drawn, successCount, originator.color.copy(alpha = 0.8f), null)
                        drawn += successCount
                        drawBarSegment(drawn, failureCount, originator.color.copy(alpha = 0.8f), EveColors.dangerRed)
                        drawn += failureCount
                        drawBarSegment(drawn, pendingCount, originator.color.copy(alpha = 0.2f), null)
                        drawn += pendingCount
                    }
                }
            }
        }
    }
}
