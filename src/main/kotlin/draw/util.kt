package com.github.hatoyuze.restarter.draw

import org.jetbrains.skia.*
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


fun Surface.drawStringCentral(content: String, font: Font, y: Float, paint: Paint): Rect {
    val measure = font.measureText(content)
    val textX = (width - (measure.right - measure.left)) / 2
    canvas.drawString(content, textX, y, font, paint)
    return measure
}

/**
 * the color of [initFont] will be changed to [backgroundColor4f]
 * */
fun Canvas.drawRRectWithEdge(
    backgroundColor4f: Color4f,
    edgeColor4f: Color4f = Color4f(0xFF_FF_FF_FF.toInt()),
    rRect: RRect,
    initFont: Paint = Paint {
        color4f = edgeColor4f
        isAntiAlias = true
    }
) {


    val edgePaint = initFont.apply {
        strokeWidth = 4f
        mode = PaintMode.STROKE
    }
    drawRRect(rRect, edgePaint)

    val backgroundPaint = initFont.apply {
        color4f = backgroundColor4f
        mode = PaintMode.FILL
    }
    drawRRect(rRect, backgroundPaint)
}