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

fun getHueAverage(pixels: List<PixelLabel>): VectorN {
    var averageHsl = VectorN(0.0, 0.0, 0.0, 0.0)

    pixels.forEach { pixelLabel ->
        val pixelHsl = pixelLabel.pixel.toHxHySL()
        averageHsl += pixelHsl
    }

    averageHsl /= pixels.size.toDouble()

    return averageHsl
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

    val outImage = BufferedImage(width, height, ColorSpace.TYPE_RGB)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val currentLabel = labels[x][y].label
            val isEdge = getNeighborLocations(x, y, 1, width, height).any { (nx, ny) ->
                labels[nx][ny].label != currentLabel
            }
            val pixelValue = if (isEdge) {
                penInt
            } else {
                source.getRGB(x, y)
            }

            outImage.setRGB(x, y, pixelValue)
        }
    }

    ImageIO.write(outImage, "PNG", outputFile)
}

fun overlayCentroids(
    source: BufferedImage,
    labels: Array<Array<PixelLabel>>,
    centroidColor: Pixel = Pixel.fromInt(0xFF00FF00.toInt())
): BufferedImage {
    val penInt = centroidColor.toInt()
    val width = source.width
    val height = source.height

    val outImage = BufferedImage(width, height, ColorSpace.TYPE_RGB)

    val visitedLabels = HashSet<Int>()
    val labelMap = labels.flatten().groupBy { it.label }

    for (x in 0 until width) {
        for (y in 0 until height) {
            val label = labels[x][y].label

            if (label !in visitedLabels) {
                val (cx, cy) = getLabelEdges(label, labels, labelMap).centroid()
                visitedLabels.add(label)
                println("X: $cx; Y: $cy")
                outImage.setRGB(cx, cy, penInt)
            }

            if (outImage.getRGB(x, y) != penInt) {
                outImage.setRGB(x, y, source.getRGB(x, y))
            }
        }
    }

    return outImage
}