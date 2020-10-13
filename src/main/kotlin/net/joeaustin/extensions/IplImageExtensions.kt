package net.joeaustin.extensions

import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_core.IplImage
import org.bytedeco.opencv.opencv_core.Mat

fun IplImage.toMat(): Mat {
    val converter = OpenCVFrameConverter.ToMat()
    return converter.convertToMat(this.toFrame())

}

fun IplImage.toFrame(): Frame {
    val converter = OpenCVFrameConverter.ToMat()
    return converter.convert(this)
}