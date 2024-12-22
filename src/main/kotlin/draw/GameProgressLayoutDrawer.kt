package com.github.hatoyuze.restarter.draw

import com.github.hatoyuze.restarter.game.entity.ExecutedEvent
import com.github.hatoyuze.restarter.game.entity.ExecutedEvent.Companion.linesCount
import com.github.hatoyuze.restarter.game.entity.ExecutedEvent.Companion.maxGrade
import com.github.hatoyuze.restarter.game.entity.Life
import com.github.hatoyuze.restarter.game.entity.Life.Companion.talents
import org.jetbrains.skia.*
import org.jetbrains.skia.RRect.Companion.makeXYWH

class GameProgressLayoutDrawer(
    private val font: Font,
    val life0: Life
) {

    private val textLineHeight by lazy { fontWithInit().measureText("你出生了。").let { it.bottom - it.top } }
    private val textWeight = fontWithInit().measureTextWidth("第 500 岁")
    private val surface by lazy {
        fun getLifeLines(): Int = life.sumOf { it.linesCount() }
        Surface.makeRasterN32Premul(
            1000,
            (getLifeLines() * (textLineHeight + 10) + life.size * 35 + INIT_EVENT_MESSAGE_Y + 50).toInt()
        )
    }

    private val life = life0.toList().map { it.warpString() }
    private val canvas = surface.canvas
    private var lastY = INIT_EVENT_MESSAGE_Y


    private val paint = Paint {
        color = Color.WHITE
        isAntiAlias = true
    }
    private fun drawBackground() {
        val paint = Paint {
            color4f = Color4f(BACKGROUND_COLOR4F)
        }
        val rect = Rect.makeWH(surface.width.toFloat(), surface.height.toFloat())
        canvas.drawRect(rect, paint)

        paint.color4f = Color4f(EVENT_BACKGROUND_COLOR4F)
        canvas.drawRRectWithEdge(
            backgroundColor4f = Color4f(EVENT_BACKGROUND_COLOR4F),
            rRect = makeXYWH(
                30f,
                INIT_EVENT_MESSAGE_Y,
                surface.width - 1.5f * MESSAGE_START_X,
                surface.height - INIT_EVENT_MESSAGE_Y - 50f,
                15f
            )
        )
    }

    private fun drawTitle() {
        val font = font.also { it.size = 40f }
        //85
        surface.drawStringCentral("人生模拟器", font, 75f, paint)
        paint.strokeWidth = 5f
        paint.mode = PaintMode.FILL

        canvas.drawLine(50f, 75f + 15f, 1000f - 50f, 75f + 15f, paint)
        paint.strokeWidth = 0f
        font.size = FONT_DEFAULT_SIZE
    }

    private fun drawSelectedTalent() {
        val drawer = TalentLayoutDrawer(
            surface, font, life0.talents, surfaceRange = Point(1000f, 1034f)
        )
        var y = 80f

        val font = font.also { it.size = 30f }
        surface.drawStringCentral("天赋列表", font, 130f, paint)

        life0.talents.onEach { talent ->
            y += drawer.boxHeight + 20
            drawer.drawBox(y, talent, -1)
        }
        font.size = FONT_DEFAULT_SIZE
    }

    fun draw(): Surface {
        drawBackground()
        drawTitle()
        drawSelectedTalent()

        lastY += 10f
        for ((index, event) in life.withIndex()) {
            drawNextEntry(index, event)
        }

        return surface
    }

    private fun drawNextEntry(age: Int, currentEvent: ExecutedEvent) {
        lastY += 20
        val messageLineStartX = MESSAGE_START_X + textWeight

        fun drawEventLine(eventName: String, isAppendLine: Boolean = true) {
            var isFirst = true
            eventName.split('\n').onEach {
                if (isAppendLine || !isFirst)
                    lastY += textLineHeight + 10
                canvas.drawString(it, messageLineStartX, lastY, font, paint)
                isFirst = false
            }
        }

        val heightRange = currentEvent.linesCount() * (textLineHeight + 10)

        // Message background
        canvas.drawRRectWithEdge(
            backgroundColor4f = Color4f(eventGradeColor4F[currentEvent.maxGrade()]),
            rRect = makeXYWH(30f, lastY, surface.width - 1.5f * MESSAGE_START_X, heightRange + 15, 3f),
            initFont = paint
        )
        paint.color4f = Color4f(Color.WHITE)
        lastY += textLineHeight + 10
        canvas.drawString("第 $age 岁", MESSAGE_START_X, lastY, font, paint)
        drawEventLine(currentEvent.mainEvent.eventName, false)

        currentEvent.subEvents.onEach {
            drawEventLine(it.eventName)
        }

        lastY += 15
    }


    private fun fontWithInit(): Font =
        font.also {
            it.size = FONT_DEFAULT_SIZE
            it.edging = FontEdging.ANTI_ALIAS
        }

    companion object {
        const val INIT_EVENT_MESSAGE_Y = 350f
        const val MESSAGE_START_X = 40f
        const val EVENT_BACKGROUND_COLOR4F = 0xFF_383D45.toInt()
        const val BACKGROUND_COLOR4F = 0xFF_222831.toInt()
        private const val FONT_DEFAULT_SIZE = 24f

        private val maxLetterCountOneLine = (800 / GameLayoutDrawer.fontChineseLetterWidth).toInt()
        private fun String.wrapString(): String {
            if (length <= maxLetterCountOneLine) return this
            val sb = StringBuilder(length + (length / maxLetterCountOneLine))
            var lineStart = 0

            for (i in indices) {
                if (i - lineStart == maxLetterCountOneLine) {
                    sb.append(substring(lineStart, i + 1)).append("\n")
                    lineStart = i + 1
                }
            }

            sb.append(substring(lineStart))
            return sb.toString()
        }

        fun ExecutedEvent.warpString(): ExecutedEvent {
            return ExecutedEvent(
                mainEvent.copy(eventName = mainEvent.eventName.wrapString()),
                subEvents.map { it.copy(eventName = it.eventName.wrapString()) }
            )
        }

        private val eventGradeColor4F = listOf(
            //grade = 0 灰色
            0XFF_454545.toInt(),
            //grade = 1 蓝色
            0Xff_6494EC.toInt(),
            //grade = 2 紫色
            0xff_E5BDFD.toInt(),
            //grade = 3 橙色
            0xff_FE7878.toInt()
        )
    }
}