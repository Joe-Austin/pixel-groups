package superpixel

import net.joeaustin.data.*
import net.joeaustin.utilities.*
import java.awt.image.BufferedImage
import kotlin.RuntimeException
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
            dataStore.flatten().groupBy { it.label }
                .filter { isPixelGroupTooSmall(dataStore, it.value, minGroupSize / 2) }
                .toMutableMap()
        println("Small group size: ${smallRegions.size}")

        smallRegions.forEach { (_, pixels) ->
            var bestLabelMatch = 0
            var bestMatchDifference = -Double.MAX_VALUE

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
            //println("Re-assigning pixel group ${pixels.first().label} with ${pixels.size} pixels to $bestLabelMatch")
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

    private fun isPixelGroupTooSmall(
        dataStore: Array<Array<PixelLabel>>,
        pixels: List<PixelLabel>,
        minSize: Int
    ): Boolean {
        val r = minSize / 2

        return pixels.none { pl ->
            getNeighborLocations(pl.point.x, pl.point.y, r, width, height).all { (nx, ny) ->
                dataStore[nx][ny].label == pl.label
            }
        }
    }

    fun mergeNeighborGroups9(dataStore: Array<Array<PixelLabel>>, threshold: Double) {
        val originalDataStore = dataStore.copyOf()
        val labelMap = dataStore.flatten().groupBy { it.label }
        val vectorMap = labelMap.mapValues { getHueAverage(it.value) }
        val visitedLabels = HashSet<Int>()

        labelMap.forEach { (originalLabel, pixels) ->
            if (originalLabel !in visitedLabels) {
                val (x, y) = pixels.first().point
                val actualLabel = dataStore[x][y].label
                val groupsToVisit = HashSet<Int>()

                findSimilarPixelNeighborLabels(
                    originalLabel,
                    labelMap,
                    vectorMap,
                    originalDataStore,
                    threshold
                ).forEach { neighborLabel ->
                    if (neighborLabel !in visitedLabels) {
                        groupsToVisit.add(neighborLabel)
                    }
                }

                while (groupsToVisit.isNotEmpty()) {
                    val targetLabel = groupsToVisit.first()
                    labelMap[targetLabel]?.forEach { (_, _, pt) ->
                        dataStore[pt.x][pt.y] = dataStore[pt.x][pt.y].copy(label = actualLabel)
                    }

                    visitedLabels.add(targetLabel)
                    groupsToVisit.remove(targetLabel)

                    findSimilarPixelNeighborLabels(
                        targetLabel,
                        labelMap,
                        vectorMap,
                        originalDataStore,
                        threshold
                    ).forEach { neighborLabel ->
                        if (neighborLabel !in visitedLabels) {
                            groupsToVisit.add(neighborLabel)
                        }
                    }
                }
            }
        }
    }

    fun mergeNeighborGroups2(dataStore: Array<Array<PixelLabel>>, threshold: Double) {
        //Pixels mapped by their label
        val labelMap = dataStore.flatten().groupBy { it.label }
        val mergedSet = HashSet<Int>(labelMap.keys.size)

        //Build similarity graph
        val neighborMap = computeLabelGraph(dataStore, threshold, true)

        //Merge similar adjacent neighbor groups
        neighborMap.forEach { (label, similarLabels) ->
            val labelGroup = labelMap[label] ?: error("Label not found")
            val (fpX, fpY) = labelGroup.first().point

            val actualLabel = similarLabels.firstOrNull { it in mergedSet } ?: dataStore[fpX][fpY].label


            similarLabels.forEach { similarLabel ->
                val similarLabelGroup = labelMap[similarLabel] ?: error("Label not found")
                val (sX, sY) = similarLabelGroup.first().point
                val similarLabelActualLabel = dataStore[sX][sY].label

                if (actualLabel != similarLabelActualLabel &&
                    arePixelGroupsNeighbors(labelGroup, similarLabelGroup)
                ) {
                    //println("Merging Similar Groups")
                    //Merge the groups
                    similarLabelGroup.forEach { (_, _, mergePoint) ->
                        val newPixelLabel = dataStore[mergePoint.x][mergePoint.y].copy(label = actualLabel)
                        dataStore[mergePoint.x][mergePoint.y] = newPixelLabel
                    }
                }
            }
        }
    }

    fun mergeNeighborGroups(dataStore: Array<Array<PixelLabel>>, threshold: Double) {
        val labelMap = dataStore.flatten().groupBy { it.label }
        val averageVectorMap = HashMap<Int, List<PixelLabel>>()
        labelMap.forEach { (_, pixels) ->
            val (x, y) = pixels.first().point
            //Due to a merge, this may have changed from when originally grouped.
            val actualLabel = dataStore[x][y].label
            val currentPixels = averageVectorMap.getOrPut(actualLabel) {
                labelMap[actualLabel] ?: error("No label found")
            }

            val currentGroupVector = getHueAverage(currentPixels)

            var bestNeighborMatch: List<PixelLabel>? = null
            var bestSimilarity = Double.MIN_VALUE

            findPixelNeighborLabels(actualLabel, pixels, dataStore).forEach { neighborLabel ->
                val neighborPixels = averageVectorMap.getOrPut(neighborLabel) {
                    labelMap[neighborLabel] ?: throw RuntimeException("No label found")
                }
                val neighborVector = getHueAverage(neighborPixels)

                val similarity = neighborVector.cosineDistance(currentGroupVector)
                if (similarity >= threshold && similarity > bestSimilarity) {
                    bestNeighborMatch = neighborPixels
                    bestSimilarity = similarity
                }
            }

            bestNeighborMatch?.let { match ->
                averageVectorMap[actualLabel] = currentPixels + match
                //Perform the merge in the labels
                match.forEach { matchedPixelLabel ->
                    val (mx, my) = matchedPixelLabel.point
                    dataStore[mx][my] = dataStore[mx][my].copy(label = actualLabel)
                }
            }
        }
    }

    fun removeLines(labels: Array<Array<PixelLabel>>) {
        val width = labels.size
        val height = labels[0].size

        var line = findFirstLine(labels)

        while (line != null) {
            line.second.forEach { (pixel, pixelLabel, pixelPoint) ->
                val pixelVector = pixel.toHxHySL()
                //Get Best Neighbor
                getNeighborLocations(pixelPoint.x, pixelPoint.y, 1, width, height).maxByOrNull { (nx, ny) ->
                    val (nPixel, nLabel, _) = labels[nx][ny]
                    val nPixelVector = nPixel.toHxHySL()
                    when (nLabel) {
                        pixelLabel -> Double.MIN_VALUE
                        else -> nPixelVector.cosineDistance(pixelVector)
                    }
                }?.let { (nx, ny) ->
                    val newLabel = labels[nx][ny].label
                    labels[pixelPoint.x][pixelPoint.y] = PixelLabel(pixel, newLabel, pixelPoint)
                }
            }

            line = findFirstLine(labels)
        }
    }

    fun findFirstLine(labels: Array<Array<PixelLabel>>): Pair<Int, List<PixelLabel>>? {
        val labelMap = labels.flatten().groupBy { it.label }
        val width = labels.size
        val height = labels[0].size

        return labelMap.toList().firstOrNull { (_, pixels) ->
            pixels.all { pixel ->
                val (_, label, pt) = pixel
                getNeighborLocations(pt.x, pt.y, 1, width, height).any { (nx, ny) ->
                    val nLabel = labels[nx][ny].label
                    nLabel != label
                }
            }
        }
    }

    fun findLines(labels: Array<Array<PixelLabel>>): Map<Int, List<PixelLabel>> {
        val labelMap = labels.flatten().groupBy { it.label }
        val width = labels.size
        val height = labels[0].size

        return labelMap.filter { (_, pixels) ->
            pixels.all { pixel ->
                val (_, label, pt) = pixel
                getNeighborLocations(pt.x, pt.y, 1, width, height).any { (nx, ny) ->
                    val nLabel = labels[nx][ny].label
                    nLabel != label
                }
            }
        }
    }

    private fun findPixelNeighborLabels(
        label: Int,
        pixels: List<PixelLabel>,
        labels: Array<Array<PixelLabel>>,
    ): Set<Int> {
        val set = HashSet<Int>()
        pixels.forEach { pixelLabel ->
            val (x, y) = pixelLabel.point
            getNeighborLocations(x, y, 1, image.width, image.height).forEach { (nx, ny) ->
                val nLabel = labels[nx][ny].label
                if (nLabel != label) {
                    set.add(nLabel)
                }
            }
        }

        return set
    }

    private fun findSimilarPixelNeighborLabels(
        label: Int,
        labelMap: Map<Int, List<PixelLabel>>,
        vectorMap: Map<Int, VectorN>,
        labels: Array<Array<PixelLabel>>,
        threshold: Double
    ): Set<Int> {
        val set = HashSet<Int>()
        val pixels = labelMap[label] ?: error("Label not found")
        val pixelsVector = vectorMap[label] ?: error("Vector not found")
        pixels.forEach { pixelLabel ->
            val (x, y) = pixelLabel.point
            getNeighborLocations(x, y, 1, image.width, image.height).forEach { (nx, ny) ->
                val nLabel = labels[nx][ny].label
                if (nLabel != label) {
                    val neighborVector = vectorMap[nLabel] ?: error("Neighbor vector not found")
                    if (neighborVector.cosineDistance(pixelsVector) >= threshold) {
                        set.add(nLabel)
                    }
                }
            }
        }

        return set
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