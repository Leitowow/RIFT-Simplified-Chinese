package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Non-lazy equivalent of LazyVerticalGrid.
 *
 * Lays out children in rows. The number of columns is computed from the available width
 * and the provided minimum column width. The last row, if not full, still spans the full
 * width by distributing the remaining space across its items.
 *
 * horizontalSpacing/verticalSpacing specify the gaps between items.
 */
@Composable
fun VerticalGrid(
    minColumnWidth: Dp,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        // Resolve available width; prefer maxWidth, fall back to minWidth if unbounded
        val availableWidth = if (constraints.maxWidth != Constraints.Infinity) {
            constraints.maxWidth
        } else {
            constraints.minWidth
        }

        // If width is still 0 or no children, nothing to layout
        if (availableWidth <= 0 || measurables.isEmpty()) {
            return@Layout layout(constraints.minWidth, 0) {}
        }

        val minColumnWidthPx = minColumnWidth.roundToPx().coerceAtLeast(1)
        val horizontalSpacingPx = horizontalSpacing.roundToPx().coerceAtLeast(0)
        val verticalSpacingPx = verticalSpacing.roundToPx().coerceAtLeast(0)

        // Compute how many columns can fit
        val columns = if (minColumnWidthPx > availableWidth) {
            1
        } else {
            val denominator = (minColumnWidthPx + horizontalSpacingPx)
            max(1, (availableWidth + horizontalSpacingPx) / denominator)
        }

        // Helper to compute per-column widths for a row that has 'count' items,
        // distributing any remainder pixels from integer division to the first items.
        // It ensures the items plus gaps exactly span the available width.
        fun columnWidthsFor(count: Int): IntArray {
            val contentWidth = availableWidth - horizontalSpacingPx * (count - 1)
            val base = contentWidth / count
            val remainder = contentWidth % count
            return IntArray(count) { index ->
                if (index < remainder) base + 1 else base
            }
        }

        val placeables = ArrayList<Placeable>(measurables.size)
        val rowHeights = ArrayList<Int>()
        val rowItemCounts = ArrayList<Int>() // store item count per row to help with placement

        var index = 0
        while (index < measurables.size) {
            val remaining = measurables.size - index
            val countThisRow = if (remaining >= columns) columns else remaining

            val columnWidths = columnWidthsFor(countThisRow)

            // Measure items for this row with exact widths computed for the row.
            var rowMaxHeight = 0
            for (i in 0 until countThisRow) {
                val item = measurables[index + i]
                val itemWidth = columnWidths[i]
                val childConstraints = Constraints(
                    minWidth = itemWidth,
                    maxWidth = itemWidth,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight,
                )
                val placeable = item.measure(childConstraints)
                placeables.add(placeable)
                rowMaxHeight = max(rowMaxHeight, placeable.height)
            }
            rowHeights.add(rowMaxHeight)
            rowItemCounts.add(countThisRow)

            index += countThisRow
        }

        val measuredHeight = if (rowHeights.isEmpty()) {
            0
        } else {
            val gaps = verticalSpacingPx * (rowHeights.size - 1)
            rowHeights.sum() + gaps
        }

        val maxHeight = if (constraints.maxHeight == Constraints.Infinity) Int.MAX_VALUE else constraints.maxHeight
        val totalHeight = measuredHeight.coerceIn(constraints.minHeight, maxHeight)

        val finalWidth = availableWidth.coerceIn(
            constraints.minWidth,
            if (constraints.maxWidth == Constraints.Infinity) Int.MAX_VALUE else constraints.maxWidth,
        )

        layout(width = finalWidth, height = totalHeight) {
            var y = 0
            var placedIndex = 0
            for (row in rowHeights.indices) {
                val countThisRow = rowItemCounts[row]
                val columnWidths = columnWidthsFor(countThisRow)

                var x = 0
                var rowHeight = rowHeights[row]
                // If totalHeight was constrained smaller than measured sum, avoid placing beyond.
                if (y + rowHeight > totalHeight) {
                    rowHeight = max(0, totalHeight - y)
                }

                for (i in 0 until countThisRow) {
                    val placeable = placeables[placedIndex + i]
                    // Center vertically within the row height if child's height is smaller.
                    val yOffset = ((rowHeight - placeable.height).coerceAtLeast(0)) / 2
                    placeable.placeRelative(x = x, y = y + yOffset)
                    x += columnWidths[i]
                    if (i != countThisRow - 1) x += horizontalSpacingPx
                }

                placedIndex += countThisRow
                y += rowHeight
                if (row != rowHeights.lastIndex) y += verticalSpacingPx
                if (y >= totalHeight) break
            }
        }
    }
}
