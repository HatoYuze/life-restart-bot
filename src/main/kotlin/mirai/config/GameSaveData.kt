package com.github.hatoyuze.restarter.mirai.config

import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.game.entity.impl.Life
import com.github.hatoyuze.restarter.game.entity.impl.LifeSave
import com.github.hatoyuze.restarter.mirai.config.GameConfig.enableGameSave
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.PluginDataHolder
import net.mamoe.mirai.console.data.PluginDataStorage
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.User
import java.time.Instant
import kotlin.time.Duration.Companion.hours

object GameSaveData: AutoSavePluginData("life") {
    val data: MutableList<LifeSave> by value(mutableListOf())

    fun save(life: Life, context: User? = null) {
        if (!enableGameSave) return
        LifeSave.translate(life, context)
    }

    @ConsoleExperimentalApi
    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
        super.onInit(owner, storage)

        if (GameConfig.dataExpireTime == -1|| !enableGameSave) return

        PluginMain.launch(CoroutineName("GameSaveData.checkExpire")) {
            val dataExpireTime = GameConfig.dataExpireTime.hours
            delay(dataExpireTime.div(2))

            val currentTimeUTC = Instant.now().toEpochMilli()
            val maxTime = dataExpireTime.inWholeMilliseconds

            val itr = data.iterator()
            while (itr.hasNext()) {
                val next = itr.next()
                if (next.createAtTimestampUTC + maxTime <= currentTimeUTC) {
                    PluginMain.logger.info("一项评分为 ${next.score} 且 创立于 ${next.createAtTimestampUTC} 的存档已过期，已自动删除")
                    itr.remove()
                }
            }
        }
    }
}