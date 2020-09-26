package net.joeaustin.data

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos

class PixelTests {

    @Test
    fun rgbToHslToRgb() {
        val red = Pixel(255, 255, 0, 0)
        val green = Pixel(255, 0, 255, 0)
        val blue = Pixel(255, 0, 0, 255)
        val gray = Pixel(255, 126, 126, 126)

        val redHsl = red.toHsl()
        val greenHsl = green.toHsl()
        val blueHsl = blue.toHsl()
        val grayHsl = gray.toHsl()

        val redRgb = Pixel.fromHsl(redHsl)
        val greenRgb = Pixel.fromHsl(greenHsl)
        val blueRgb = Pixel.fromHsl(blueHsl)
        val grayRgb = Pixel.fromHsl(grayHsl)

        assertEquals("Red", red, redRgb)
        assertEquals("Green", green, greenRgb)
        assertEquals("Blue", blue, blueRgb)
        assertEquals("Gray", gray, grayRgb)
    }

    //TODO: Move to VectorN Test Class

    @Test
    fun appendTest() {
        val initial = VectorN(0.0, 1.0, 2.0, 3.0)
        val grown = initial.append(4.0, 5.0, 6.0)

        val expected = VectorN(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        assertEquals(expected, grown)
    }

    @Test
    fun removalTest() {
        val initial = VectorN(0.0, 1.0, 2.0, 3.0)
        val expected = VectorN(0.0, 2.0, 3.0)
        assertEquals(expected, initial.remove(1))
    }

    @Test
    fun insertionTest() {
        val initial = VectorN(0.0, 1.0, 4.0, 5.0, 6.0)
        val expected = VectorN(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        assertEquals(expected, initial.insert(2, 2.0, 3.0))
    }

    @Test
    fun test() {
        //val one = Pixel(255, 127, 255, 0)
        val one = Pixel.fromInt(0xFF80807B.toInt())
        val two = Pixel.fromInt(0xFF695848.toInt())
        val three = Pixel.fromInt(0xFF211C19.toInt())

        println(three)

        val oneHsl = one.toHxHyLV()
        val twoHsl = two.toHxHyLV()
        val threeHsl = three.toHxHyLV()

        println(oneHsl)
        println(twoHsl)
        println(threeHsl)
        println("-".repeat(20))
        println()

        //oneHsl[0] = cos(Math.toRadians(oneHsl[0] * 360.0))
        //twoHsl[0] = cos(Math.toRadians(twoHsl[0] * 360.0))
        //threeHsl[0] = cos(Math.toRadians(threeHsl[0] * 360.0))

        val colorOne = one.toRgbVector()
        val colorTwo = two.toRgbVector()
        val colorThree = three.toRgbVector()

        println(oneHsl.cosineDistance(twoHsl))
        println(oneHsl.cosineDistance(threeHsl))

        println("-".repeat(15))

        println(colorOne.cosineDistance(colorTwo))
        println(colorOne.cosineDistance(colorThree))

    }

}