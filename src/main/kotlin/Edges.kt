import net.joeaustin.data.*
import net.joeaustin.extensions.*
import org.bytedeco.opencv.global.opencv_core.*
import org.bytedeco.opencv.global.opencv_imgcodecs.imread
import org.bytedeco.opencv.global.opencv_imgcodecs.imwrite
import org.bytedeco.opencv.global.opencv_imgproc.*
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun main() {
    val inputFile = File("data/bs.jpg")
    val t = 0.5

    val image = ImageIO.read(inputFile)
    val blurredFile = File("output/${inputFile.nameWithoutExtension}-blurred.png")
    //val blurredImage = blurImage(image)
    val blurredImage = applyGaussianBlur(image)
    ImageIO.write(blurredImage, "PNG", blurredFile)
    println(blurredFile.toPath().toUri())


    val cannyOutput = File("output/${inputFile.nameWithoutExtension}-canny.png")
    val colorOutput = File("output/${inputFile.nameWithoutExtension}-colorEdge.png")


    performCannyEdgeDetection(inputFile, cannyOutput)
    performColorEdgeDetection(inputFile, colorOutput, t)
}

fun performCannyEdgeDetection(inputFile: File, outputFile: File) {
    val imgMat = imread(inputFile.absolutePath)
    val iplImg = imgMat.toIplImage()
    val grayImage = cvCreateImage(cvGetSize(iplImg), iplImg.depth(), 1)
    val cannyImage = cvCreateImage(cvGetSize(iplImg), iplImg.depth(), 1)

    cvCvtColor(iplImg, grayImage, CV_BGR2GRAY)
    cvSmooth(grayImage, grayImage, CV_GAUSSIAN, 7, 7, 0.0, 0.0)
    cvCanny(grayImage, cannyImage, 10.0, 300.0, 5);

    val outMat = cannyImage.toMat()

    imgMat.release()
    grayImage.release()
    cannyImage.release()

    imwrite(outputFile.absolutePath, outMat)
    outMat.release()
    println(outputFile.toPath().toUri())
    println("Done!")
}

fun applyGaussianBlur(image: BufferedImage): BufferedImage {
    val iplImg = image.toFrame().toMat().toIplImage()
    val smoothImage = cvCreateImage(cvGetSize(iplImg), iplImg.depth(), 3)
    cvSmooth(iplImg, smoothImage, CV_GAUSSIAN, 7, 7, 0.0, 0.0)

    iplImg.release()
    val returnImage = smoothImage.toMat().toBufferedImage()
    smoothImage.release()
    return returnImage
}

fun blurImage(image: BufferedImage): BufferedImage {
    val kernel = Matrix.of(
        arrayOf(0.0, -1.0, 0.0),
        arrayOf(-1.0, 5.0, -1.0),
        arrayOf(0.0, -1.0, 0.0),
    )

    return applyKernel(kernel, image, 1.0)
}

fun applyKernel(kernel: Matrix, image: BufferedImage, w: Double = 1.0): BufferedImage {
    val width = image.width
    val height = image.height

    val newImage = BufferedImage(width, height, TYPE_INT_RGB)

    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val (r, g, b) = getImageRegion(image, x with y).asRgbMatrices()
            val newR = ((r conv kernel) * w).roundToInt().coerceIn(0..255)
            val newG = ((g conv kernel) * w).roundToInt().coerceIn(0..255)
            val newB = ((b conv kernel) * w).roundToInt().coerceIn(0..255)
            val newPixel = Pixel(255, newR, newG, newB)
            newImage.setRGB(x, y, newPixel.toInt())
        }
    }

    return newImage
}

fun performColorEdgeDetection(inputFile: File, outputFile: File, threshold: Double = 1.0) {
    val white = 0xFFFFFFFF.toInt()

    val inputImage = ImageIO.read(inputFile)
    val width = inputImage.width
    val height = inputImage.height
    val outputImage = BufferedImage(width, height, TYPE_INT_RGB)

    val xKernel = Matrix.of(
        arrayOf(1.0, 0.0, -1.0),
        arrayOf(2.0, 0.0, -2.0),
        arrayOf(1.0, 0.0, -1.0),
    )

    val yKernel = Matrix.of(
        arrayOf(1.0, 2.0, 1.0),
        arrayOf(0.0, 0.0, 0.0),
        arrayOf(-1.0, -2.0, -1.0),
    )

    //Ignore the border for now
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val center = Pixel.fromInt(inputImage.getRGB(x, y)).toHxHySL()
            val region = getImageRegion(inputImage, x with y).map {
                it.map { p ->
                    p.toHxHySL().cosineDistance(center)
                }.toTypedArray()
            }.toTypedArray()

            val (mag, _) = applyKernels(region.asMatrix(), xKernel, yKernel)

            if (mag >= threshold) {
            //if (mag >= threshold) {
                outputImage.setRGB(x, y, white)
            }
        }
    }

    ImageIO.write(outputImage, "PNG", outputFile)
    println(outputFile.toPath().toUri())
}


private fun getImageRegion(image: BufferedImage, center: Point): Array<Array<Pixel>> {
    val (x, y) = center

    return arrayOf(
        arrayOf(
            Pixel.fromInt(image.getRGB(x - 1, y - 1)),
            Pixel.fromInt(image.getRGB(x - 0, y - 1)),
            Pixel.fromInt(image.getRGB(x + 1, y - 1))
        ),

        arrayOf(
            Pixel.fromInt(image.getRGB(x - 1, y)),
            Pixel.fromInt(image.getRGB(x - 0, y)),
            Pixel.fromInt(image.getRGB(x + 1, y))
        ),

        arrayOf(
            Pixel.fromInt(image.getRGB(x - 1, y + 1)),
            Pixel.fromInt(image.getRGB(x - 0, y + 1)),
            Pixel.fromInt(image.getRGB(x + 1, y + 1))
        )
    )

}

private fun applyKernels(
    values: Matrix,
    xKernel: Matrix,
    yKernel: Matrix
): Pair<Double, Double> {
    val xGradient = values conv xKernel
    val yGradient = values conv yKernel
    val magnitude = sqrt(xGradient.pow(2) + yGradient.pow(2))
    val angle = Math.toDegrees(atan(yGradient / xGradient))

    return magnitude to angle
}
