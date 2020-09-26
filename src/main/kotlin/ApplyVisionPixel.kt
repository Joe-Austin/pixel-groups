import net.joeaustin.data.Pixel
import net.joeaustin.data.PixelLabel
import superpixel.VisionPixels
import java.awt.color.ColorSpace.TYPE_RGB
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val inputImageFile = File("data/segmented.png")
    val threshold = 0.95
    val outputImageFile = File("output/${inputImageFile.nameWithoutExtension}-vision-$threshold.png")
    val inputImage = ImageIO.read(inputImageFile)

    val visionPixels = VisionPixels(inputImage)
    val labels = visionPixels.labelPixels(threshold)

    createSuperPixelImage(labels, outputImageFile)

    val uniqueLabels = labels.flatten().distinctBy { it.label }.size
    println("Label Count: $uniqueLabels")

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