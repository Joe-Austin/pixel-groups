package net.joeaustin

import us.hebi.matlab.mat.format.Mat5
import us.hebi.matlab.mat.types.MatFile

import us.hebi.matlab.mat.types.Sources


fun main() {
    val mat: MatFile = Mat5.newReader(Sources.openFile("data/mat.mat")).readMat()
    val gtClass = mat.entries.first() //There can be only one

    for (entry in mat.entries) {
        println(entry)
    }
}