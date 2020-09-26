package net.joeaustin.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class VectorN(vararg val elements: Double) {

    val size = elements.size

    fun insert(index: Int, vararg values: Double): VectorN {
        val newSize = size + values.size
        val pickupIndex = index + values.size

        val array = DoubleArray(newSize) { i ->
            when {
                i < index -> {
                    this[i]
                }
                i < pickupIndex -> {
                    values[i - index]
                }
                else -> {
                    this[i - values.size]
                }
            }
        }

        return VectorN(*array)
    }

    fun append(vararg values: Double): VectorN {
        val growBy = values.size
        val array = DoubleArray(size + growBy) { i ->
            if (i >= size) values[i - size] else this[i]
        }

        return VectorN(*array)
    }

    fun remove(index: Int): VectorN {
        val removeCount = if (index in 0 until size) 1 else 0
        val array = DoubleArray(size - removeCount) { i ->
            if (i < index) this[i] else this[i + 1]
        }

        return VectorN(*array)
    }

    fun removeFirst(): VectorN = remove(0)

    fun removeLast(): VectorN {
        return remove(size - 1)
    }

    fun cosineDistance(other: VectorN): Double {
        val d = this.magnitude() * other.magnitude()
        return if (d != 0.0) {
            (this dot other) / d
        } else {
            throw UnsupportedOperationException("Cannot perform cosine similarity on a zero vector")
        }
    }

    fun magnitude(): Double {
        return sqrt(this dot this)
    }

    operator fun plus(other: VectorN): VectorN {
        val largeVector = if (this.size >= other.size) this else other
        val returnSize = max(this.size, other.size)
        val returnArray = DoubleArray(returnSize) { i -> largeVector[i] }
        val minSize = min(this.size, other.size)

        for (i in 0 until minSize) {
            returnArray[i] = this[i] + other[i]
        }
        return VectorN(*returnArray)
    }

    operator fun minus(other: VectorN): VectorN {
        //We'll track a multiplier to fill the difference, if any since subtraction is not commutative
        val (largeVector, m) = if (this.size >= other.size) {
            this to 1.0
        } else {
            other to -1.0
        }

        val returnSize = max(this.size, other.size)
        val returnArray = DoubleArray(returnSize) { i -> largeVector[i] * m }
        val minSize = min(this.size, other.size)

        for (i in 0 until minSize) {
            returnArray[i] = this[i] - other[i]
        }
        return VectorN(*returnArray)
    }

    operator fun times(other: VectorN): VectorN {
        val returnSize = max(this.size, other.size)
        val returnArray = DoubleArray(returnSize) { 0.0 }
        val minSize = min(this.size, other.size)

        for (i in 0 until minSize) {
            returnArray[i] = this[i] * other[i]
        }
        return VectorN(*returnArray)
    }

    infix fun dot(other: VectorN): Double {
        val minSize = min(this.size, other.size)
        var value = 0.0

        for (i in 0 until minSize) {
            value += this[i] * other[i]
        }

        return value
    }

    operator fun get(index: Int): Double {
        return this.elements[index]
    }

    operator fun set(index: Int, value: Double) {
        this.elements[index] = value
    }

    override fun toString(): String {
        return elements.joinToString(prefix = "[", postfix = "]")
    }

    override fun equals(other: Any?): Boolean {
        return if (other is VectorN) {
            this.elements.contentEquals(other.elements)
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return elements.hashCode()
    }

}