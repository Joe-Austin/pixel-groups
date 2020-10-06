import net.joeaustin.data.*
import net.joeaustin.utilities.bounds
import org.json.JSONArray
import org.json.JSONObject
import superpixel.VisionPixels
import java.awt.color.ColorSpace.TYPE_RGB
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.log2
import kotlin.math.round

fun main() {
    val inputImageFile = File("data/astro.png")
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

    val smallGroups = labels.flatten().groupBy { it.label }.filter { it.value.size < 16 }.size
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

    computeAndPrintGroupSimilarity(labels, 1444, 3358)

    println("Done!")
}

private fun computeAndPrintGroupSimilarity(labels: Array<Array<PixelLabel>>, label1: Int, label2: Int) {
    val averagePixels = getPixelLabelMapByHueAverage(labels)
    val labelGroups = labels.flatten().groupBy { it.label }

    val pixelGroup1 = labelGroups[label1]?.map { it.pixel } ?: throw RuntimeException("Label $label1 does not exist")
    val pixelGroup2 = labelGroups[label2]?.map { it.pixel } ?: throw RuntimeException("Label $label2 does not exist")

    val pixelGroup1Average = averagePixels[label1] ?: throw RuntimeException("Label $label1 does not exist")
    val pixelGroup2Average = averagePixels[label2] ?: throw RuntimeException("Label $label2 does not exist")

    var pixelGroup1Hsl = pixelGroup1Average.toHxHySL()
    var pixelGroup2Hsl = pixelGroup2Average.toHxHySL()

    println("HSL Cosine Similarity Between Pixel Group 1 -> 2 = ${pixelGroup1Hsl.cosineDistance(pixelGroup2Hsl)}")

    val pixelGroup1Entropy = computePixelGroupEntropy(pixelGroup1)
    val pixelGroup2Entropy = computePixelGroupEntropy(pixelGroup2)

    pixelGroup1Hsl = pixelGroup1Hsl.append(pixelGroup1Entropy)
    pixelGroup2Hsl = pixelGroup2Hsl.append(pixelGroup2Entropy)

    println("HSL & Entropy Cosine Similarity Between Pixel Group 1 -> 2 = ${pixelGroup1Hsl.cosineDistance(pixelGroup2Hsl)}")
    println(pixelGroup1Hsl)
    println(pixelGroup2Hsl)
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


fun computePixelGroupEntropy(group: List<Pixel>): Double {
    val lightnessGroup = group.map { round(it.toHsl()[2] * 100) }.groupBy { it }
    val maxEntropy = .01 * log2(0.01) * -100
    val possibilities = lightnessGroup.values.sumBy { it.size }.toDouble()

    println("Max: ${lightnessGroup.maxByOrNull { it.value.size }?.value?.size}")
    println("Min: ${lightnessGroup.minByOrNull { it.value.size }?.value?.size}")

    return lightnessGroup
        .toList()
        .sumOf { (_, group) ->
            val p = group.size / possibilities
            p * log2(p) / maxEntropy
        } * -1
}