package net.joeaustin.utilities

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.joeaustin.data.PixelLabel
import net.joeaustin.data.VectorN
import superpixel.VisionPixels
import java.util.concurrent.ConcurrentHashMap
import kotlin.RuntimeException

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


