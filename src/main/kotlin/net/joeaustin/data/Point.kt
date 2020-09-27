package net.joeaustin.data

data class Point(val x: Int, val y: Int)

infix fun Int.with(y: Int): Point {
    return Point(this, y)
}