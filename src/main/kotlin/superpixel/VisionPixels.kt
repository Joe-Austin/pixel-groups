package superpixel

import net.joeaustin.data.Pixel
import net.joeaustin.data.PixelLabel
import net.joeaustin.data.VectorN
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min

typealias Point = Pair<Int, Int>
typealias GroupState = Pair<VectorN, Int> //Current Average Vector and Sample Size

class VisionPixels(private val image: BufferedImage) {
    private val width = image.width
    private val height = image.height

    fun labelPixels(threshold: Double = 0.9): Array<Array<PixelLabel>> {
        val dataStore = Array(width) { Array(height) { PixelLabel(Pixel(0, 0, 0, 0), 0) } }
        val groupStateMap = HashMap<Int, GroupState>() //Key = LabelId, Value = GroupState

        var currentLabel = -1
        val mappedPixels = HashMap<Point, Int>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val currentPixel = Pixel.fromInt(image.getRGB(x, y))
                val currentPixelHsl = currentPixel.toHxHyLV()
                var bestDifference = -1.0
                var closestLabel = -1

                val neighbors = getNeighborLocations(x, y, 1, width, height)
                val mappedNeighbors =
                    neighbors.mapNotNull { pt -> mappedPixels[pt]?.run { pt to this } }

                mappedNeighbors.forEach { (_, label) ->
                    groupStateMap[label]?.let { (groupHsl, _) ->
                        val difference = currentPixelHsl.cosineDistance(groupHsl)
                        if (difference > bestDifference) {
                            bestDifference = difference
                            closestLabel = label
                        }
                    }
                }

                val actualLabel = if (bestDifference >= threshold) {
                    closestLabel
                } else {
                    ++currentLabel
                }

                val (currentAverage, samples) = groupStateMap[actualLabel] ?: currentPixelHsl to 0
                val newSampleSize = samples + 1
                val difference = currentAverage - currentPixelHsl
                val newAverage = currentAverage + (difference / newSampleSize.toDouble())

                groupStateMap[actualLabel] = newAverage to newSampleSize

                mappedPixels[x to y] = actualLabel
                dataStore[x][y] = PixelLabel(currentPixel, actualLabel)
            }
        }

        return dataStore
    }

    private fun getNeighborLocations(
        x: Int, y: Int, radius: Int,
        width: Int, height: Int
    ): List<Point> {
        val minX = max(0, x - radius)
        val minY = max(0, y - radius)

        val maxX = min(width, x + radius)
        val maxY = min(height, y + radius)

        val size = ((maxX - minX + 1) * (maxY - minY + 1)) - 1
        val neighbors = ArrayList<Pair<Int, Int>>(size)

        for (i in minX..maxX) {
            for (j in minY..maxY) {
                if (i != x || j != y) {
                    neighbors.add(i to j)
                }
            }
        }

        return neighbors
    }
}