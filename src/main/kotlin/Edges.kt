import net.joeaustin.extensions.toIplImage
import net.joeaustin.extensions.toMat
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.global.opencv_core.*
import org.bytedeco.opencv.global.opencv_imgcodecs.imread
import org.bytedeco.opencv.global.opencv_imgcodecs.imwrite
import org.bytedeco.opencv.global.opencv_imgproc.*
import java.io.File

fun main() {

    val inputFile = File("data/hair1.jpg")
    val outputFile = File("output/${inputFile.nameWithoutExtension}-canny.png")

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