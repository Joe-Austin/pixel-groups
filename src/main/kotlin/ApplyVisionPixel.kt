import net.joeaustin.data.*
import net.joeaustin.utilities.*
import org.json.JSONArray
import org.json.JSONObject
import superpixel.VisionPixels
import superpixel.VisionPixels.Companion.computePixelGroupEntropy
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
    val inputImageFile = File("data/coco.png")
    val labelDumpDir: String? = null//"output/astro"
    val expirementName = "group"
    val threshold = 0.97
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
        writeLabelsToFiles(File(dumpDir), inputImage, false, labels)
    }

    //computeAndPrintGroupSimilarity(labels, 1444, 3358)

    //val targetLabel = 3502
    val targetLabel = labels[1129][542].label
    val similarGroups = findSimilarLabels(targetLabel, labels, 0.99, true).toMutableMap()
    //val similarGroups = findAllSimilarLabels(targetLabel, labels, 0.99, true)
    val similarGroupFile = File("output/${expirementName}_Similar_Groups_To_$targetLabel.png")
    val targetGroup = labels.flatten().filter { it.label == targetLabel }
    println("Target Group has ${targetGroup.size} pixels")
    similarGroups[targetLabel] = targetGroup
    println("Dumping similar pixels to image")
    writeLabelsToFile(similarGroupFile,
        inputImage,
        false,
        labels,
        similarGroups.flatMap { it.value })

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