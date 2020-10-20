package net.joeaustin.data

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.joeaustin.exceptions.IncompatibleSizeException

class Matrix {
    val rows: Int
    val cols: Int
    val array: DoubleArray

    constructor(rows: Int, cols: Int) {
        array = DoubleArray(rows * cols)
        this.rows = rows
        this.cols = cols
    }


    private constructor(sourceArray: Array<Array<Double>>) {
        array = sourceArray.flatten().toDoubleArray()
        rows = sourceArray.size
        cols = sourceArray[0].size
    }

    fun transpose(): Matrix {
        val outMatrix = Matrix(cols, rows)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                outMatrix[c, r] = this[r, c]
            }
        }

        return outMatrix
    }

    operator fun get(r: Int, c: Int): Double {
        return array[computeFlatIndex(r, c)]
    }

    operator fun set(r: Int, c: Int, value: Double) {
        array[computeFlatIndex(r, c)] = value
    }

    fun getRow(r: Int): VectorN {
        val startIndex = computeFlatIndex(r, 0)
        val stopIndex = computeFlatIndex(r, cols - 1)
        return VectorN(*array.sliceArray(startIndex..stopIndex))
    }

    fun getColumn(c: Int): VectorN {
        val outArray = DoubleArray(rows)
        for (r in 0 until rows) {
            outArray[r] = this[r, c]
        }

        return VectorN(*outArray)
    }

    private fun computeFlatIndex(r: Int, c: Int): Int {
        return cols * r + c
    }

    operator fun times(vectorN: VectorN): VectorN {
        if (this.rows != vectorN.size) throw IncompatibleSizeException("Matrix has $rows rows and vector is of size ${vectorN.size}")
        val outArray = DoubleArray(vectorN.size)

        runBlocking {
            for (i in 0 until rows) {
                launch {
                    val rowVector = getRow(i)
                    outArray[i] = rowVector dot vectorN
                }
            }
        }

        return VectorN(*outArray)
    }

    operator fun times(other: Matrix): Matrix {
        if (this.rows != other.cols) throw IncompatibleSizeException("Left Matrix has $rows rows and right matrix hax ${other.cols} columns")
        val outMatrix = Matrix(this.rows, other.cols)

        runBlocking {
            for (r in 0 until rows) {
                for (c in 0 until other.cols) {
                    launch {
                        val rowVector = getRow(r)
                        val colVector = other.getColumn(c)
                        outMatrix[r, c] = rowVector dot colVector
                    }
                }
            }
        }
        return outMatrix
    }

    override fun toString(): String {
        val sb = StringBuilder("[")

        for (r in 0 until rows) {
            sb.append(getRow(r).toString())
            if (r < rows - 1) {
                sb.appendLine(",")
            } else {
                sb.append("]")
            }
        }

        return sb.toString()
    }

    override fun hashCode(): Int {
        return array.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Matrix) {
            this.array.contentEquals(other.array)
        } else {
            false
        }
    }

    companion object {
        fun fromArrays(arrays: Array<Array<Double>>): Matrix {
            val expectedCols = arrays[0].size
            val sizeOk = arrays.all { it.size == expectedCols }
            if (!sizeOk) throw IllegalArgumentException("All rows must have the same number of columns")
            return Matrix(arrays)
        }

        fun of(vararg rows: Array<Double>): Matrix {
            return fromArrays(rows as Array<Array<Double>>)
        }
    }
}

infix fun Matrix.conv(other: Matrix): Double {
    if (this.rows != other.rows) throw IncompatibleSizeException("Left Matrix has $rows rows and right matrix has ${other.rows}")
    if (this.cols != other.cols) throw IncompatibleSizeException("Left Matrix has $cols columns and right matrix has ${other.cols}")

    var sum = 0.0

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            sum += this[r, c] * other[r, c]
        }
    }

    return sum
}