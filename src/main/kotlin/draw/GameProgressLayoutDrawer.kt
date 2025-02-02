package com.github.hatoyuze.restarter.draw

import com.github.hatoyuze.restarter.game.entity.ExecutedEvent
import com.github.hatoyuze.restarter.game.entity.ExecutedEvent.Companion.linesCount
import com.github.hatoyuze.restarter.game.entity.ExecutedEvent.Companion.maxGrade
import com.github.hatoyuze.restarter.game.entity.ExecutedEvent.Companion.showPostEvent
import com.github.hatoyuze.restarter.game.entity.ILife
import com.github.hatoyuze.restarter.game.entity.LifeAttribute
import org.jetbrains.skia.*
import org.jetbrains.skia.RRect.Companion.makeXYWH

class GameProgressLayoutDrawer(
    private val font0: Font,
    val life0: ILife
) {

    private val titleFont by lazy { font0.apply { size = 40f } }
    private val defaultFont by lazy {
        font0.apply {
            size = FONT_DEFAULT_SIZE
            edging = FontEdging.ANTI_ALIAS
        }
    }
    private val emojiTextDescFont by lazy {
        font0.apply {
            size = 18f
        }
    }
    private val subTitleFont by lazy { font0.apply { size = 30f } }

    private val textLineHeight by lazy { defaultFont.measureText("你出生了。").let { it.bottom - it.top } }
    private val textWeight = defaultFont.measureTextWidth("第 500 岁")
    private val surface by lazy {
        fun getLifeLines(): Int = life.sumOf { it.linesCount() }
        Surface.makeRasterN32Premul(
            1000,
            (getLifeLines() * (textLineHeight + 10) + life.size * 45 + INIT_EVENT_MESSAGE_Y + 50).toInt()
        )
    }

    private val life = life0.toList().map { it.warpString() }
    private val canvas = surface.canvas
    private var lastY = INIT_EVENT_MESSAGE_Y

    private val toDrawAttributes = mutableMapOf<Float, LifeAttribute>()

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
        surface.drawStringCentral("人生模拟器", titleFont, 75f, paint)
        paint.strokeWidth = 5f
        paint.mode = PaintMode.FILL

        canvas.drawLine(50f, 75f + 15f, 1000f - 50f, 75f + 15f, paint)
        paint.strokeWidth = 0f
    }

    private fun drawSelectedTalent() {
        val drawer = TalentLayoutDrawer(
            surface, font0, life0.talents, surfaceRange = Point(1000f, 1034f)
        )
        var y = 80f

        surface.drawStringCentral("天赋列表", subTitleFont, 130f, paint)

        life0.talents.onEach { talent ->
            y += drawer.boxHeight + 20
            drawer.drawBox(y, talent, -1)
        }
    }

    fun draw(): Surface {
        drawBackground()
        drawTitle()
        drawSelectedTalent()

        lastY += 10f
        for ((index, event) in life.withIndex()) {
            drawNextEntry(index, event)
        }
        drawAttributes()

        return surface
    }

    private fun drawAttributes() {
        fun Int.padStart() = toString().padStart(3, ' ')
        val font = GameLayoutDrawer.emojiFont
        font.size = 18f

        lateinit var defaultFont: Font
        if (!GameLayoutDrawer.enableSegoeEmoji) {
            defaultFont = emojiTextDescFont
        }

        paint.color = Color.WHITE
        for ((y, attributes) in toDrawAttributes) {

            // NOTE: Segoe Emoji 字体支持非 emoji 字体的渲染，可以直接输入文字
            //       而使用 NotoColorEmoji 则与此不同，使用 NotoColorEmoji 无法渲染出正常的文字/数字，需要分开渲染
            if (GameLayoutDrawer.enableSegoeEmoji) {
                canvas.drawString(
                        "$APPEARANCE_EMOJI ${attributes.appearance.padStart()  } |" +
                        "$INTELLIGENT_EMOJI ${attributes.intelligent.padStart()} |" +
                        "$STRENGTH_EMOJI ${attributes.strength.padStart()      } |" +
                        "$MONEY_EMOJI ${attributes.money.padStart()            } |" +
                        "$SPIRIT_EMOJI ${attributes.spirit.padStart()          }",
                    MESSAGE_START_X, y - 5f, font, paint
                )
                continue
            }

            // NOTE: NotoColorEmoji 会将所有的半角空格渲染为全角空格形式，空隙极大。以下数据不建议更改
            canvas.drawString(
                    "$APPEARANCE_EMOJI  |" +
                    "$INTELLIGENT_EMOJI  |" +
                    "$STRENGTH_EMOJI  |" +
                    "$MONEY_EMOJI  |" +
                    "$SPIRIT_EMOJI  ",
                MESSAGE_START_X,
                y - 5f,
                font,
                paint
            )
            canvas.drawString(
                    "$NOTO_COLOR_EMOJI_PAD${attributes.appearance.padStart() } | " +
                    "$NOTO_COLOR_EMOJI_PAD${attributes.intelligent.padStart()} | " +
                    "$NOTO_COLOR_EMOJI_PAD${attributes.strength.padStart()   } | " +
                    "$NOTO_COLOR_EMOJI_PAD${attributes.money.padStart()      } | " +
                    "$NOTO_COLOR_EMOJI_PAD${attributes.spirit.padStart()     }",
                MESSAGE_START_X,
                y - 5f,
                defaultFont,
                paint
            )

        }
    }

    private fun drawNextEntry(age: Int, currentEvent: ExecutedEvent) {
        lastY += 20
        toDrawAttributes[lastY] = currentEvent.attribute
        val messageLineStartX = MESSAGE_START_X + textWeight

        fun drawEventLine(eventName: String, isAppendLine: Boolean = true) {
            var isFirst = true
            eventName.split('\n').onEach {
                if (isAppendLine || !isFirst)
                    lastY += textLineHeight + 10
                canvas.drawString(it, messageLineStartX, lastY, defaultFont, paint)
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
        canvas.drawString("第 $age 岁", MESSAGE_START_X, lastY, defaultFont, paint)
        drawEventLine(currentEvent.mainEvent.eventName, false)

        if (currentEvent.showPostEvent()) {
            drawEventLine(currentEvent.mainEvent.postEvent!!)
        }

        currentEvent.subEvents.onEach {
            drawEventLine(it.eventName)
        }

        lastY += 25
    }

    companion object {
        const val INIT_EVENT_MESSAGE_Y = 350f
        const val MESSAGE_START_X = 40f
        const val EVENT_BACKGROUND_COLOR4F = 0xFF_383D45.toInt()
        const val BACKGROUND_COLOR4F = 0xFF_222831.toInt()

        private const val FONT_DEFAULT_SIZE = 24f
        private const val NOTO_COLOR_EMOJI_PAD = "\t\t\t"
        private const val APPEARANCE_EMOJI = "\uD83D\uDE0E"
        private const val INTELLIGENT_EMOJI = "\uD83E\uDDE0"
        private const val STRENGTH_EMOJI = "\uD83D\uDCAA"
        private const val MONEY_EMOJI = "\uD83D\uDCB5"
        private const val SPIRIT_EMOJI = "\uD83E\uDD70"

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
                subEvents.map { it.copy(eventName = it.eventName.wrapString()) },
                attribute
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