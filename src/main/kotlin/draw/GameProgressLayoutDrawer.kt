package com.github.hatoyuze.restarter.draw

import com.github.hatoyuze.restarter.game.entity.ExecutedEvent
import com.github.hatoyuze.restarter.game.entity.ExecutedEvent.Companion.linesCount
import com.github.hatoyuze.restarter.game.entity.ExecutedEvent.Companion.maxGrade
import com.github.hatoyuze.restarter.game.entity.ExecutedEvent.Companion.showPostEvent
import com.github.hatoyuze.restarter.game.entity.ILife
import com.github.hatoyuze.restarter.game.entity.LifeAttribute
import com.github.hatoyuze.restarter.mirai.config.GameConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.jetbrains.skia.*
import org.jetbrains.skia.RRect.Companion.makeXYWH

class GameProgressLayoutDrawer(
    private val font0: Font,
    val life0: ILife,
    val coroutineScope: CoroutineScope
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
    private val textWeight = defaultFont.measureTextWidth("第 500 岁 ")
    private val topSurface by lazy {
        val topLife = pagedLife.first()
        fun getLifeLines(): Int = topLife.sumOf { it.linesCount() }
        Surface.makeRasterN32Premul(
            1000,
            (getLifeLines() * (textLineHeight + 10) + topLife.size * 45 + INIT_EVENT_MESSAGE_Y + 50).toInt()
        )
    }


    private val life = life0.toList().map { it.warpString() }
    private val pagedLife = if (life.size > 105 && GameConfig.isPaginatedEnable) life.chunked(101) else listOf(life)
    private val topCanvas = topSurface.canvas
    private var lastY = INIT_EVENT_MESSAGE_Y

    private val globalToDrawAttributes = mutableMapOf<Float, LifeAttribute>()

    private val paint = Paint {
        color = Color.WHITE
        isAntiAlias = true
    }

    private fun drawBackground(surface: Surface, initTop: Float = INIT_EVENT_MESSAGE_Y) {
        surface.canvas.clear(BACKGROUND_COLOR4F)

        surface.canvas.drawRRectWithEdge(
            backgroundColor4f = Color4f(EVENT_BACKGROUND_COLOR4F),
            rRect = makeXYWH(
                30f,
                initTop - 3f,
                surface.width - 1.5f * MESSAGE_START_X,
                surface.height - initTop - 50f,
                15f
            )
        )
    }

    private fun drawTitle() {
        val paint = Paint {
            color = Color.WHITE
            isAntiAlias = true
        }
        topSurface.drawStringCentral("人生模拟器", titleFont, 75f, paint)
        paint.strokeWidth = 5f
        paint.mode = PaintMode.FILL

        topCanvas.drawLine(50f, 75f + 15f, 1000f - 50f, 75f + 15f, paint)
        paint.strokeWidth = 0f
    }

    private fun drawSelectedTalent() {
        val drawer = TalentLayoutDrawer(
            topSurface, font0, life0.talents, surfaceRange = Point(1000f, 1034f)
        )
        var y = 80f

        topSurface.drawStringCentral("天赋列表", subTitleFont, 130f, paint)

        life0.talents.onEach { talent ->
            y += drawer.boxHeight + 20
            drawer.drawBox(y, talent, -1)
        }
    }

    suspend fun draw(): List<Surface> {
        drawBackground(topSurface)
        drawTitle()
        drawSelectedTalent()

        lastY += 10f

        val results = mutableListOf<Deferred<Surface>>()
        for ((groupIdx, pagedElement) in pagedLife.withIndex()) {
            if (groupIdx == 0) {
                results.add(
                    EventDrawer(
                        rangedLife = pagedElement,
                        localSurface = topSurface,
                        coroutineScope = coroutineScope,
                        drawingY = lastY,
                        enableCoverBackground = false,
                        groupIndex = 0
                    ).draw()
                )
                continue
            }
            results.add(
                EventDrawer(pagedElement, groupIndex = groupIdx, coroutineScope = coroutineScope).draw()
            )
        }

        return results.map {
            it.await()
        }
    }

    inner class EventDrawer(
        val rangedLife: List<ExecutedEvent>,
        val localSurface: Surface = Surface.makeRasterN32Premul(
            1000,
            (rangedLife.sumOf { it.linesCount() } * (textLineHeight + 10) + rangedLife.size * 45 + 50).toInt()
        ),
        val coroutineScope: CoroutineScope,
        var drawingY: Float = 25f,
        val enableCoverBackground: Boolean = true,
        val paint: Paint = Paint{
            color = Color.WHITE
            isAntiAlias = true
        },
        groupIndex: Int,
    ) {
        val baseAge = groupIndex * 100
        private val toDrawAttributes = mutableMapOf<Float, LifeAttribute>()

        private fun drawAttributes(
            canvas: Canvas = localSurface.canvas,
            toDrawAttributes: MutableMap<Float, LifeAttribute> = globalToDrawAttributes
        ) {
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
                        "$APPEARANCE_EMOJI ${attributes.appearance.padStart()} |" +
                            "$INTELLIGENT_EMOJI ${attributes.intelligent.padStart()} |" +
                            "$STRENGTH_EMOJI ${attributes.strength.padStart()} |" +
                            "$MONEY_EMOJI ${attributes.money.padStart()} |" +
                            "$SPIRIT_EMOJI ${attributes.spirit.padStart()}",
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
                    "$NOTO_COLOR_EMOJI_PAD${attributes.appearance.padStart()} | " +
                        "$NOTO_COLOR_EMOJI_PAD${attributes.intelligent.padStart()} | " +
                        "$NOTO_COLOR_EMOJI_PAD${attributes.strength.padStart()} | " +
                        "$NOTO_COLOR_EMOJI_PAD${attributes.money.padStart()} | " +
                        "$NOTO_COLOR_EMOJI_PAD${attributes.spirit.padStart()}",
                    MESSAGE_START_X,
                    y - 5f,
                    defaultFont,
                    paint
                )

            }
        }

        private fun drawNextEntry(
            age: Int,
            currentEvent: ExecutedEvent,
            canvas: Canvas = localSurface.canvas,
            toDrawAttributes: MutableMap<Float, LifeAttribute> = globalToDrawAttributes
        ) {
            drawingY += 20
            toDrawAttributes[drawingY] = currentEvent.attribute
            val messageLineStartX = MESSAGE_START_X + textWeight

            fun drawEventLine(eventName: String, isAppendLine: Boolean = true) {
                var isFirst = true
                eventName.split('\n').onEach {
                    if (isAppendLine || !isFirst)
                        drawingY += textLineHeight + 10
                    canvas.drawString(it, messageLineStartX, drawingY, defaultFont, paint)
                    isFirst = false
                }
            }

            val heightRange = currentEvent.linesCount() * (textLineHeight + 10)

            // Message background
            canvas.drawRRectWithEdge(
                backgroundColor4f = Color4f(eventGradeColor4F[currentEvent.maxGrade()]),
                rRect = makeXYWH(30f, drawingY, localSurface.width - 1.5f * MESSAGE_START_X, heightRange + 15, 3f),
                initFont = paint
            )
            paint.color4f = Color4f(Color.WHITE)
            drawingY += textLineHeight + 10
            canvas.drawString("第 $age 岁", MESSAGE_START_X, drawingY, defaultFont, paint)
            drawEventLine(currentEvent.mainEvent.eventName, false)

            if (currentEvent.showPostEvent()) {
                drawEventLine(currentEvent.mainEvent.postEvent!!)
            }

            currentEvent.subEvents.onEach {
                drawEventLine(it.eventName)
            }

            drawingY += 25
        }

        fun draw(): Deferred<Surface> =
            coroutineScope.async {
                if (enableCoverBackground) {
                    drawBackground(localSurface,drawingY)
                }

                for ((idx, data) in rangedLife.withIndex()) {
                    drawNextEntry(
                        age = baseAge + idx, data,
                        localSurface.canvas, toDrawAttributes
                    )
                }
                drawAttributes(localSurface.canvas, toDrawAttributes)
                this@EventDrawer.localSurface
            }

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