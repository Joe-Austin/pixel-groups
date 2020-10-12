import net.joeaustin.data.*
import net.joeaustin.utilities.getNeighborLocations
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList

private const val BLACK = 0xFF000000.toInt()

fun main() {
    val sourceImageFile = File("data/ng.png")
    val contentImageFile = File("output/ng-Refined_Manual.png")
    val outputImageFile = File("output/${contentImageFile.nameWithoutExtension}-Filled.png")
    val similarity = 0.99

    //TODO: Add manual override for adds and removals

    val sourceImage = ImageIO.read(sourceImageFile)
    val contentImage = ImageIO.read(contentImageFile)

    println("Filling holes")
    fillHolesInImage(sourceImage, contentImage, similarity)

    ImageIO.write(contentImage, "PNG", outputImageFile)
    println(outputImageFile.toPath().toUri())
    println("Done!")

}

private fun fillHolesInImage(
    sourceImage: BufferedImage,
    contentImage: BufferedImage,
    similarityThreshold: Double
) {
    val testStack = Stack<PixelLabel>()
    testStack.addAll(findPixelsWithEmptyNeighbors(contentImage))

    while (testStack.isNotEmpty()) {
        val (px, _, pt) = testStack.pop()
        val (x, y) = pt
        val pxHsl = px.toHxHySL()

        getEmptyNeighbors(contentImage, x, y).forEach { (nx, ny) ->
            val neighborColor = sourceImage.getRGB(nx, ny)
            val neighborPx = Pixel.fromInt(neighborColor)
            val neighborHsl = neighborPx.toHxHySL()
            if (areVectorsSimilar(pxHsl, neighborHsl, similarityThreshold)) {
                contentImage.setRGB(nx, ny, neighborColor)
                if (hasEmptyNeighbors(contentImage, nx, ny)) {
                    testStack.push(PixelLabel(neighborPx, 0, nx with ny))
                }
            }
        }
    }
}

/**
 * Search the image for non-empty pixels with an empty pixel immediately neighboring it
 * @return A [List] of non-empty pixels that have at least one immediate empty neighbor.
 */
private fun findPixelsWithEmptyNeighbors(image: BufferedImage): List<PixelLabel> {
    val width = image.width
    val height = image.height

    val pixelsAdjacentToHoles = ArrayList<PixelLabel>(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val px = image.getRGB(x, y)
            if (px != BLACK && hasEmptyNeighbors(image, x, y)) {
                val label = PixelLabel(Pixel.fromInt(px), 0, x with y)
                pixelsAdjacentToHoles.add(label)
            }
        }
    }

    pixelsAdjacentToHoles.trimToSize()
    return pixelsAdjacentToHoles
}

private fun hasEmptyNeighbors(image: BufferedImage, x: Int, y: Int): Boolean {
    return getNeighborLocations(x, y, 1, image.width, image.height)
        .any { (nx, ny) ->
            image.getRGB(nx, ny) == BLACK
        }
}

private fun areVectorsSimilar(v1: VectorN, v2: VectorN, threshold: Double): Boolean {
    return v1.cosineDistance(v2) >= threshold
}

private fun getEmptyNeighbors(image: BufferedImage, x: Int, y: Int): List<Point> {
    return getNeighborLocations(x, y, 1, image.width, image.height)
        .filter { (nx, ny) ->
            image.getRGB(nx, ny) == BLACK
        }
}