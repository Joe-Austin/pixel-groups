package net.joeaustin.extensions

import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_core.IplImage
import org.bytedeco.opencv.opencv_core.Mat
import java.awt.image.BufferedImage

fun Mat.toFrame(): Frame {
    val converter = OpenCVFrameConverter.ToMat()
    return converter.convert(this)
}

fun Mat.toIplImage(): IplImage {
    val converter = OpenCVFrameConverter.ToIplImage()
    return converter.convertToIplImage(this.toFrame())
}

fun Mat.toBufferedImage(): BufferedImage {
    val converter = Java2DFrameConverter()
    return converter.getBufferedImage(this.toFrame())
}