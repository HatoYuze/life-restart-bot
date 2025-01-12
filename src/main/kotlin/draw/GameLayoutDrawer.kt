package com.github.hatoyuze.restarter.draw

import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.entity.ILife
import com.github.hatoyuze.restarter.mirai.ResourceManager
import com.github.hatoyuze.restarter.mirai.ResourceManager.newCacheFile
import com.github.hatoyuze.restarter.mirai.config.GameConfig.defaultFont
import com.github.hatoyuze.restarter.mirai.config.GameConfig.quality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.io.File

object GameLayoutDrawer {

    suspend fun createTalentOptionImage(talents: List<Talent>): File {
        return createImage(newCacheFile("talents-${talents.hashCode()}.png"), width = 985, height = 1034) {
            TalentLayoutDrawer(this, font, talents).draw()
        }
    }

    suspend fun createGamingImage(life: ILife): File {
        val surface = GameProgressLayoutDrawer(font, life).draw()
        return newCacheFile("life-${life.hashCode()}.jpg").also {
            val code = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, quality) ?: error("无法渲染图片！")
            withContext(Dispatchers.IO) {
                it.writeBytes(code.bytes)
            }
        }
    }

    // static resource. shouldn't be closed
    val font by lazy {
        val inputStream =
            if (ResourceManager.isTesting) {
                ResourceManager.getResourceAsStream("font/harmony/HarmonyOS_Sans_SC_Regular.ttf")
            } else {
                if (defaultFont.isEmpty()) {
                    PluginMain.getResourceAsStream("font/harmony/HarmonyOS_Sans_SC_Regular.ttf") ?: error("Cannot find font resource!")
                }else File(defaultFont).inputStream()
            }
        Font(
            FontMgr.default.makeFromData(
                Data.makeFromBytes(inputStream.readBytes())
            ) ?: error("Font data is not recognized")
        )
    }

    var enableSegoeEmoji: Boolean = false
        private set
    val emojiFont by lazy {
        // https://learn.microsoft.com/zh-cn/typography/font-list/segoe-ui-emoji
        if (hostOs == OS.Windows && System.getProperty("os.version").toDouble() >= 10) {
            enableSegoeEmoji = true
            return@lazy Font(FontMgr.default.matchFamily("Segoe UI Emoji").getTypeface(0))
        }
        val inputStream =
            if (ResourceManager.isTesting) {
                ResourceManager.getResourceAsStream("font/noto-emoji/NotoColorEmoji.ttf")
            } else {
                PluginMain.getResourceAsStream("font/noto-emoji/NotoColorEmoji.ttf")
                    ?: error("Cannot find font resource!")
            }
        Font(
            FontMgr.default.makeFromData(
                Data.makeFromBytes(inputStream.readBytes())
            ) ?: error("Font data is not recognized")
        )
    }


    val fontChineseLetterWidth = font.apply { size = 24f }.measureTextWidth("我")

    const val BACKGROUND_COLOR_4F = 0xFF_222831.toInt()

}