package net.joeaustin.extensions

import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage

fun BufferedImage.toFrame() : Frame {
    val converter = Java2DFrameConverter()
    return converter.getFrame(this)
}