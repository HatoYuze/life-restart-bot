package com.github.hatoyuze.restarter.mirai.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object GameConfig : AutoSavePluginConfig("game") {
    @ValueDescription("最大总属性点，用户分配的属性点最终一定为该值（默认为20）")
    val maxAttributePoint: Int by value(20)
}