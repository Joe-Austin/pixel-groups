package net.joeaustin.extensions

import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_core.Mat

fun Frame.toMat () : Mat {
    val converter = OpenCVFrameConverter.ToMat()
    return converter.convertToMat(this)
}