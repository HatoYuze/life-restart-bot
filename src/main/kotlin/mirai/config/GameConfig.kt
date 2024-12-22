package com.github.hatoyuze.restarter.mirai.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object GameConfig : AutoSavePluginConfig("game") {
    @ValueDescription("最大总属性点，用户分配的属性点最终一定为该值（默认为20）")
    val maxAttributePoint: Int by value(20)

    @ValueDescription("绘图使用的字体，如需自定义需要在此处填写字体文件的绝对路径\n默认下使用 HarmonyOS Sans Regular 字体")
    val defaultFont: String by value("")

    @ValueDescription("临时存储文件的目录\n在插件退出时，这些图片会被清除, 为空时表示插件的 data 目录")
    val cachePath: String by value("")


    fun String?.ifNull(replacement: String) = if (this.isNullOrEmpty()) replacement else this
}