package net.joeaustin

import net.joeaustin.data.Pixel
import net.joeaustin.data.PixelLabel
import net.joeaustin.data.with
import net.joeaustin.extensions.toBufferedImage
import net.joeaustin.extensions.toFrame
import net.joeaustin.utilities.getPixelLabelMapByHueAverage
import net.joeaustin.utilities.overlayPixelBoundsOnImage
import org.bytedeco.opencv.global.opencv_core.CV_32SC1
import org.bytedeco.opencv.global.opencv_imgcodecs.imread
import org.bytedeco.opencv.global.opencv_ximgproc.*
import org.bytedeco.opencv.opencv_core.Mat
import java.io.File
import java.nio.ByteBuffer
import java.nio.IntBuffer

fun main() {
    val input = File("data/goat.jpg")
    val output = File("output/${input.nameWithoutExtension}-slic.png")

    val imageMat = imread(input.path)
    //val slic = createSuperpixelSLIC(imageMat, SLIC, 30, 500f)
    val slic = createSuperpixelSLIC(imageMat, SLICO, 10, 10f)
    //val slic = createSuperpixelSLIC(imageMat)

    //val labelMat = Mat(imageMat.rows(), imageMat.cols(), CV_32SC1)
    val labelMat = Mat(imageMat.rows(), imageMat.cols(), CV_32SC1)

    slic.getLabels(labelMat)
    println("SP Count: ${slic.numberOfSuperpixels}")
    println(labelMat)
    val buffer = labelMat.asByteBuffer()

    //val data = labelMat.arrayData()
    //val data = labelMat.asByteBuffer()
    //val data = labelMat.asByteBuffer()
    //val intBuffer = data.asIntBuffer()

    val data = labelMat.toFrame().image[0] as IntBuffer
    val img = imageMat.toBufferedImage()

    val frame = labelMat.toFrame()
    println(frame.image[0])


    println("${labelMat.arrayWidth()} x ${labelMat.arrayHeight()}")

    val labels = Array(img.width) { x ->
        Array(img.height) { y ->
            val pos = (y * img.width) + x
            //val label = data.getInt(pos)
            val label = data.get(pos)
            //println(label)
            val pixel = Pixel.fromInt(img.getRGB(x, y))
            PixelLabel(pixel, label, x with y)
        }
    }

    overlayPixelBoundsOnImage(img, labels, output)
    println(output.toPath().toUri())

}