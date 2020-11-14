package net.joeaustin.utilities

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.joeaustin.data.PixelLabel
import net.joeaustin.data.Point
import net.joeaustin.data.VectorN
import net.joeaustin.data.with
import superpixel.VisionPixels
import java.util.concurrent.ConcurrentHashMap
import kotlin.RuntimeException
import kotlin.math.max
import kotlin.math.min

fun findSimilarLabels(
    label: Int,
    labels: Array<Array<PixelLabel>>,
    threshold: Double = 0.95,
    useEntropy: Boolean = true
): Map<Int, List<PixelLabel>> {
    val labelGroups = labels.flatten().groupBy { it.label }
    val averagePixelMap = getPixelLabelMapByHueAverage(labels)
        .map { (label, averagePixel) ->
            val pixelGroup = labelGroups[label] ?: throw RuntimeException("Label $label not found?")
            val entropy = VisionPixels.computePixelGroupEntropy(pixelGroup.map { it.pixel })
            val p = if (useEntropy) averagePixel.toHxHySL().append(entropy) else averagePixel.toHxHySL()
            label to p
        }.toMap()

    val targetVector = averagePixelMap[label] ?: throw RuntimeException("Label $label not found")
    val similarGroups = averagePixelMap.filter { (l, v) ->
        l != label && v.cosineDistance(targetVector) >= threshold
    }

    return labelGroups.filter { (label, _) -> label in similarGroups }
}

fun computeLabelGraph(
    labels: Array<Array<PixelLabel>>,
    threshold: Double = 0.95,
    useEntropy: Boolean = true
): Map<Int, Set<Int>> = runBlocking {
    val labelGraph = ConcurrentHashMap<Int, Set<Int>>()
    val labelMap = labels.flatten().groupBy { it.label }
    val averagePixelMap = getPixelLabelMapByHueAverage(labels)
        .map { (label, averagePixel) ->
            if (useEntropy) {
                val pixels = labelMap[label] ?: throw RuntimeException("Label $label not found")
                val entropy = VisionPixels.computePixelGroupEntropy(pixels.map { it.pixel })
                label to averagePixel.toHxHySL().append(entropy)
            } else {
                label to averagePixel.toHxHySL()
            }
        }.toMap()

    averagePixelMap.forEach { (label, vector) ->
        launch {
            val hits = averagePixelMap.mapNotNull { (searchLabel, searchVector) ->
                if (searchLabel != label && searchVector.cosineDistance(vector) >= threshold) {
                    searchLabel
                } else {
                    null
                }
            }.toSet()

            labelGraph[label] = hits
        }
    }

    labelGraph
}

fun arePixelGroupsNeighbors(pixelGroup1: List<PixelLabel>, pixelGroup2: List<PixelLabel>): Boolean {
    val (smallerGroup, largerGroup) = if (pixelGroup1.size < pixelGroup2.size) {
        pixelGroup1 to pixelGroup2
    } else {
        pixelGroup2 to pixelGroup1
    }

    val largerGroupPoints = largerGroup.map { it.point }.toHashSet()

    return smallerGroup.any { pixelLabel ->
        val pt = pixelLabel.point
        getNeighborLocations(pt.x, pt.y, 1, Int.MAX_VALUE, Int.MAX_VALUE).any { npt ->
            npt in largerGroupPoints
        }
    }
}

fun getNeighborLocations(
    x: Int, y: Int, radius: Int,
    width: Int, height: Int
): List<Point> {
    val minX = max(0, x - radius)
    val minY = max(0, y - radius)

    val maxX = min(width - 1, x + radius)
    val maxY = min(height - 1, y + radius)

    val size = ((maxX - minX + 1) * (maxY - minY + 1)) - 1
    if (size < 0) {
        println("Uh-oh")
    }
    val neighbors = ArrayList<Point>(size)

    for (i in minX..maxX) {
        for (j in minY..maxY) {
            if (i != x || j != y) {
                neighbors.add(i with j)
            }
        }
    }

    return neighbors
}

fun getLabelEdges(label: Int, labels: Array<Array<PixelLabel>>, labelMap: Map<Int, List<PixelLabel>>): List<Point> {
    val width = labels.size
    val height = labels[0].size

    val labelPixels = labelMap[label] ?: error("Label not found")
    return labelPixels.mapNotNull { (_, _, pPt) ->
        val isEdge = getNeighborLocations(pPt.x, pPt.y, 1, width, height).any { (nx, ny) ->
            labels[nx][ny].label != label
        }

        if (isEdge) pPt else null
    }
}

fun getLabelEdges(label: Int, labels: Array<Array<PixelLabel>>): List<Point> {
    val labelMap = labels.flatten().groupBy { it.label }
    return getLabelEdges(label, labels, labelMap)
}

