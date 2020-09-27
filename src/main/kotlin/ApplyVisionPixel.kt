import net.joeaustin.data.Pixel
import net.joeaustin.data.PixelLabel
import net.joeaustin.data.Point
import net.joeaustin.data.with
import net.joeaustin.utilities.bounds
import superpixel.VisionPixels
import java.awt.color.ColorSpace.TYPE_RGB
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val inputImageFile = File("data/astro.png")
    val threshold = 0.95
    val outputGroupedFile = File("output/${inputImageFile.nameWithoutExtension}-vision-$threshold.png")
    val overlayOutputImageFile = File("output/${inputImageFile.nameWithoutExtension}-vision-$threshold-overlay.png")
    val inputImage = ImageIO.read(inputImageFile)

    val visionPixels = VisionPixels(inputImage)
    val labels = visionPixels.labelPixels(threshold)

    println("Creating grouped image")
    createSuperPixelImage(labels, outputGroupedFile)
    println("Grouped image created")

    val uniqueLabels = labels.flatten().distinctBy { it.label }.size
    println("Label Count: $uniqueLabels")

    println("Creating label overlay image")
    overlayPixelBoundsOnImage(inputImage, labels, overlayOutputImageFile)
    println("Label overlay image created")

    println("Done!")
}

private fun createSuperPixelImage(labels: Array<Array<PixelLabel>>, outputFile: File) {
    val labelMap = getPixelLabelMap(labels)
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