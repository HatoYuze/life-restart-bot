package com.github.hatoyuze.restarter.draw

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import java.io.File


inline fun Paint(block: Paint.() -> Unit) = Paint().apply(block)


suspend fun createImage(output: File, width: Int, height: Int, block: suspend Surface.() -> Unit): File {
    val surface = Surface.makeRasterN32Premul(
        width, height
    ).apply {
        this.block()
    }
    output.outputStream().use {
        it.write(
            surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes
        )
    }
    surface.close()

    return output
}