package com.github.hatoyuze.restarter.draw

import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.entity.Life
import com.github.hatoyuze.restarter.mirai.ResourceManager
import com.github.hatoyuze.restarter.mirai.ResourceManager.newCacheFile
import com.github.hatoyuze.restarter.mirai.config.GameConfig
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import java.io.File

object GameLayoutDrawer {

    suspend fun createTalentOptionImage(talents: List<Talent>): File {
        return createImage(newCacheFile("talents-${talents.hashCode()}.png"), width = 985, height = 1034) {
            TalentLayoutDrawer(this, font, talents).draw()
        }
    }

    fun createGamingImage(life: Life): File {
        val surface = GameProgressLayoutDrawer(font, life).draw()
        return newCacheFile("life-${life.hashCode()}.png").also {
            it.writeBytes(
                surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)?.bytes ?: byteArrayOf(0)
            )
        }
    }

    // static resource. shouldn't be closed
    val font by lazy {
        val inputStream =
            if (ResourceManager.isTesting) {
                ResourceManager.getResourceAsStream("font/HarmonyOS_Sans_SC_Regular.ttf")
            } else {
                GameConfig.defaultFont?.let { File(it).inputStream() }
                    ?: PluginMain.getResourceAsStream("font/HarmonyOS_Sans_SC_Regular.ttf")
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