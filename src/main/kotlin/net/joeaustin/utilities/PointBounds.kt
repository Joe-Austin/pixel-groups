package net.joeaustin.utilities

import net.joeaustin.data.Point

fun List<Point>.bounds(): List<Point> {
    if (this.isEmpty()) return emptyList()

    val rows = this.groupBy { (_, y) -> y }
    val cols = this.groupBy { (x, _) -> x }

    val rowsForColumn = cols.toList().map { (col, point) ->
        point.filter { (x, _) -> x == col }
    }

    val colsForRow = rows.toList().map { (row, point) ->
        point.filter { (_, y) -> y == row }
    }

    val minRowsForCol = rowsForColumn.map { it.minByOrNull { (_, y) -> y } ?: throw RuntimeException("WTF") }
        .groupBy { it.x }
        .mapValues { v -> v.value.map { it.y } }
    val maxRowsForCol = rowsForColumn.map { it.maxByOrNull { (_, y) -> y } ?: throw RuntimeException("WTF") }
        .groupBy { it.x }
        .mapValues { v -> v.value.map { it.y } }


    val minColsForRow = colsForRow.map { it.minByOrNull { (x, _) -> x } ?: throw RuntimeException("WTF") }
        .groupBy { it.y }
        .mapValues { v -> v.value.map { it.x } }
    val maxColsForRow = colsForRow.map { it.maxByOrNull { (x, _) -> x } ?: throw RuntimeException("WTF") }
        .groupBy { it.y }
        .mapValues { v -> v.value.map { it.x } }

    return this.filter { (x, y) ->
        //val isVerticalExtent = minRowsForCol[x].y == y || maxRowsForCol[x].y == y
        //val isHorizontalExtent = minColsForRow[y].x == x || maxColsForRow[y].x == x
        val isVerticalExtent = minRowsForCol[x]?.contains(y) == true || maxRowsForCol[x]?.contains(y) == true
        val isHorizontalExtent = minColsForRow[y]?.contains(x) == true || maxColsForRow[y]?.contains(x) == true

        isHorizontalExtent || isVerticalExtent
    }.distinct()
}