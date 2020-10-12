import kotlinx.coroutines.*
import net.joeaustin.data.PixelLabel
import net.joeaustin.data.Point
import net.joeaustin.data.with
import net.joeaustin.utilities.findSimilarLabels
import superpixel.VisionPixels
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val sourceFile = File("data/coco.png")
    val maskFile = File("data/coco_mask.png")
    val outputFile = File("output/${sourceFile.nameWithoutExtension}-Refined.png")

    val sourceImage = ImageIO.read(sourceFile)
    val sourceMask = ImageIO.read(maskFile)

    val visionPixels = VisionPixels(sourceImage)
    println("Getting labels")
    val labels = visionPixels.labelPixels(0.98)
    visionPixels.mergeSmallGroups(labels)

    println("Refining Image")
    val refinedImage = createRefinedImage(labels, sourceImage, sourceMask, 0.5, 0.95)
    println("Writing Image")
    ImageIO.write(refinedImage, "PNG", outputFile)
    println(outputFile.toPath().toUri())
    println("Done!")
}

fun createRefinedImage(
    labels: Array<Array<PixelLabel>>,
    sourceImage: BufferedImage,
    maskImage: BufferedImage,
    inclusiveThreshold: Double = 0.5,
    similarityThreshold: Double = 0.99,
): BufferedImage = runBlocking {

    val outputImage = BufferedImage(sourceImage.width, sourceImage.height, TYPE_INT_RGB)
    val visitedLabels = HashSet<Int>()
    println("Getting Masked Pixels")
    val maskedPixels = getMaskedPixels(maskImage)
    val labelMap = labels.flatten().groupBy { it.label }
    val maskedLabels = maskedPixels.map { (x, y) -> labels[x][y] }.distinctBy { it.label }
    val syncRoot = Any()

    maskedLabels.forEachIndexed { index, pixelLabel ->
        launch {
            print("Progress: ${(index / maskedLabels.size.toDouble()) * 100}\r")
            val (_, label, _) = pixelLabel
            val shouldRun = synchronized(syncRoot) {
                val v = label !in visitedLabels
                visitedLabels.add(label)
                v
            }
            if (shouldRun) {
                visitedLabels.add(label)
                if (shouldLabelBeIncluded(
                        maskedPixels,
                        labels,
                        labelMap,
                        label,
                        inclusiveThreshold,
                        similarityThreshold
                    )
                ) {
                    val pixels = labelMap[label] ?: throw RuntimeException("Label $label not found in label map")
                    putPixelsInImage(pixels, outputImage)
                }
            }
        }
    }

    println()
    println("Done with mask refinement")
    outputImage
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

private fun putPixelsInImage(pixels: List<PixelLabel>, sinkImage: BufferedImage) {
    pixels.forEach { pixelLabel ->
        val (pixel, _, pt) = pixelLabel
        sinkImage.setRGB(pt.x, pt.y, pixel.toInt())
    }
}