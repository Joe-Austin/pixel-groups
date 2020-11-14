package net.joeaustin

import net.joeaustin.data.Pixel
import net.joeaustin.data.PixelLabel
import net.joeaustin.data.Point
import net.joeaustin.data.with
import net.joeaustin.utilities.overlayCentroids
import net.joeaustin.utilities.overlayPixelBoundsOnImage
import org.json.JSONArray
import org.json.JSONObject
import superpixel.VisionPixels
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val imageFileName = "data/goat.jpg"
    val mergeThreshold = 0.99

    val imageFile = File(imageFileName)
    val overlayImagePath = "output/${imageFile.nameWithoutExtension}-label_overlay.png"
    val groupedPath = "output/${imageFile.nameWithoutExtension}-grouped.png"
    val groupedFile = File(groupedPath)
    val outputFile = File(overlayImagePath)
    val image = ImageIO.read(imageFile)
    val jsonFile = File(imageFile.parent, "${imageFile.nameWithoutExtension}.json")
    val json = JSONObject(jsonFile.readText())

    val labels = loadImageLabels(image, json)
    val centroidImage = overlayCentroids(image, labels)
    overlayPixelBoundsOnImage(centroidImage, labels, outputFile)
    println(outputFile.toPath().toUri())

    val visionPixels = VisionPixels(image)
    visionPixels.mergeNeighborGroups9(labels, mergeThreshold)

    overlayPixelBoundsOnImage(image, labels, groupedFile)
    println(groupedFile.toPath().toUri())
}

private fun loadImageLabels(image: BufferedImage, labelObject: JSONObject): Array<Array<PixelLabel>> {
    val labelMap = getPixelLabelMap(labelObject)
    return Array(image.width) { x ->
        Array(image.height) { y ->
            val pixel = Pixel.fromInt(image.getRGB(x, y))
            val pt = x with y
            val label = labelMap[pt] ?: error("Missing label")
            PixelLabel(pixel, label, pt)
        }
    }

}

private fun getPixelLabelMap(labelObject: JSONObject): Map<Point, Int> {
    val map = HashMap<Point, Int>()
    labelObject.keySet().forEach { labelString ->
        val label = labelString.toInt()
        labelObject.getJSONArray(labelString).forEach { array ->
            val x = (array as JSONArray)[0] as Int
            val y = array[1] as Int
            map[x with y] = label
        }
    }

    return map
}