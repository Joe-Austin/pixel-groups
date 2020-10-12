package superpixel

import net.joeaustin.data.*
import net.joeaustin.utilities.getNeighborLocations
import java.awt.image.BufferedImage
import java.lang.RuntimeException
import kotlin.math.*

typealias GroupState = Pair<VectorN, Int> //Current Average Vector and Sample Size

private const val SIGMA = 1.0
private const val MIN_GROUP_SIZE = 5

class VisionPixels(private val image: BufferedImage) {
    private val width = image.width
    private val height = image.height

    fun labelPixelsWithAverage(threshold: Double = 0.9): Array<Array<PixelLabel>> {
        val dataStore = Array(width) { Array(height) { PixelLabel(Pixel(0, 0, 0, 0), 0, 0 with 0) } }
        val groupStateMap = HashMap<Int, GroupState>() //Key = LabelId, Value = GroupState

        var currentLabel = -1
        val mappedPixels = HashMap<Point, Int>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val currentPixel = Pixel.fromInt(image.getRGB(x, y))
                val currentPixelHsl = currentPixel.toHxHySL()
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
                val difference = currentPixelHsl - currentAverage
                val newAverage = currentAverage + (difference / newSampleSize.toDouble())

                groupStateMap[actualLabel] = newAverage to 0

                mappedPixels[x with y] = actualLabel
                dataStore[x][y] = PixelLabel(currentPixel, actualLabel, x with y)
            }
        }

        return dataStore
    }

    fun labelPixels(threshold: Double = 0.9): Array<Array<PixelLabel>> {
        val dataStore = Array(width) { Array(height) { PixelLabel(Pixel(0, 0, 0, 0), 0, 0 with 0) } }
        val groupStateMap = HashMap<Int, GroupState>() //Key = LabelId, Value = GroupState

        var currentLabel = -1
        val mappedPixels = HashMap<Point, Int>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val currentPixel = Pixel.fromInt(image.getRGB(x, y))
                val currentPixelHsl = currentPixel.toHxHySL()
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
                //val newSampleSize = samples + 1
                val groupVote = currentAverage * SIGMA
                val currentVote = currentPixelHsl * (1 - SIGMA)
                //val newAverage = currentAverage + (difference / newSampleSize.toDouble())
                val newAverage = groupVote + currentVote

                groupStateMap[actualLabel] = newAverage to 0

                mappedPixels[x with y] = actualLabel
                dataStore[x][y] = PixelLabel(currentPixel, actualLabel, x with y)
            }
        }

        return dataStore
    }

    fun mergeSmallGroups(dataStore: Array<Array<PixelLabel>>, minGroupSize: Int = MIN_GROUP_SIZE) {
        val smallRegions =
            dataStore.flatten().groupBy { it.label }.filter { it.value.size < minGroupSize }.toMutableMap()
        val joinMap = HashMap<Int, List<PixelLabel>>()
        println("Small group size: ${smallRegions.size}")

        smallRegions.forEach { (_, pixels) ->
            var bestLabelMatch = 0
            var bestMatchDifference = Double.MIN_VALUE

            pixels.forEach { pixel ->
                val currentPixelHsl = pixel.pixel.toHxHySL()
                val (x, y) = pixel.point
                val currentLabel = dataStore[x][y].label
                getNeighborLocations(x, y, 1, width, height)
                    .map { (x, y) -> dataStore[x][y] }
                    .filter { it.label != currentLabel }
                    .forEach { neighborPixel ->
                        val currentDifference = currentPixelHsl.cosineDistance(neighborPixel.pixel.toHxHySL())
                        if (currentDifference > bestMatchDifference) {
                            bestLabelMatch = neighborPixel.label
                            bestMatchDifference = currentDifference
                        }
                    }
            }

            //Assign best label to this group in the data store
            pixels.forEach { pixel ->
                val (x, y) = pixel.point
                dataStore[x][y] = dataStore[x][y].copy(label = bestLabelMatch)
            }

            smallRegions.computeIfPresent(bestLabelMatch) { _, currentPixels ->
                currentPixels + pixels
            }
        }

        val postMergeSmallRegions = dataStore.flatten().groupBy { it.label }.filter { it.value.size < minGroupSize }
        println("Post Merge Small group size: ${postMergeSmallRegions.size}")
    }


    fun labelImageWithTextureCondensing(
        textureThreshold: Double = 0.9,
        initialThreshold: Double = 0.95
    ): Array<Array<PixelLabel>> {
        val dataStore = labelPixels(initialThreshold)
        val colorRegions = dataStore.flatten().groupBy { it.label }
        //val colorLabelToTextureLabel = HashMap<Int, Int>() //Key = color label; value = new label with texture
        val colorLabelToTextureVectorMap = HashMap<Int, VectorN>() // Key = color label; value = texture vector

        for (y in 0 until height) {
            for (x in 0 until width) {
                val currentLabel = dataStore[x][y].label
                val currentTextureVector = colorLabelToTextureVectorMap.getOrPut(currentLabel) {
                    val region = colorRegions[currentLabel]
                        ?: throw RuntimeException("Color label $currentLabel was unexpectedly not found in color region map")
                    getRgbRegionTextureVector(region)
                }

                getNeighborLocations(x, y, 1, width, height)
                    .map { (x, y) -> dataStore[x][y].label }
                    .distinct()
                    .forEach { neighborLabel ->
                        if (neighborLabel != currentLabel) {
                            //Determine if they should be merged
                            val neighborRegion = colorRegions[neighborLabel]
                                ?: throw RuntimeException("Color label $neighborLabel was unexpectedly not found in color region map")
                            val neighborTextureVector = colorLabelToTextureVectorMap.getOrPut(neighborLabel) {
                                getRgbRegionTextureVector(neighborRegion)
                            }

                            val shouldMerge =
                                currentTextureVector.cosineDistance(neighborTextureVector) >= textureThreshold
                            if (shouldMerge) {
                                //Set neighbor region's labels to that of the current label
                                neighborRegion.forEach { (_, _, point) ->
                                    val currentDataStoreLabel = dataStore[point.x][point.y]
                                    dataStore[point.x][point.y] = currentDataStoreLabel.copy(label = currentLabel)
                                }
                            }
                        }
                    }
            }
        }

        return dataStore
    }

    private fun getRegionTextureVector(pixelLabels: List<PixelLabel>): VectorN {
        val colorVectors = pixelLabels.map { it.pixel.toHxHySL() }
        var averageColor = VectorN(0.0, 0.0, 0.0, 0.0)

        colorVectors.forEach { colorVector ->
            averageColor += colorVector
        }
        averageColor /= pixelLabels.size.toDouble()

        val averageLightness = averageColor[3]

        val variance = colorVectors.sumByDouble { v -> (v[3] - averageLightness).pow(2.0) }
        val std = sqrt(variance)

        return averageColor.append(std)
    }

    private fun getRegionLuminanceTexture(pixelLabels: List<PixelLabel>): VectorN {
        val colorVectors = pixelLabels.map { it.pixel.toHxHySL() }
        var averageColor = VectorN(0.0, 0.0, 0.0, 0.0)

        colorVectors.forEach { colorVector ->
            averageColor += colorVector
        }
        averageColor /= pixelLabels.size.toDouble()

        val averageLightness = averageColor[3]

        val variance = colorVectors.sumByDouble { v -> (v[3] - averageLightness).pow(2.0) }
        val std = sqrt(variance)

        return averageColor.append(std)
    }

    private fun getRgbRegionTextureVector(pixelLabels: List<PixelLabel>): VectorN {
        val hslColorVectors = pixelLabels.map { it.pixel.toHxHySL() }
        val rgbColorVectors = pixelLabels.map { it.pixel.toRgbVector() }

        var averageHsl = VectorN(0.0, 0.0, 0.0, 0.0)
        var averageRgb = VectorN(0.0, 0.0, 0.0)

        for (i in hslColorVectors.indices) {
            averageHsl += hslColorVectors[i]
            averageRgb += rgbColorVectors[i]
        }

        averageHsl /= pixelLabels.size.toDouble()
        averageRgb /= pixelLabels.size.toDouble()


        var sumRgbVariance = VectorN(0.0, 0.0, 0.0)
        rgbColorVectors.forEach { v ->
            val computedVector = v - averageRgb
            sumRgbVariance += computedVector * computedVector
        }

        val stdRgb = VectorN(sqrt(sumRgbVariance[0]), sqrt(sumRgbVariance[1]), sqrt(sumRgbVariance[2]))

        return averageHsl.append(stdRgb[0], stdRgb[1], stdRgb[2])
    }


    companion object {
        fun computePixelGroupEntropy(pixelGroup: List<Pixel>): Double {
            return computeHistogramPixelGroupEntropy(pixelGroup, 10)
            /*val lightnessGroup = pixelGroup.map { round(it.toHsl()[2] * 100) }.groupBy { it }
            val possibilities = pixelGroup.size.toDouble()
            val maxEntropy = (1.0 / possibilities) * log2((1.0 / possibilities)) * -possibilities

            return lightnessGroup
                .toList()
                .sumOf { (_, group) ->
                    val p = group.size / possibilities
                    p * log2(p) / maxEntropy
                } * -1
            */
        }

        fun computeHistogramPixelGroupEntropy(pixelGroup: List<Pixel>, binCount: Int = 10): Double {
            val lightnessGroup = pixelGroup.map { round(it.toHsl()[2] * 100) }.groupBy { l ->
                floor(l / 10) * 10
            }
            val possibilities = pixelGroup.size.toDouble()
            val maxEntropy = (1.0 / possibilities) * log2((1.0 / possibilities)) * -possibilities

            return lightnessGroup
                .toList()
                .sumOf { (_, group) ->
                    val p = group.size / possibilities
                    p * log2(p) / maxEntropy
                } * -1
        }
    }
}


private fun <T> List<T>.debug(run: List<T>.() -> Unit): List<T> {
    run(this)
    return this
}