package net.joeaustin.data

import net.joeaustin.extensions.asMatrix
import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixTests {

    @Test
    fun testIdentity() {
        val matrix = arrayOf(
            arrayOf(1.0, 2.0, 3.0),
            arrayOf(4.0, 5.0, 6.0),
            arrayOf(7.0, 8.0, 9.0)
        ).asMatrix()

        val identity = Matrix.of(
            arrayOf(1.0, 0.0, 0.0),
            arrayOf(0.0, 1.0, 0.0),
            arrayOf(0.0, 0.0, 1.0)
        )

        val multipliedMatrix = matrix * identity
        assertEquals(matrix, multipliedMatrix)
    }

    @Test
    fun testMatrixMultiplication() {
        val lhs = arrayOf(
            arrayOf(1.0, 2.0, 3.0),
            arrayOf(4.0, 5.0, 6.0)
        ).asMatrix()

        val rhs = Matrix.of(
            arrayOf(7.0, 8.0),
            arrayOf(9.0, 10.0),
            arrayOf(11.0, 12.0)
        )

        val expected = Matrix.of(
            arrayOf(58.0, 64.0),
            arrayOf(139.0, 154.0),
        )

        assertEquals(expected, lhs * rhs)
    }

    @Test
    fun testVectorMultiplication() {
        val matrix = arrayOf(
            arrayOf(1.0, 2.0, 3.0),
            arrayOf(4.0, 5.0, 6.0),
            arrayOf(7.0, 8.0, 9.0)
        ).asMatrix()

        val v = VectorN(2.0, 1.0, 3.0)
        val expected = VectorN(13.0, 31.0, 49.0)

        assertEquals(expected, matrix * v)
    }

    @Test
    fun testIdentityConvolution() {
        val matrix = arrayOf(
            arrayOf(1.0, 2.0, 3.0),
            arrayOf(4.0, 5.0, 6.0),
            arrayOf(7.0, 8.0, 9.0)
        ).asMatrix()

        val identity = Matrix.of(
            arrayOf(0.0, 0.0, 0.0),
            arrayOf(0.0, 1.0, 0.0),
            arrayOf(0.0, 0.0, 0.0)
        )

        assertEquals(matrix[1, 1], matrix conv identity, 0.000001)
    }

    @Test
    fun testConvolution() {
        val matrix = arrayOf(
            arrayOf(1.0, 2.0, 3.0),
            arrayOf(4.0, 5.0, 6.0),
            arrayOf(7.0, 8.0, 9.0)
        ).asMatrix()

        val xKernel = Matrix.of(
            arrayOf(-1.0, 0.0, 1.0),
            arrayOf(-2.0, 0.0, 2.0),
            arrayOf(-1.0, 0.0, 1.0)
        )

        val yKernel = xKernel.transpose()

        val expectedX = 8.0
        val expectedY = 24.0
        assertEquals(expectedX, matrix conv xKernel, 0.000001)
        assertEquals(expectedY, matrix conv yKernel, 0.000001)
    }
}