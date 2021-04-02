import jdk.jfr.Threshold
import net.joeaustin.data.*
import net.joeaustin.utilities.*
import org.json.JSONArray
import org.json.JSONObject
import superpixel.DistancePixel
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
    //val inputImageFile = File("/Users/joeaustin/Downloads/BSDS300 2/images/train/43070.jpg")
    val inputImageFile = File("data/3.jpg")
    val labelDumpDir: String? = null // "data/dog_labels"
    val expirementName = "group"
    val threshold = 0.95
    val mergeThreshold: Double? = 0.995


    /*
    //Largest pixel group

    val largestOutput = File("output/${inputImageFile.nameWithoutExtension}-largestGroup.png")
    writeOutLargestImageRegion(inputImageFile, largestOutput, threshold, false, false)
    println(largestOutput.toPath().toUri())

    return
    */


    //val distanceThreshold = 0.25
    val collectSimilarGroupPosition: Point? = null //218 with 190 //null// 372  with 500
    val outputGroupedFile = File("output/${inputImageFile.nameWithoutExtension}-vision-$expirementName-$threshold.png")
    val outputDistanceGroupedFile =
        File("output/${inputImageFile.nameWithoutExtension}-distance_vision-$expirementName-$threshold.png")
    val overlayOutputImageFile =
        File("output/${inputImageFile.nameWithoutExtension}-vision-$expirementName-$threshold-overlay.png")
    val overlayDistanceOutputImageFile =
        File("output/${inputImageFile.nameWithoutExtension}-distance_vision-$expirementName-$threshold-overlay.png")
    val inputImage = ImageIO.read(inputImageFile)

    val visionPixels = VisionPixels(inputImage)
    //val distancePixels = DistancePixel(inputImage)
    //val labels = visionPixels.labelPixelsWithAverage(threshold)
    val labels = visionPixels.labelPixels(threshold)


    //val distanceLabels = distancePixels.labelPixels(distanceThreshold)
    //val labels = visionPixels.labelImageWithTextureCondensing(threshold, threshold)
    println("Raw Lines")
    getLines(labels)

    //println("Removing Lines")
    //visionPixels.removeLines(labels)
    //println("Lines Removed")
    //getLines(labels)


    //println("Merging Neighbors")
    //visionPixels.mergeNeighborGroups(labels, 0.95)
    //visionPixels.mergeNeighborGroups2(labels, 0.98)
    //KEEP
    mergeThreshold?.let { visionPixels.mergeNeighborGroups9(labels, it)}
    //println("Neighbors Merged")
    //getLines(labels)

    //KEEP
    visionPixels.mergeSmallGroups(labels, 16)
    println("Small Groups")
    //getLines(labels)
    //*/

    //distancePixels.mergeSmallGroups(distanceLabels, 16)


    println("Creating grouped image")
    createSuperPixelImage(labels, outputGroupedFile)
    println(outputGroupedFile.toPath().toUri())
    //createSuperPixelImage(distanceLabels, outputDistanceGroupedFile)
    println(outputDistanceGroupedFile.toPath().toUri())
    println("Grouped image created")

    val uniqueLabels = labels.flatten().distinctBy { it.label }.size
    //val uniqueDistanceLabels = distanceLabels.flatten().distinctBy { it.label }.size
    println("Label Count: $uniqueLabels")
    //println("Distance Label Count: $uniqueDistanceLabels")

    println("Creating label overlay image")
    println("Overlaying Centroids")
    val centroidImage = overlayCentroids(inputImage, labels)
    overlayPixelBoundsOnImage(centroidImage, labels, overlayOutputImageFile)
    println(overlayOutputImageFile.toPath().toUri())
    //overlayPixelBoundsOnImage(inputImage, distanceLabels, overlayDistanceOutputImageFile)
    println(overlayDistanceOutputImageFile.toPath().toUri())
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
    collectSimilarGroupPosition?.let { (px, py) ->
        val labelGroups = labels.flatten().groupBy { it.label }
        val targetLabel = labels[px][py].label
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

        val labelFile = File("output/${expirementName}_$targetLabel.png")

        writeLabelsToFile(labelFile, inputImage, false, labels, labelGroups[targetLabel] ?: error(""))
        println("Similar Groups Written to ${similarGroupFile.toPath().toUri()}")
        println("Single Group Written to ${labelFile.toPath().toUri()}")
    }


    println("Done!")
}

private fun getLines(labels: Array<Array<PixelLabel>>) {
    val labelMap = labels.flatten().groupBy { it.label }
    val width = labels.size
    val height = labels[0].size

    val lines = labelMap.filter { (_, pixels) ->
        pixels.all { pixel ->
            val (_, label, pt) = pixel
            getNeighborLocations(pt.x, pt.y, 1, width, height).any { (nx, ny) ->
                val nLabel = labels[nx][ny].label
                nLabel != label
            }
        }
    }

    val minSize = lines.minByOrNull { (_, pixels) -> pixels.size }?.value?.size
    val maxSize = lines.maxByOrNull { (_, pixels) -> pixels.size }?.value?.size

    println("Found ${lines.size} lines (Range: $minSize - $maxSize)")
}

private fun writeOutLargestImageRegion(
    input: File,
    outputFile: File,
    threshold: Double = 0.95,
    useAverageColor: Boolean = true,
    includeSimilarRegions: Boolean = true,
    minGroupSize: Int = 16
) {
    val inputImage = ImageIO.read(input)
    val visionPixels = VisionPixels(inputImage)

    println("Getting labels")
    val labels = visionPixels.labelPixels(threshold)
    visionPixels.mergeSmallGroups(labels, minGroupSize)

    println("Finding largest group")
    val labelGroups = labels.flatten().groupBy { it.label }
    val (largestLabel, pixels) = labelGroups.maxByOrNull { (_, pixels) -> pixels.size }
        ?: throw RuntimeException("No groups?")
    val largestPixelsPoints = pixels.map { it.point }.toHashSet()

    println("Finding similar groups")
    val similarLabels = if (includeSimilarRegions) {
        findSimilarLabels(largestLabel, labels, threshold, true)
            .filter { (_, similarPixels) ->
                similarPixels.any { sp ->
                    val (nx, ny) = sp.point
                    val neighborPoints = getNeighborLocations(nx, ny, 1, inputImage.width, inputImage.height)
                    neighborPoints.any { np -> np in largestPixelsPoints }
                }
            }
    } else {
        emptyMap()
    }

    println("Writing label $largestLabel to file")
    writeLabelsToFile(outputFile, inputImage, useAverageColor, labels, pixels + similarLabels.values.flatten())


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