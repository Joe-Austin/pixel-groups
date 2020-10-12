package net.joeaustin.utilities

import net.joeaustin.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun createSuperPixelImage(labels: Array<Array<PixelLabel>>, outputFile: File) {
    //val labelMap = getPixelLabelMap(labels)
    val labelMap = getPixelLabelMapByHueAverage(labels)
    val width = labels.size
    val height = labels[0].size
    val outImage = BufferedImage(width, height, ColorSpace.TYPE_RGB)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val (_, label) = labels[x][y]
            labelMap[label]?.let { outImage.setRGB(x, y, it.toInt()) }
        }
    }

    ImageIO.write(outImage, "PNG", outputFile)

}

fun getPixelLabelMapByHueAverage(labels: Array<Array<PixelLabel>>): Map<Int, Pixel> {
    val pixelMap = HashMap<Int, Pixel>() //Key = Label; Value = Average Pixel Value

    labels.flatten().groupBy { it.label }.forEach { (label, pixelLabels) ->
        //val pixelValues =
        var averageHsl = VectorN(0.0, 0.0, 0.0, 0.0)

        pixelLabels.map { it.pixel.toHxHySL() }.forEach { pixelHsl ->
            averageHsl += pixelHsl
        }

        averageHsl /= pixelLabels.size.toDouble()

        pixelMap[label] = Pixel.fromHxHySL(averageHsl)
    }

    return pixelMap
}

fun getPixelLabelMapByMeanAverage(labels: Array<Array<PixelLabel>>): Map<Int, Pixel> {
    val pixelMap = HashMap<Int, Pixel>() //Key = Label; Value = Average Pixel Value

    labels.flatten().groupBy { it.label }.forEach { (label, pixelLabels) ->
        val size = pixelLabels.size
        val a = 255 //assume no transparency
        val r = pixelLabels.sumBy { it.pixel.r } / size
        val g = pixelLabels.sumBy { it.pixel.g } / size
        val b = pixelLabels.sumBy { it.pixel.b } / size

        pixelMap[label] = Pixel(a, r, g, b)
    }

    return pixelMap
}

fun writeLabelsToFile(
    outputFile: File,
    source: BufferedImage,
    useAverageColor: Boolean,
    labels: Array<Array<PixelLabel>>,
    pixels: List<PixelLabel>
) {
    val labelMap = getPixelLabelMapByHueAverage(labels)
    val width = source.width
    val height = source.height

    val pixelMap = pixels.groupBy { it.point }

    val outImage = BufferedImage(width, height, ColorSpace.TYPE_RGB)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pt = x with y
            pixelMap[pt]?.let { pixels ->
                val color = if (useAverageColor) {
                    labelMap[pixels.first().label]?.toInt()
                        ?: throw RuntimeException("Missing ${pixels.first().label} label")
                } else {
                    pixels.first().pixel.toInt()
                }
                outImage.setRGB(x, y, color)
            }
        }
    }

    ImageIO.write(outImage, "PNG", outputFile)
}


fun writeLabelsToFiles(
    dir: File,
    source: BufferedImage,
    useAverageColor: Boolean,
    labels: Array<Array<PixelLabel>>
) {
    val labelMap = getPixelLabelMapByHueAverage(labels)
    val width = labels.size
    val height = labels[0].size

    val labelGroups = labels.flatten().groupBy { it.label }
    val json = JSONObject()

    labelGroups.forEach { (label, pixels) ->
        val outImage = BufferedImage(width, height, ColorSpace.TYPE_RGB)
        val firstPixelPoint = pixels.first().point
        val outputFileName = "${pixels.size}-$label-$firstPixelPoint.png"
        val outputFile = File(dir, outputFileName)
        val pixelArray = JSONArray(pixels.map { "${it.point.x}, ${it.point.y}" })

        json.put(label.toString(), JSONObject().apply {
            put("file", outputFile.toPath().toUri().toString())
            put("pixels", pixelArray)
        })

        pixels.forEach { pixel ->
            val (x, y) = pixel.point
            val pixelValue = if (useAverageColor) {
                labelMap[pixel.label]?.toInt() ?: throw RuntimeException("Missing pixel label")
            } else {
                source.getRGB(x, y)
            }

            outImage.setRGB(x, y, pixelValue)
        }
        ImageIO.write(outImage, "PNG", outputFile)
    }

    println("Writing Summary File")
    File(dir, "_Summary_.json").outputStream().bufferedWriter().use { writer ->
        writer.write(json.toString(2))
    }
}

fun overlayPixelBoundsOnImage(
    source: BufferedImage,
    labels: Array<Array<PixelLabel>>,
    outputFile: File,
    penColor: Pixel = Pixel.fromInt(0xFFFFFF00.toInt())
) {
    val penInt = penColor.toInt()
    val width = source.width
    val height = source.height

    val labelToPointsMap = HashMap<Int, ArrayList<Point>>() //Key = Label; Value = Points in label

    for (x in 0 until width) {
        for (y in 0 until height) {
            val (_, label) = labels[x][y]
            val targetList = labelToPointsMap.getOrDefault(label, ArrayList())
            targetList.add(x with y)
            labelToPointsMap[label] = targetList
        }
    }

    val edges = labelToPointsMap.map { (_, points) ->
        points.bounds()
    }.flatten().toSet()

    val outImage = BufferedImage(width, height, ColorSpace.TYPE_RGB)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pt = x with y
            val pixelValue = if (pt in edges) {
                penInt
            } else {
                source.getRGB(x, y)
            }

            outImage.setRGB(x, y, pixelValue)
        }
    }

    ImageIO.write(outImage, "PNG", outputFile)
}