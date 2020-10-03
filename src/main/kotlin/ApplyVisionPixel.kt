import net.joeaustin.data.*
import net.joeaustin.utilities.bounds
import org.json.JSONArray
import org.json.JSONObject
import superpixel.VisionPixels
import java.awt.color.ColorSpace.TYPE_RGB
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val inputImageFile = File("data/bs.jpg")
    val labelDumpDir: String? = null//"output/astro"
    val expirementName = "avg"
    val threshold = 0.95
    val outputGroupedFile = File("output/${inputImageFile.nameWithoutExtension}-vision-$expirementName-$threshold.png")
    val overlayOutputImageFile =
        File("output/${inputImageFile.nameWithoutExtension}-vision-$expirementName-$threshold-overlay.png")
    val inputImage = ImageIO.read(inputImageFile)

    val visionPixels = VisionPixels(inputImage)
    //val labels = visionPixels.labelPixelsWithAverage(threshold)
    val labels = visionPixels.labelPixels(threshold)
    //val labels = visionPixels.labelImageWithTextureCondensing(threshold, threshold)
    visionPixels.mergeSmallGroups(labels, 16)


    println("Creating grouped image")
    createSuperPixelImage(labels, outputGroupedFile)
    println("Grouped image created")

    val uniqueLabels = labels.flatten().distinctBy { it.label }.size
    println("Label Count: $uniqueLabels")

    println("Creating label overlay image")
    overlayPixelBoundsOnImage(inputImage, labels, overlayOutputImageFile)
    println("Label overlay image created")

    val smallGroups = labels.flatten().groupBy { it.label }.filter { it.value.size < 6 }.size
    println("$smallGroups small groups")
    /*labels.flatten().groupBy { it.label }.toList().sortedBy { it.second.size }.take(15)
        .forEach { (label, pixels) ->
            println("$label - ${pixels.size} pixels")
            pixels.forEach { println("${it.pixel} -> ${it.point}") }
            println()
        }*/

    labelDumpDir?.let { dumpDir ->
        println("Dumping labels to $dumpDir")
        dumpLabelsToFiles(File(dumpDir), inputImage, false, labels)
    }


    println("Done!")
}

private fun createSuperPixelImage(labels: Array<Array<PixelLabel>>, outputFile: File) {
    //val labelMap = getPixelLabelMap(labels)
    val labelMap = getPixelLabelMapByHueAverage(labels)
    val width = labels.size
    val height = labels[0].size
    val outImage = BufferedImage(width, height, TYPE_RGB)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val (_, label) = labels[x][y]
            labelMap[label]?.let { outImage.setRGB(x, y, it.toInt()) }
        }
    }

    ImageIO.write(outImage, "PNG", outputFile)

}

private fun getPixelLabelMap(labels: Array<Array<PixelLabel>>): Map<Int, Pixel> {
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

private fun getPixelLabelMapByHueAverage(labels: Array<Array<PixelLabel>>): Map<Int, Pixel> {
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

private fun overlayPixelBoundsOnImage(
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

    val outImage = BufferedImage(width, height, TYPE_RGB)

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

private fun dumpLabelsToFiles(
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
        val outImage = BufferedImage(width, height, TYPE_RGB)
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