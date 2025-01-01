package com.github.hatoyuze.restarter.mirai.config

import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.game.entity.impl.Life
import com.github.hatoyuze.restarter.game.entity.impl.LifeSave
import com.github.hatoyuze.restarter.mirai.config.CommandLimitData.dailyUserRecord
import com.github.hatoyuze.restarter.mirai.config.CommandLimitData.rateLimiting
import com.github.hatoyuze.restarter.mirai.config.GameConfig.enableGameSave
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.PluginDataHolder
import net.mamoe.mirai.console.data.PluginDataStorage
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.User
import java.time.Duration.between
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

object GameSaveData: AutoSavePluginData("life") {
    @Serializable
    data class Data(
        @SerialName("I")
        val id: Int,
        @SerialName("S")
        val content: LifeSave
    )
    private val enableDataRemover = GameConfig.dataExpireTime >= 0 && enableGameSave

    val data: MutableList<Data> by value(mutableListOf())

    private var lastId: Int by value(100)


    fun save(life: Life, context: User? = null): Optional<Int> {
        if (!enableGameSave) return Optional.empty()
        lastId++
        data.add(
            Data(
                id = lastId,
                content = LifeSave.translate(life, context)
            )
        )
        return Optional.of(lastId)
    }

    fun timeFilter(duration: Duration): List<Data> {
        val currentTimeUTC = Instant.now().toEpochMilli()
        val startTime = currentTimeUTC - duration.inWholeMilliseconds
        return data.filter { it.content.createAtTimestampUTC >= startTime }
    }

    @ConsoleExperimentalApi
    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
        super.onInit(owner, storage)

        val now = ZonedDateTime.now(ZoneId.of("UTC+8"))
        val midnight = now.plusDays(1)
            .minusHours(now.hour.toLong()).minusMinutes(now.minute.toLong()).minusSeconds(now.second + 2L)

        val duration = between(now, midnight).toMillis()
        PluginMain.launch(CoroutineName("GameSaveData.checkExpire")) {
            delay(duration)
            while (isActive) {
                changeData()

                delay(1.days)
            }
        }
    }

    private fun changeData() {
        dailyUserRecord.clear()
        rateLimiting.clear()

        if (!enableDataRemover) return
        val dataExpireTime = GameConfig.dataExpireTime.hours
        val currentTimeUTC = Instant.now().toEpochMilli()
        val maxTime = dataExpireTime.inWholeMilliseconds

        val itr = data.iterator()
        while (itr.hasNext()) {
            val next = itr.next().content
            if (next.createAtTimestampUTC + maxTime <= currentTimeUTC) {
                PluginMain.logger.info("一项评分为 ${next.score} 且 创立于 ${next.createAtTimestampUTC} 的存档已过期，已自动删除")
                itr.remove()
            }
        }
    }
}