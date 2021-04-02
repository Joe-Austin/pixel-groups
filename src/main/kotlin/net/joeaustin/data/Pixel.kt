package net.joeaustin.data

import kotlin.math.*

private const val WEIGHT_RED = .3
private const val WEIGHT_GREEN = .59
private const val WEIGHT_BLUE = .11
private const val GAMMA = 0.04045

data class Pixel(val a: Int, val r: Int, val g: Int, val b: Int) {

    val luminosity: Double by lazy { computeLuminosity() }

    fun toRgbVector(): VectorN {
        return VectorN(r.toDouble(), g.toDouble(), b.toDouble())
    }


    fun toInt(): Int {
        var value = 0
        value = value or (a shl 24)
        value = value or (r shl 16)
        value = value or (g shl 8)
        value = value or (b shl 0)

        return value
    }

    fun toHsl(): VectorN {
        val alpha = a / 255.0
        val red = (r * alpha) / 255.0
        val green = (g * alpha) / 255.0
        val blue = (b * alpha) / 255.0

        val min = min(red, min(green, blue))
        val max = max(red, max(green, blue))

        val l = (max + min) / 2.0
        val s: Double
        var h: Double

        if (max == min) {
            h = 0.0
            s = 0.0
        } else {
            val d = max - min
            s = if (l > 0.5) d / (2 - max - min) else d / (max + min)

            h = when {
                red == max -> {
                    val o = if (green < blue) 6.0 else 0.0
                    (green - blue) / d + o
                }
                green == max -> (blue - red) / d + 2.0
                else -> (red - green) / d + 4.0
            }

            h /= 6.0
        }

        return VectorN(h, s, l)
    }

    fun toXyz(): VectorN {
        val alpha = a / 255.0
        val red = (r * alpha) / 255.0
        val green = (g * alpha) / 255.0
        val blue = (b * alpha) / 255.0

        val sRgb = VectorN(
            linearizeRgbValue(red),
            linearizeRgbValue(green),
            linearizeRgbValue(blue)
        )

        val cieMatrix = Matrix.of(
            arrayOf(0.4124564, 0.3575761, 0.1804375),
            arrayOf(0.2126729, 0.7151522, 0.0721750),
            arrayOf(0.0193339, 0.1191920, 0.9503041)
        )

        return cieMatrix * sRgb
    }

    private fun linearizeRgbValue(value: Double): Double {
        return when {
            value > GAMMA -> ((value + 0.055) / 1.055).pow(2.4)
            else -> value / 12.92
        }
    }

    fun toHxHySL(): VectorN {
        val hsl = toHsl()
        val h = hsl[0]
        val s = hsl[1]
        val l = hsl[2]

        val hDegrees = Math.toRadians(h * 360.0)
        val hx = sin(hDegrees)
        val hy = cos(hDegrees)

        return VectorN(hx, hy, s, l)
    }

    private fun computeLuminosity(): Double {
        val rValue = WEIGHT_RED * r
        val gValue = WEIGHT_GREEN * g
        val bValue = WEIGHT_BLUE * b

        return rValue + gValue + bValue
    }

    companion object {
        fun fromInt(color: Int): Pixel {
            val a = 0xFF and (color shr 24)
            val r = 0xFF and (color shr 16)
            val g = 0xFF and (color shr 8)
            val b = 0xFF and (color shr 0)

            return Pixel(a, r, g, b)
        }

        fun fromHxHySL(hxhyslVector: VectorN): Pixel {
            val hx = hxhyslVector[0]
            val hy = hxhyslVector[1]
            val s = hxhyslVector[2]
            val l = hxhyslVector[3]

            //val degrees = Math.toDegrees(atan2(hy, hx)) / 360.0
            val degrees = Math.toDegrees(atan2(hx, hy)) / 360.0

            return fromHsl(VectorN(degrees, s, l))
        }

        fun fromHsl(hslVector: VectorN): Pixel {
            if (hslVector.size < 3) throw IllegalArgumentException("Vector must be at least of size 3, this vector is of size ${hslVector.size}")
            val h = hslVector[0]
            val s = hslVector[1]
            val l = hslVector[2]

            val r: Double
            val g: Double
            val b: Double

            if (s == 0.0) {
                r = l
                g = l
                b = l
            } else {
                val q = if (l < 0.5) l * (1 + s) else l + s - l * s
                val p = 2.0 * l - q

                r = hue2rgb(p, q, h + 1 / 3.0)
                g = hue2rgb(p, q, h)
                b = hue2rgb(p, q, h - 1 / 3.0)
            }

            val red = (r * 255.0).roundToInt()
            val green = (g * 255.0).roundToInt()
            val blue = (b * 255.0).roundToInt()
            return Pixel(255, red, green, blue)
        }

        private fun hue2rgb(p: Double, q: Double, t: Double): Double {
            var mt = t

            if (mt < 0) mt += 1
            if (mt > 1) mt -= 1

            return when {
                mt < 1 / 6.0 -> p + (q - p) * 6.0 * mt
                mt < 1 / 2.0 -> q
                mt < 2 / 3.0 -> p + (q - p) * (2 / 3.0 - mt) * 6.0
                else -> p
            }
        }
    }
}