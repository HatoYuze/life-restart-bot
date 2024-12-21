package com.github.hatoyuze.restarter.draw

import com.github.hatoyuze.restarter.game.data.Talent
import org.jetbrains.skia.*


class TalentLayoutDrawer(
    private val surface: Surface,
    private val font: Font,
    val talents: List<Talent>
) {
    private val canvas = surface.canvas
    private val boxWidth = (surface.width * BOX_WIDTH_RADIO).toFloat()
    private val boxHeight = (surface.height * BOX_HEIGHT_RADIO).toFloat()

    private val boxPositionX = (surface.width - boxWidth) / 2.0f

    fun draw() {
        fillBackground()
        drawTitle()

        var y = 60f
        talents.onEachIndexed { index, talent ->
            y += boxHeight + 20
            drawBox(y, talent, index + 1)
        }

    }

    private fun fillBackground() {
        val paint = Paint {
            color4f = Color4f(GameLayoutDrawer.BACKGROUND_COLOR_4F)
        }
        val rect = Rect.makeWH(surface.width.toFloat(), surface.height.toFloat())
        canvas.drawRect(rect, paint)
    }

    private fun drawTitle() {
        fun Canvas.drawStringCentral(content: String, font: Font, y: Float, paint: Paint): Rect {
            val measure = font.measureText(content)
            val textX = (surface.width - (measure.right - measure.left)) / 2
            drawString(content, textX, y, font, paint)
            return measure
        }

        val paint = Paint {
            color = Color.WHITE
            isAntiAlias = true
        }
        val font = font.also { it.size = 40f }
        canvas.drawStringCentral("天赋抽卡", font, 75f, paint)

        font.size = 25f
        val height = canvas.drawStringCentral(
            "请在抽取的随机天赋中共选择3个",
            font,
            surface.height.toFloat() - 150f,
            paint
        ).height
        canvas.drawStringCentral(
            "输入对应的序号选择，数字间由逗号相隔(如: 1,2,3)",
            font,
            surface.height.toFloat() - 165f - height,
            paint
        )
    }

    private fun drawBox(y: Float, talent: Talent, id: Int) {
        val textBaseline = y + boxHeight - 15f
        fun drawStringFittedBoxCentral(font0: Font, content: String, paint: Paint) {
            var fontSize = 24.0f
            val font = font0.also { it.size = fontSize }
            var textWidth = font.measureTextWidth(content)
            val desiredTextWidth = boxWidth - 20f
            while (textWidth > desiredTextWidth && fontSize >= 1.0f) {
                fontSize -= 0.3f
                font.size = fontSize
                textWidth = font.measureTextWidth(content)
            }

            val textX = (boxPositionX + (boxWidth - textWidth) / 2)
            canvas.drawString(content, textX, textBaseline, font, paint)
        }

        val range = RRect.makeXYWH(boxPositionX, y, boxWidth, boxHeight, 10f)
        val initFont = Paint {
            color = Color.WHITE
            isAntiAlias = true
        }

        canvas.drawRRectWithEdge(
            backgroundColor4f = Color4f(talentsGradeColor4F[talent.grade]),
            rRect = range
        )

        val font = font.apply {
            size = 24f
            edging = FontEdging.ANTI_ALIAS
        }
        val textPaint = initFont.apply {
            color = Color.WHITE
        }
        canvas.drawString("#$id ", boxPositionX + 5f, textBaseline, font, textPaint)

        drawStringFittedBoxCentral(font, "${talent.name}(${talent.description})", textPaint)
    }

    companion object {
        const val BOX_WIDTH_RADIO = 570.0 / 985.0
        const val BOX_HEIGHT_RADIO = 44.0 / 1034.0
        private val talentsGradeColor4F = listOf(
            //grade = 0 无色
            0x00_000000,
            //grade = 1 蓝色
            0Xff_6494EC.toInt(),
            //grade = 2 紫色
            0xff_E5BDFD.toInt(),
            //grade = 3 橙色
            0xff_FE7878.toInt()
        )
    }
}