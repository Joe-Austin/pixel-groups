package net.joeaustin.data

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.*

class PixelTests {

    @Test
    fun sandox() {
        //8B9E07
        //33CA39
        //00E600
        val p1 = Pixel.fromInt(0xFF4B9671.toInt())
        val p2 = Pixel.fromInt(0xFF33CA39.toInt())
        val p3 = Pixel.fromInt(0xFF04E600.toInt())

        println(p1)
        println(p2)
        println(p3)


        val a = Pixel.fromInt(0xFF4B9671.toInt()).toRgbVector()
        val b = Pixel.fromInt(0xFF33CA39.toInt()).toRgbVector()
        val c = Pixel.fromInt(0xFF04E600.toInt()).toRgbVector()
        //val c = VectorN(158.18364524085416, 158.18364524085416, 57.0)
        //val c = VectorN(139.0, 158.0, 57.0)

        val diff = b - c
        val angle = -90.0
        val rotationMatrix = Matrix.of(
            arrayOf(cos(angle), -sin(angle), 0.0),
            arrayOf(cos(angle), -sin(angle), 0.0),
            arrayOf(0.0, 0.0, 1.0),
        )

        val rotatedVector = rotationMatrix * b
        //val rotatedVector = rotationMatrix * b
        //val translatedVector = b + rotatedVector

        println("Rotated Vector: $rotatedVector")
        println("Diff From Rotated Vector: ${b.distanceTo(rotatedVector)}")


        println("B -> A ${b.distanceTo(a)}")
        println("B -> C ${b.distanceTo(c)}")
        println("B - C ${b - c}")
    }

    @Test
    fun hslSandbox() {

        // Opposite Colors
        val pa = Pixel(255, 0, 0, 1)
        val pb = Pixel(255, 126, 126, 126)
        val pc = Pixel(255, 255, 255, 255)

        /*// Opposite Colors
        val pa = Pixel(255, 0, 255, 0)
        val pb = Pixel(255, 255, 0, 0)
        val pc = Pixel(255, 0, 0, 255)*/

        /*// Green Colors
        val pa = Pixel(255, 75, 150, 113)
        val pb = Pixel(255, 50, 202, 57)
        val pc = Pixel(255, 0, 230, 0)*/


        /*//Red Colors
        val pa = Pixel.fromInt(0xFFFF0008.toInt())
        val pb = Pixel.fromInt(0xFFFF0000.toInt())
        val pc = Pixel.fromInt(0xFFFF0800.toInt())*/

        /*//Cat Colors A,B
        val pa = Pixel.fromInt(0xFF3E4662.toInt())
        val pb = Pixel.fromInt(0xFF090E2A.toInt())
        val pc = Pixel.fromInt(0xFFFFFFFF.toInt())*/

        //Cat Colors C,D
        /*val pa = Pixel.fromInt(0xFFAA7740.toInt())
        val pb = Pixel.fromInt(0xFFE2C09C.toInt())
        val pc = Pixel.fromInt(0xFFFFFFFF.toInt())*/


        val aHxHySL = pa.toHxHySL()
        val bHxHySL = pb.toHxHySL()
        val cHxHySL = pc.toHxHySL()

        val aXyz = pa.toXyz()
        val bXyz = pb.toXyz()
        val cXyz = pc.toXyz()

        val aHsl = pa.toHsl()
        val bHsl = pb.toHsl()
        val cHsl = pc.toHsl()

        val aRgb = pa.toRgbVector()
        val bRgb = pb.toRgbVector()
        val cRgb = pc.toRgbVector()

        //val c = VectorN(158.18364524085416, 158.18364524085416, 57.0)
        //val c = Pixel(255, 139, 158, 57).toHxHySL()


        println("-------- HSL --------")
        println("B -> $bHsl")
        println("A -> $aHsl")
        println("C -> $cHsl")
        println("CD B -> A ${bHsl.cosineDistance(aHsl)} | ED B-> A ${bHsl.distanceTo(aHsl)}")
        println("CD B -> C ${bHsl.cosineDistance(cHsl)} | ED B-> C ${bHsl.distanceTo(cHsl)}")

        println()
        println("-------- HxHySL --------")
        println("B -> $bHxHySL")
        println("A -> $aHxHySL")
        println("C -> $cHxHySL")
        println("CD B -> A ${bHxHySL.cosineDistance(aHxHySL)} | ED B-> A ${bHxHySL.distanceTo(aHxHySL)}")
        println("CD B -> C ${bHxHySL.cosineDistance(cHxHySL)} | ED B-> C ${bHxHySL.distanceTo(cHxHySL)}")

        println()
        println("-------- XYZ --------")
        println("B -> $bXyz")
        println("A -> $aXyz")
        println("C -> $cXyz")
        println("CD B -> A ${bXyz.cosineDistance(aXyz)} | ED B-> A ${bXyz.distanceTo(aXyz)}")
        println("CD B -> C ${bXyz.cosineDistance(cXyz)} | ED B-> C ${bXyz.distanceTo(cXyz)}")

        println()
        println("-------- RGB --------")
        println("B -> $bRgb")
        println("A -> $aRgb")
        println("C -> $cRgb")
        println("CD B -> A ${bRgb.cosineDistance(aRgb)} | ED B-> A ${bRgb.distanceTo(aRgb)}")
        println("CD B -> C ${bRgb.cosineDistance(cRgb)} | ED B-> C ${bRgb.distanceTo(cRgb)}")
    }

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
    fun testColinearity() {
        val one = Pixel.fromInt(0xFFa78b7a.toInt())
        val two = Pixel.fromInt(0xFFa78b7a.toInt())

        val oneHsl = one.toHxHySL()
        val twoHsl = two.toHxHySL()

        println(oneHsl.cosineDistance(twoHsl))

        val oneXY = oneHsl.append(0.0, 0.0)
        val twoXY = twoHsl.append(0.0, 0.3)

        println(oneXY.cosineDistance(twoXY))
    }

    @Test
    fun test() {
        //val one = Pixel(255, 127, 255, 0)
        val one = Pixel.fromInt(0xFFa78b7a.toInt())
        val two = Pixel.fromInt(0xFF6f59ae.toInt())
        val three = Pixel.fromInt(0xFF6f59ae.toInt())

        val oneHsl = one.toHxHySL()
        val twoHsl = two.toHxHySL()
        val threeHsl = three.toHxHySL()

        println("Distance between one and two ${oneHsl.cosineDistance(twoHsl)}")

        val oneScaled = oneHsl * 20.0
        val twoScaled = twoHsl * 20.0

        println("Distance between one and two scaled ${oneScaled.cosineDistance(twoHsl / 2.0)}")


        println("Distance between one and two ${oneHsl.cosineDistance(twoHsl)}")
        println("Distance between one and three ${oneHsl.cosineDistance(threeHsl)}")

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
        val one = Pixel.fromInt(0xFFbb3513.toInt())
        val two = Pixel.fromInt(0xFFc36b34.toInt())
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

    @Test
    fun testColorEdges() {

        /*val colorValues = arrayOf(
            arrayOf(0xFF346f8b, 0xFF102a34, 0xFF16303a),
            arrayOf(0xFF316784, 0xFF122732, 0xFF1b303b),
            arrayOf(0xFF285e7b, 0xFF122732, 0xFF1b303b)
        )*/

        val colorValues = arrayOf(
            arrayOf(0xFFFF0000, 0xFFFF0000, 0xFFFF0000),
            arrayOf(0xFF0000FF, 0xFFFF0000, 0xFFFF0000),
            arrayOf(0xFF0000FF, 0xFF0000FF, 0xFFFF0000)
        )

        //NG
        val group = colorValues.map { it.map { v -> Pixel.fromInt(v.toInt()) } }

        val center = group[1][1].toHxHySL()

        val diffGroup = group.map { row -> row.map { p -> p.toHxHySL().cosineDistance(center) } }
        //println(diffGroup)


        val xKernel = arrayOf(
            arrayOf(1.0, 0.0, -1.0),
            arrayOf(2.0, 0.0, -2.0),
            arrayOf(1.0, 0.0, -1.0),
        )

        val yKernel = arrayOf(
            arrayOf(1.0, 2.0, 1.0),
            arrayOf(0.0, 0.0, 0.0),
            arrayOf(-1.0, -2.0, -1.0),
        )

        diffGroup.forEach { println(it) }

        var xGradient = 0.0
        var yGradient = 0.0

        for (y in 0..2) {
            for (x in 0..2) {
                xGradient += xKernel[y][x] * diffGroup[y][x]
                yGradient += yKernel[y][x] * diffGroup[y][x]
            }
        }

        println(xGradient)
        println(yGradient)

        val magnitude = sqrt(xGradient.pow(2) + yGradient.pow(2))
        val angle = Math.toDegrees(atan(yGradient / xGradient))

        println("Mag: $magnitude")
        println("Theta: $angle")

    }

    @Test
    fun testColorNonEdges() {

        val colorValues = arrayOf(
            arrayOf(0xFFd0a785, 0xFFd1a886, 0xFFd0aa87),
            arrayOf(0xFFcfa684, 0xFFd1a886, 0xFFd0aa87),
            arrayOf(0xFFd0a785, 0xFFd2a987, 0xFFd0aa87)
        )

        /*val colorValues = arrayOf(
            arrayOf(0xFF346f8b, 0xFF102a34, 0xFF16303a),
            arrayOf(0xFF316784, 0xFF122732, 0xFF1b303b),
            arrayOf(0xFF285e7b, 0xFF122732, 0xFF1b303b)
        )*/

        //NG
        val group = colorValues.map { it.map { v -> Pixel.fromInt(v.toInt()) } }
        /*val group = arrayOf(
            arrayOf(0.0, 0.0, 0.0),
            arrayOf(0xFFFFFFFF.toDouble(), 0xFFFFFFFF.toDouble(), 0xFFFFFFFF.toDouble()),
            arrayOf(0xFFFFFFFF.toDouble(), 0xFFFFFFFF.toDouble(), 0xFFFFFFFF.toDouble()),
        )*/

        val center = group[1][1].toHxHySL()

        val diffGroup = group.map { row -> row.map { p -> p.toHxHySL().cosineDistance(center) } }
        val lightness = group.map { row -> row.map { p -> p.toHsl()[2] } }


        val xKernel = arrayOf(
            arrayOf(1.0, 0.0, -1.0),
            arrayOf(2.0, 0.0, -2.0),
            arrayOf(1.0, 0.0, -1.0),
        )

        val yKernel = arrayOf(
            arrayOf(1.0, 2.0, 1.0),
            arrayOf(0.0, 0.0, 0.0),
            arrayOf(-1.0, -2.0, -1.0),
        )

        println("Diff Group")
        diffGroup.forEach { println(it) }
        println("Lightness")
        lightness.forEach { println(it) }

        var xGradient = 0.0
        var yGradient = 0.0

        var xlGradient = 0.0
        var ylGradient = 0.0

        for (y in 0..2) {
            for (x in 0..2) {
                xGradient += xKernel[y][x] * diffGroup[y][x]
                yGradient += yKernel[y][x] * diffGroup[y][x]

                xlGradient += xKernel[y][x] * lightness[y][x]
                ylGradient += yKernel[y][x] * lightness[y][x]
            }
        }

        println(xGradient)
        println(yGradient)
        println()
        println(xlGradient)
        println(ylGradient)

        val magnitude = sqrt(xGradient.pow(2) + yGradient.pow(2))
        val angle = Math.toDegrees(atan(yGradient / xGradient))

        val lMagnitude = sqrt(xlGradient.pow(2) + ylGradient.pow(2))
        val lAngle = Math.toDegrees(atan(ylGradient / xlGradient))

        println("Mag: $magnitude")
        println("Theta: $angle")

        println("LMag: $lMagnitude")
        println("LTheta: $lAngle")

    }


    @Test
    fun testColorGradients() {

        val colorValues = arrayOf(
            arrayOf(0xFF000000, 0xFF000000, 0xFFFFFFFF),
            arrayOf(0xFF000000, 0xFF000000, 0xFFFFFFFF),
            arrayOf(0xFF000000, 0xFF000000, 0xFFFFFFFF)
        )

        /*val colorValues = arrayOf(
            arrayOf(0xFFd0a785, 0xFFd1a886, 0xFFd0aa87),
            arrayOf(0xFFcfa684, 0xFFd1a886, 0xFFd0aa87),
            arrayOf(0xFFd0a785, 0xFFd2a987, 0xFFd0aa87)
        )*/

        /*val colorValues = arrayOf(
            arrayOf(0xFF346f8b, 0xFF102a34, 0xFF16303a),
            arrayOf(0xFF316784, 0xFF122732, 0xFF1b303b),
            arrayOf(0xFF285e7b, 0xFF122732, 0xFF1b303b)
        )*/

        //NG
        val group = colorValues.map { it.map { v -> Pixel.fromInt(v.toInt()) } }
        val vectors = group.map { it.map { p -> p.toHxHySL() } }


        val xVector = VectorN(
            vectors[0][0].cosineDistance(vectors[0][2]),
            vectors[1][0].cosineDistance(vectors[1][2]) * 2,
            vectors[2][0].cosineDistance(vectors[2][2])
        )

        println(xVector.toString())

        val yVector = VectorN(
            vectors[0][0].cosineDistance(vectors[2][0]),
            vectors[0][1].cosineDistance(vectors[2][1]) * 2,
            vectors[0][2].cosineDistance(vectors[2][2])
        )

        val xGradient = xVector.magnitude()
        val yGradient = yVector.magnitude()
        val magnitude = sqrt(xGradient + yGradient)
        val angle = Math.toDegrees(atan(yGradient / xGradient))

        println("Mag: $magnitude")
        println("Theta: $angle")

    }


    @Test
    fun testLuminosity() {

        /*val colorValues = arrayOf(
            arrayOf(0xFF0000FF, 0xFFFF0000, 0xFFFF0000),
            arrayOf(0xFF0000FF, 0xFF0000FF, 0xFFFF0000),
            arrayOf(0xFF0000FF, 0xFF0000FF, 0xFF0000FF)
        )*/
/*
EDGE
        val colorValues = arrayOf(
            arrayOf(0xFFc69c82, 0xFFc59681, 0xFFc3997f),
            arrayOf(0xFFc69c82, 0xFFc59681, 0xFFc1977d),
            arrayOf(0xFFc3997f, 0xFF122732, 0xFFc1977d)
        )*/


        val colorValues = arrayOf(
            arrayOf(0xFFC79087, 0xFFc89e84, 0xFFc89e84),
            arrayOf(0xFFC79087, 0xFFc99f85, 0xFFc89e84),
            arrayOf(0xFFC79087, 0xFFc99f85, 0xFFc89e84)
        )

        //NG
        val group = colorValues.map { it.map { v -> Pixel.fromInt(v.toInt()) } }
        /*val group = arrayOf(
            arrayOf(0.0, 0.0, 0.0),
            arrayOf(0xFFFFFFFF.toDouble(), 0xFFFFFFFF.toDouble(), 0xFFFFFFFF.toDouble()),
            arrayOf(0xFFFFFFFF.toDouble(), 0xFFFFFFFF.toDouble(), 0xFFFFFFFF.toDouble()),
        )*/

        val center = group[1][1].toHxHySL()

        val diffGroup = group.map { row -> row.map { p -> p.toHxHySL().cosineDistance(center) } }
        val lightness = group.map { row -> row.map { p -> p.luminosity } }


        val xKernel = arrayOf(
            arrayOf(1.0, 0.0, -1.0),
            arrayOf(2.0, 0.0, -2.0),
            arrayOf(1.0, 0.0, -1.0),
        )

        val yKernel = arrayOf(
            arrayOf(1.0, 2.0, 1.0),
            arrayOf(0.0, 0.0, 0.0),
            arrayOf(-1.0, -2.0, -1.0),
        )

        println("Diff Group")
        diffGroup.forEach { println(it) }
        println("Lightness")
        lightness.forEach { println(it) }

        var xGradient = 0.0
        var yGradient = 0.0

        var xlGradient = 0.0
        var ylGradient = 0.0

        for (y in 0..2) {
            for (x in 0..2) {
                xGradient += xKernel[y][x] * diffGroup[y][x]
                yGradient += yKernel[y][x] * diffGroup[y][x]

                xlGradient += xKernel[y][x] * lightness[y][x]
                ylGradient += yKernel[y][x] * lightness[y][x]
            }
        }

        println(xGradient)
        println(yGradient)
        println()
        println(xlGradient)
        println(ylGradient)

        val magnitude = sqrt(xGradient.pow(2) + yGradient.pow(2))
        val angle = Math.toDegrees(atan(yGradient / xGradient))

        val lMagnitude = sqrt(xlGradient.pow(2) + ylGradient.pow(2))
        val lAngle = Math.toDegrees(atan(ylGradient / xlGradient))

        println("Mag: $magnitude")
        println("Theta: $angle")

        println("LMag: $lMagnitude")
        println("LTheta: $lAngle")

    }


}