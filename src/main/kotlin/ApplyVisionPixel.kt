import net.joeaustin.data.*
import net.joeaustin.utilities.bounds
import org.json.JSONArray
import org.json.JSONObject
import superpixel.VisionPixels
import java.awt.color.ColorSpace.TYPE_RGB
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.log2
import kotlin.math.round

fun main() {
    val inputImageFile = File("data/hair1.jpg")
    val labelDumpDir: String? = null//"output/astro"
    val expirementName = "group"
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

    //val smallGroups = labels.flatten().groupBy { it.label }.filter { it.value.size < 16 }.size
    //println("$smallGroups small groups")
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

    //computeAndPrintGroupSimilarity(labels, 1444, 3358)

    //val targetLabel = 3502
    val targetLabel = labels[95][516].label
    val similarGroups = findSimilarLabels(targetLabel, labels, 0.99, true)
    //val similarGroups = findAllSimilarLabels(targetLabel, labels, 0.99, true)
    val similarGroupFile = File("output/${expirementName}_Similar_Groups_To_$targetLabel.png")
    val targetGroup = labels.flatten().filter { it.label == targetLabel }
    println("Dumping similar pixels to image")
    dumpPixelsLabelsToFile(similarGroupFile,
        inputImage,
        false,
        labels,
        similarGroups.flatMap { it.value } + targetGroup)

    println("Similar Groups Written to ${similarGroupFile.toPath().toUri()}")

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

private fun dumpPixelsLabelsToFile(
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

    val outImage = BufferedImage(width, height, TYPE_RGB)

    for (y in 0 until width) {
        for (x in 0 until height) {
            val pt = Point(x, y)
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

fun computePixelGroupEntropy(pixelGroup: List<Pixel>): Double {
    val lightnessGroup = pixelGroup.map { round(it.toHsl()[2] * 100) }.groupBy { it }
    val possibilities = pixelGroup.size.toDouble()
    val maxEntropy = (1.0 / possibilities) * log2((1.0 / possibilities)) * -possibilities

    return lightnessGroup
        .toList()
        .sumOf { (_, group) ->
            val p = group.size / possibilities
            p * log2(p) / maxEntropy
        } * -1
}

fun findSimilarLabels(
    label: Int,
    labels: Array<Array<PixelLabel>>,
    threshold: Double = 0.95,
    useEntropy: Boolean = true
): Map<Int, List<PixelLabel>> {
    val labelGroups = labels.flatten().groupBy { it.label }
    val averagePixelMap = getPixelLabelMapByHueAverage(labels)
        .map { (label, averagePixel) ->
            val pixelGroup = labelGroups[label] ?: throw RuntimeException("Label $label not found?")
            val entropy = computePixelGroupEntropy(pixelGroup.map { it.pixel })
            val p = if (useEntropy) averagePixel.toHxHySL().append(entropy) else averagePixel.toHxHySL()
            label to p
        }.toMap()

    val targetVector = averagePixelMap[label] ?: throw RuntimeException("Label $label not found")
    val similarGroups = averagePixelMap.filter { (l, v) ->
        l != label && v.cosineDistance(targetVector) >= threshold
    }

    return labelGroups.filter { (label, _) -> label in similarGroups }
}

fun findAllSimilarLabels(
    label: Int,
    labels: Array<Array<PixelLabel>>,
    threshold: Double = 0.95,
    useEntropy: Boolean = true
): Map<Int, List<PixelLabel>> {
    val foundGroups = HashMap<Int, List<PixelLabel>>()
    val visitedLabels = HashSet<Int>()
    val labelsToSearch = Stack<Int>()

    labelsToSearch.push(label)

    while (labelsToSearch.isNotEmpty()) {
        val searchLabel = labelsToSearch.pop()
        println("${labelsToSearch.size}")
        visitedLabels.add(searchLabel)

        findSimilarLabels(searchLabel, labels, threshold, useEntropy)
            .forEach { (foundLabel, pixels) ->
                if (!visitedLabels.contains(foundLabel) && !labelsToSearch.contains(foundLabel)) {
                    foundGroups[foundLabel] = pixels
                    labelsToSearch.push(foundLabel)
                }
            }
    }

    return foundGroups
}