package dev.nohus.rift.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.table_sort_ascending
import dev.nohus.rift.generated.resources.table_sort_descending
import org.jetbrains.compose.resources.painterResource

sealed interface TableCell {

    /**
     * Amount is used for sorting, if null, the column will be sorted alphabetically by the text
     */
    data class TextTableCell(
        val text: String,
        val sortingAmount: Double? = null,
    ) : TableCell

    data class RichTableCell(
        val sortingText: String,
        val height: Dp,
        val naturalWidth: Dp,
        val content: @Composable () -> Unit,
    ) : TableCell
}

data class TableRow(
    val id: String,
    val cells: List<TableCell>,
    val characterId: Int? = null,
)

data class SortingColumn(
    val index: Int,
    val sort: Sort,
)

enum class Sort {
    Ascending,
    Descending,
}

/**
 * Table layout
 * extraSpacing is the additional width that each column will start with over its natural width
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RiftTable(
    columns: List<String>,
    rows: List<TableRow>,
    extraSpacing: Dp = 0.dp,
    defaultSort: SortingColumn? = null,
    modifier: Modifier = Modifier,
) {
    val headingStyle = RiftTheme.typography.bodySecondary
    val cellStyle = RiftTheme.typography.bodyPrimary
    val cellHorizontalPadding = Spacing.medium
    val rowVerticalPadding = Spacing.verySmall
    val dividerThickness = 1.dp
    val minColumnWidth = 35.dp

    val textCount = rows.flatMap { it.cells }.size + columns.size
    val textMeasurer = rememberTextMeasurer(textCount)
    val naturalColumnWidths = List(columns.size) { columnIndex ->
        val titleWidth = textMeasurer.measure(columns[columnIndex], headingStyle).size.width.let {
            LocalDensity.current.run { it.toDp() + cellHorizontalPadding + dividerThickness }
        }
        val maxCellWidth = rows.mapNotNull { row ->
            row.cells.getOrNull(columnIndex)
        }.maxOf { cell ->
            when (cell) {
                is TableCell.RichTableCell -> cell.naturalWidth + cellHorizontalPadding * 2
                is TableCell.TextTableCell -> textMeasurer.measure(cell.text, cellStyle).size.width.let {
                    LocalDensity.current.run { it.toDp() + cellHorizontalPadding * 2 }
                }
            }
        }
        maxOf(titleWidth, maxCellWidth)
    }
    val columnWidths = remember(columns, rows) {
        naturalColumnWidths.map { it + extraSpacing }.toMutableStateList()
    }

    var sortingColumn: SortingColumn? by remember { mutableStateOf(defaultSort) }
    val sortedRows = sortingColumn?.let { sorting ->
        val isNumericSorting = when (val cell = rows.firstOrNull()?.cells?.getOrNull(sorting.index)) {
            is TableCell.RichTableCell, null -> false
            is TableCell.TextTableCell -> cell.sortingAmount != null
        }
        val selector: (TableRow) -> Comparable<*>? = if (isNumericSorting) {
            { row -> (row.cells.getOrNull(sorting.index) as? TableCell.TextTableCell)?.sortingAmount ?: 0.0 }
        } else {
            { row ->
                when (val cell = row.cells.getOrNull(sorting.index)) {
                    is TableCell.RichTableCell -> cell.sortingText
                    is TableCell.TextTableCell -> cell.text
                    null -> ""
                }
            }
        }
        when (sorting.sort) {
            Sort.Ascending -> rows.sortedBy { selector(it) as Comparable<Any>? }
            Sort.Descending -> rows.sortedByDescending { selector(it) as Comparable<Any>? }
        }
    } ?: rows

    BoxWithConstraints {
        Column(
            modifier = modifier,
        ) {
            // Column headers
            val headersPointerInteractionStateHolder = rememberPointerInteractionStateHolder()
            Row(
                modifier = Modifier
                    .pointerInteraction(headersPointerInteractionStateHolder)
                    .width(this@BoxWithConstraints.maxWidth)
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .padding(vertical = rowVerticalPadding),
            ) {
                repeat(columns.size) { columnIndex ->
                    val width = columnWidths[columnIndex]
                    val pointerInteractionStateHolder = rememberPointerInteractionStateHolder()
                    Box(
                        modifier = Modifier
                            .pointerInteraction(pointerInteractionStateHolder)
                            .onClick(
                                onClick = {
                                    val sort = sortingColumn?.takeIf { it.index == columnIndex }?.let {
                                        when (it.sort) {
                                            Sort.Ascending -> Sort.Descending
                                            Sort.Descending -> Sort.Ascending
                                        }
                                    } ?: Sort.Ascending
                                    sortingColumn = SortingColumn(columnIndex, sort)
                                },
                                onDoubleClick = {
                                    columnWidths[columnIndex] = naturalColumnWidths[columnIndex]
                                },
                            )
                            .height(IntrinsicSize.Min)
                            .width(width)
                            .padding(start = cellHorizontalPadding),
                    ) {
                        val isSortingThisColumn = sortingColumn?.index == columnIndex
                        val isTooSmall = width < naturalColumnWidths[columnIndex]

                        val textColor by animateColorAsState(if (pointerInteractionStateHolder.isHovered) headingStyle.copy(color = RiftTheme.colors.textHighlighted).color else headingStyle.color)
                        Text(
                            text = columns[columnIndex],
                            style = headingStyle.copy(color = textColor),
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false,
                            modifier = Modifier
                                .modifyIf(isSortingThisColumn) {
                                    padding(end = 16.dp)
                                }
                                .fillMaxWidth()
                                .clipToBounds()
                                .modifyIf(isTooSmall || isSortingThisColumn) {
                                    fadingRightEdge()
                                },
                        )

                        if (isSortingThisColumn) {
                            sortingColumn?.let {
                                val icon = when (it.sort) {
                                    Sort.Ascending -> Res.drawable.table_sort_ascending
                                    Sort.Descending -> Res.drawable.table_sort_descending
                                }
                                Image(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(16.dp),
                                )
                            }
                        }

                        val alpha by animateFloatAsState(if (headersPointerInteractionStateHolder.isHovered) 1f else 0f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                        ColumnDividerHandle(
                            dividerThickness = dividerThickness,
                            modifier = Modifier.alpha(alpha),
                            onDragDelta = { dp ->
                                val newWidth = (columnWidths[columnIndex] + dp).coerceAtLeast(minColumnWidth)
                                columnWidths[columnIndex] = newWidth
                            },
                        )
                    }
                }
            }

            // Rows
            var selectedRowId: String? by remember { mutableStateOf(null) }
            for (row in sortedRows) {
                ClickableCharacter(row.characterId) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .hoverBackground(isSelected = row.id == selectedRowId)
                            .onClick {
                                selectedRowId = row.id
                            }
                            .width(this@BoxWithConstraints.maxWidth)
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .padding(vertical = rowVerticalPadding),
                    ) {
                        row.cells.forEachIndexed { index, cell ->
                            val width = columnWidths[index]
                            val isRightAligned = (cell as? TableCell.TextTableCell)?.sortingAmount != null
                            Box(
                                contentAlignment = if (isRightAligned) Alignment.CenterEnd else Alignment.CenterStart,
                                modifier = Modifier
                                    .width(width)
                                    .padding(horizontal = cellHorizontalPadding),
                            ) {
                                when (cell) {
                                    is TableCell.RichTableCell -> cell.content()
                                    is TableCell.TextTableCell -> Text(
                                        text = cell.text,
                                        style = cellStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ColumnDividerHandle(
    dividerThickness: Dp,
    modifier: Modifier = Modifier,
    onDragDelta: (Dp) -> Unit,
) {
    val dividerPointerInteractionStateHolder = rememberPointerInteractionStateHolder()
    val density = LocalDensity.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset(3.dp)
            .pointerInteraction(dividerPointerInteractionStateHolder)
            .pointerHoverIcon(PointerIcon(Cursors.dragHorizontal))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dp = with(density) { dragAmount.x.toDp() }
                        onDragDelta(dp)
                    },
                )
            }
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(7.dp)
            .zIndex(1f),
    ) {
        val alpha by animateFloatAsState(if (dividerPointerInteractionStateHolder.isHovered) 0.75f else 0.1f)
        val blur by animateFloatAsState(if (dividerPointerInteractionStateHolder.isHovered) 4f else 0.5f)
        val color by animateColorAsState(if (dividerPointerInteractionStateHolder.isHovered) RiftTheme.colors.primary else RiftTheme.colors.borderGreyLight)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(5.dp)
                .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                .background(color.copy(alpha = alpha)),
        ) {}
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(dividerThickness)
                .background(color),
        ) {}
    }
}
