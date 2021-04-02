package net.joeaustin

import net.joeaustin.data.Pixel
import net.joeaustin.data.PixelLabel
import net.joeaustin.data.with
import net.joeaustin.utilities.overlayCentroids
import net.joeaustin.utilities.overlayPixelBoundsOnImage
import net.joeaustin.utilities.writeLabelsToFile
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val imageId = "3096"
    val humanId = "1107"
    val segmentationFile = File("/Users/joeaustin/Downloads/BSDS300/human/color/$humanId/$imageId.seg")
    val imageFile = File("/Users/joeaustin/Downloads/BSDS300 2/images/test/$imageId.jpg")
    val outputFile =
        File("output/${imageFile.nameWithoutExtension}_human_${segmentationFile.nameWithoutExtension}.png")


    val image = ImageIO.read(imageFile)
    val labels = parseFile(segmentationFile, image)
    overlayPixelBoundsOnImage(image, labels, outputFile)

    println(outputFile.toPath().toUri())
}

private fun parseFile(segmentationFile: File, image: BufferedImage): Array<Array<PixelLabel>> {
    val lines = segmentationFile.readLines()
    val width = image.width
    val height = image.height

    val data = Array<Array<PixelLabel>>(width) { x ->
        Array(height) { y ->
            val pixel = Pixel.fromInt(image.getRGB(x, y))
            PixelLabel(pixel, 0, x with y)
        }
    }

    var foundData = false

    lines.forEach { line ->
        if (!foundData) {
            foundData = line.trim().toLowerCase() == "data"
        } else {
            parseLine(line).forEach { (label, y, x) ->
                data[x][y] = data[x][y].copy(label = label)
            }
        }
    }

    return data
}

private fun parseLine(line: String): List<Triple<Int, Int, Int>> {
    val parts = line.trim().split(" ")
    val label = parts[0].toInt()
    val row = parts[1].toInt()
    val startCol = parts[2].toInt()
    val stopCol = parts[3].toInt()

    val data = ArrayList<Triple<Int, Int, Int>>(parts.size - 2)

    for (i in startCol..stopCol) {
        data.add(Triple(label, row, i))
    }

    return data
}