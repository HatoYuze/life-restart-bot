package com.github.hatoyuze.restarter.mirai.config

import com.github.hatoyuze.restarter.mirai.config.GameConfig.Limit.Companion.isNone
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.User
import kotlin.time.Duration.Companion.milliseconds

object CommandLimitData : AutoSavePluginData("command_limit_status") {
    val dailyUserRecord: MutableMap<Long, Int> by value(mutableMapOf())
    val rateLimiting: MutableMap<Long,Long> by value(mutableMapOf())


    data class RateLimitingData(
        val isWaiting: Boolean,
        val remainingSeconds: Int = -1
    ) {
        companion object {
            val NORMAL_STATUS = RateLimitingData(
                false, -1
            )
        }
    }

    fun CommandSender.checkCooldownStatus(): RateLimitingData {
        if (GameConfig.Limit.frequencyLimitSeconds.isNone()) return RateLimitingData.NORMAL_STATUS
        val contact = when(GameConfig.Limit.frequencyType) {
            GameConfig.Limit.ContactType.SUBJECT -> subject
            GameConfig.Limit.ContactType.SENDER -> user
        } ?: return RateLimitingData.NORMAL_STATUS
        val currentTime = System.currentTimeMillis()

        var isWaiting = true
        var remainingSeconds = -1
        rateLimiting.compute(contact.id) { _, cooldownTime ->
            if (cooldownTime == null || currentTime >= cooldownTime) {
                isWaiting = false
                return@compute currentTime + GameConfig.Limit.frequencyLimitSeconds.get().inWholeMilliseconds
            }

            remainingSeconds = (cooldownTime - currentTime).milliseconds.inWholeSeconds.toInt()
            cooldownTime
        }
        return RateLimitingData(isWaiting, remainingSeconds)
    }

    fun User.isUserOverLimit(): Boolean {
        if (GameConfig.Limit.userDailyGamingLimit.isNone()) return false
        return (dailyUserRecord[id] ?: 0) > GameConfig.Limit.userDailyGamingLimit.get()
    }
}
