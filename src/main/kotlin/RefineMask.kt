import kotlinx.coroutines.*
import net.joeaustin.data.PixelLabel
import net.joeaustin.data.Point
import net.joeaustin.data.with
import net.joeaustin.utilities.computeLabelGraph
import net.joeaustin.utilities.findSimilarLabels
import superpixel.VisionPixels
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
fun main() {
    //val sourceFile = File("data/coco-really_sharpen.png")
    //val maskFile = File("data/coco_mask.png")
    val sourceFile = File("data/ng.png")
    val maskFile = File("data/ng_mask.png")
    val outputFile = File("output/${sourceFile.nameWithoutExtension}-Refined.png")

    val sourceImage = ImageIO.read(sourceFile)
    val sourceMask = ImageIO.read(maskFile)

    val visionPixels = VisionPixels(sourceImage)
    println("Getting labels")
    val labels = visionPixels.labelPixels(0.99)
    visionPixels.mergeSmallGroups(labels)

    println("Refining Image")
    val refinedImage = createRefinedImage(labels, sourceImage, sourceMask, 0.25, 0.99)
    println()
    println("Done with Mask Refinement")
    println("Writing Image")
    ImageIO.write(refinedImage, "PNG", outputFile)
    println(outputFile.toPath().toUri())
    println("Done!")
}

@ExperimentalTime
fun createRefinedImage(
    labels: Array<Array<PixelLabel>>,
    sourceImage: BufferedImage,
    maskImage: BufferedImage,
    inclusiveThreshold: Double = 0.5,
    similarityThreshold: Double = 0.99,
): BufferedImage {

    val outputImage = BufferedImage(sourceImage.width, sourceImage.height, TYPE_INT_RGB)
    println("Getting label graph")
    val labelGraph = computeLabelGraph(labels, similarityThreshold)

    println("Getting Masked Pixels")
    val maskedPixels = getMaskedPixels(maskImage)
    val labelMap = labels.flatten().groupBy { it.label }
    val maskedLabels = maskedPixels.map { (x, y) -> labels[x][y] }.distinctBy { it.label }
    val total = maskedLabels.size.toDouble()
    val processed = AtomicInteger(0)

    runBlocking {
        maskedLabels.forEach { pixelLabel ->
            launch {
                if (shouldLabelBeIncluded(maskedPixels, labelMap, labelGraph, pixelLabel.label, inclusiveThreshold)) {
                    val pixels = labelMap[pixelLabel.label]
                        ?: throw RuntimeException("Label ${pixelLabel.label} not found in label map")
                    putPixelsInImage(pixels, outputImage)
                }
                print("Progress: ${(processed.getAndIncrement() / total) * 100}\r")
            }
        }
    }

    return outputImage
}

private fun getMaskedPixels(maskImage: BufferedImage): Set<Point> {
    val set = HashSet<Point>()
    val blackColor = 0xFF000000.toInt()
    for (y in 0 until maskImage.height) {
        for (x in 0 until maskImage.width) {
            val pixelColor = maskImage.getRGB(x, y)
            if (pixelColor != blackColor) {
                set.add(x with y)
            }
        }
    }

    return set
}

private fun shouldLabelBeIncluded(
    maskedPixels: Set<Point>,
    labels: Array<Array<PixelLabel>>,
    labelMap: Map<Int, List<PixelLabel>>,
    label: Int,
    inclusiveThreshold: Double,
    pixelSimilarityThreshold: Double = 0.99
): Boolean {
    val targetGroupPixelTotal =
        labelMap[label]?.map { it.point } ?: throw RuntimeException("Label $label not found in label map")
    val labelPixels = targetGroupPixelTotal + findSimilarLabels(label, labels, pixelSimilarityThreshold)
        .map { (_, pixelLabels) -> pixelLabels.map { it.point } }
        .flatten()

    val maskGroups = labelPixels.groupBy { it in maskedPixels }
    val maskedPixelCount = maskGroups[true]?.size ?: 0

    return maskedPixelCount / labelPixels.size.toDouble() >= inclusiveThreshold
}

private fun shouldLabelBeIncluded(
    maskedPixels: Set<Point>,
    labelMap: Map<Int, List<PixelLabel>>,
    labelGraph: Map<Int, Set<Int>>,
    label: Int,
    inclusiveThreshold: Double,
): Boolean {
    val targetGroupPixelTotal =
        labelMap[label] ?: throw RuntimeException("Label $label not found in label map")
    val labelPixels = targetGroupPixelTotal + (labelGraph[label]?.flatMap { similarLabel ->
        labelMap[similarLabel] ?: throw RuntimeException("Label $similarLabel not found in label map")
    } ?: throw RuntimeException("Label $label not found in label map"))

    val maskGroups = labelPixels.groupBy { it.point in maskedPixels }
    val maskedPixelCount = maskGroups[true]?.size ?: 0

    return maskedPixelCount / labelPixels.size.toDouble() >= inclusiveThreshold
}

private fun putPixelsInImage(pixels: Collection<PixelLabel>, sinkImage: BufferedImage) {
    pixels.forEach { pixelLabel ->
        val (pixel, _, pt) = pixelLabel
        sinkImage.setRGB(pt.x, pt.y, pixel.toInt())
    }
}