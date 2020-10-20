package net.joeaustin.extensions

import net.joeaustin.data.Matrix
import net.joeaustin.data.Pixel

fun Array<Array<Double>>.asMatrix(): Matrix {
    return Matrix.fromArrays(this)
}

fun Array<Array<Pixel>>.asRgbMatrices(): Triple<Matrix, Matrix, Matrix> {
    val rows = this.size
    val cols = this[0].size

    val rMatrix = Matrix(rows, cols)
    val gMatrix = Matrix(rows, cols)
    val bMatrix = Matrix(rows, cols)

    this.forEachIndexed { r, pixelRow ->
        pixelRow.forEachIndexed { c, pixel ->
            rMatrix[r, c] = pixel.r.toDouble()
            gMatrix[r, c] = pixel.g.toDouble()
            bMatrix[r, c] = pixel.b.toDouble()
        }
    }

    return Triple(rMatrix, gMatrix, bMatrix)
}

fun Array<Array<Pixel>>.asLuminosityMatrix(): Matrix {
    val rows = this.size
    val cols = this[0].size

    val matrix = Matrix(rows, cols)

    this.forEachIndexed { r, pixelRow ->
        pixelRow.forEachIndexed { c, pixel ->
            matrix[r, c] = pixel.luminosity
        }
    }

    return matrix
}

