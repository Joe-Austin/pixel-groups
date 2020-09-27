package net.joeaustin.utilities

import net.joeaustin.data.Point
import net.joeaustin.data.with
import org.junit.Assert.assertEquals
import org.junit.Test

class BoundsTest {

    @Test
    fun testBounds() {
        val points = listOf(
            0 with 0,
            0 with 1, 1 with 1, 2 with 1,
            0 with 2, 1 with 2, 2 with 2,
            0 with 3, 1 with 3, 2 with 3
        )

        val expectedPoints = listOf(
            0 with 0,
            0 with 1, 1 with 1, 2 with 1,
            0 with 2, 2 with 2,
            0 with 3, 1 with 3, 2 with 3
        )

        val bounds = points.bounds()

        assertEquals(expectedPoints.size, bounds.size)
        assert(bounds.containsAll(expectedPoints))
    }

    @Test
    fun testBoundsWithSingleElement() {
        val points = listOf(0 with 0)
        val bounds = points.bounds()

        assertEquals(points.size, bounds.size)
        assert(bounds.containsAll(points))

    }

    @Test
    fun testAllEdges() {
        val points = listOf(
            Point(x = 10, y = 0),
            Point(x = 10, y = 1),
            Point(x = 10, y = 2),
            Point(x = 11, y = 0),
            Point(x = 11, y = 1),
            Point(x = 11, y = 2),
            Point(x = 11, y = 3),
            Point(x = 11, y = 4)
        )

        val bounds = points.bounds()

        assertEquals(points.size, bounds.size)
        assert(bounds.containsAll(points))
    }

}