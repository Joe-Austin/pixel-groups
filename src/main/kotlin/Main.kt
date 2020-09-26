import net.joeaustin.data.Pixel
import java.awt.color.ColorSpace.TYPE_RGB
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.round

private const val PATCH_SIZE = 4

fun main() {
    val inputFile = File("data/segmented.png")
    val inputImage = ImageIO.read(inputFile)
    val outImage = createPixelatedImage(inputImage, PATCH_SIZE)

    val outName = "${inputFile.name}-out.png"
    val outFile = File("output/$outName")
    ImageIO.write(outImage, "PNG", outFile)


    println("Done!")
}

private fun createPixelatedImage(sourceImage: BufferedImage, patchSize: Int): BufferedImage {
    val outImage = BufferedImage(sourceImage.width, sourceImage.height, TYPE_RGB)

    for (y in 0 until sourceImage.height step patchSize) {
        for (x in 0 until sourceImage.width step patchSize) {
            val xOffset = min(patchSize, sourceImage.width - x)
            val yOffset = min(patchSize, sourceImage.height - y)
            val averageColor = computeAverageColor(sourceImage.getSubimage(x, y, xOffset, yOffset)).toInt()

            for (subY in y until y + yOffset) {
                for (subX in x until x + xOffset) {
                    outImage.setRGB(subX, subY, averageColor)
                }
            }

        }
    }

    return outImage
}

private fun computeAverageColor(image: BufferedImage): Pixel {
    var aSum = 0
    var rSum = 0
    var gSum = 0
    var bSum = 0

    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val pixel = Pixel.fromInt(image.getRGB(x, y))
            aSum += pixel.a
            rSum += pixel.r
            gSum += pixel.g
            bSum += pixel.b
        }
    }

    val size = image.height * image.width

    return Pixel(
            round(aSum / size.toDouble()).toInt(),
            round(rSum / size.toDouble()).toInt(),
            round(gSum / size.toDouble()).toInt(),
            round(bSum / size.toDouble()).toInt(),
    )

}