package net.joeaustin.data

import org.junit.Assert.assertEquals
import org.junit.Test

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

    @Test
    fun rgbToHxHySLToRgb() {
        val red = Pixel(255, 255, 0, 0)
        val green = Pixel(255, 0, 255, 0)
        val blue = Pixel(255, 0, 0, 255)
        val gray = Pixel(255, 126, 126, 126)

        val redHsl = red.toHxHySL()
        val greenHsl = green.toHxHySL()
        val blueHsl = blue.toHxHySL()
        val grayHsl = gray.toHxHySL()

        val redRgb = Pixel.fromHxHySL(redHsl)
        val greenRgb = Pixel.fromHxHySL(greenHsl)
        val blueRgb = Pixel.fromHxHySL(blueHsl)
        val grayRgb = Pixel.fromHxHySL(grayHsl)

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
        val one = Pixel.fromInt(0xFF1f273c.toInt())
        val two = Pixel.fromInt(0xFF27292f.toInt())
        val three = Pixel.fromInt(0xFF25324f.toInt())

        val oneHsl = one.toHxHySL()
        val twoHsl = two.toHxHySL()
        val threeHsl = three.toHxHySL()

        println(oneHsl[3] * 255)
        println(one.luminosity)
        println("-".repeat(20))

        println(one)
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

        println("CosineDistance from one - two: ${oneHsl.cosineDistance(twoHsl)}")
        println("CosineDistance from one - three: ${oneHsl.cosineDistance(threeHsl)}")

        println("-".repeat(5))

        println("Euclidean Distance from one - two: ${oneHsl.distanceTo(twoHsl)}")
        println("Euclidean Distance from one - three: ${oneHsl.distanceTo(threeHsl)}")

        println("-".repeat(15))

        println(colorOne.cosineDistance(colorTwo))
        println(colorOne.cosineDistance(colorThree))

    }

    @Test
    fun testHslAndRgb() {
        //val one = Pixel(255, 127, 255, 0)
        val one = Pixel.fromInt(0xFF9a9397.toInt())
        val two = Pixel.fromInt(0xFFc9c2c1.toInt())
        val three = Pixel.fromInt(0xFFa8a5a4.toInt())

        val colorOne = one.toRgbVector() / 255.0
        val colorTwo = two.toRgbVector() / 255.0
        val colorThree = three.toRgbVector() / 255.0

        val oneHsl = one.toHxHySL().append(*colorOne.elements)
        val twoHsl = two.toHxHySL().append(*colorTwo.elements)
        val threeHsl = three.toHxHySL().append(*colorThree.elements)

        println(oneHsl[3] * 255)
        println(one.luminosity)
        println("-".repeat(20))

        println(one)
        println(oneHsl)
        println(twoHsl)
        println(threeHsl)
        println("-".repeat(20))
        println()



        println("CosineDistance from one -> two: ${oneHsl.cosineDistance(twoHsl)}")
        println("CosineDistance from one -> three: ${oneHsl.cosineDistance(threeHsl)}")

        println("-".repeat(5))

        println("Euclidean Distance from one -> two: ${oneHsl.distanceTo(twoHsl)}")
        println("Euclidean Distance from one -> three: ${oneHsl.distanceTo(threeHsl)}")

        println("-".repeat(15))

        println(colorOne.cosineDistance(colorTwo))
        println(colorOne.cosineDistance(colorThree))

    }

    @Test
    fun testAverages() {
        val one = Pixel.fromInt(0xFFFF0000.toInt())
        val two = Pixel.fromInt(0xFF695848.toInt())
        val three = Pixel.fromInt(0xFF211C19.toInt())

        val averageRgb = (one.toRgbVector() + two.toRgbVector() + three.toRgbVector()) / 3.0
        println(averageRgb)

        val averageHsl = (one.toHxHySL() + two.toHxHySL() + three.toHxHySL()) / 3.0
        println(averageHsl)
        val pixel = Pixel.fromHxHySL(averageHsl)
        println("${Pixel.fromHxHySL(averageHsl)} - 0x${pixel.toInt().toUInt().toString(16)}")
        println("Hue: ${pixel.toHsl()[0] * 360}")


    }

}