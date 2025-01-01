@file:OptIn(ConsoleExperimentalApi::class)

package com.github.hatoyuze.restarter.mirai

import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.draw.GameLayoutDrawer
import com.github.hatoyuze.restarter.game.LifeEngine
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.data.UserEvent
import com.github.hatoyuze.restarter.game.entity.impl.LifeSave.Companion.decodeBase64
import com.github.hatoyuze.restarter.mirai.config.CommandLimitData.checkCooldownStatus
import com.github.hatoyuze.restarter.mirai.config.CommandLimitData.dailyUserRecord
import com.github.hatoyuze.restarter.mirai.config.CommandLimitData.isUserOverLimit
import com.github.hatoyuze.restarter.mirai.config.GameConfig
import com.github.hatoyuze.restarter.mirai.config.GameConfig.Limit.Companion.isNone
import com.github.hatoyuze.restarter.mirai.config.GameConfig.Limit.Companion.userDailyGamingLimit
import com.github.hatoyuze.restarter.mirai.config.GameSaveData
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.isNotUser
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

object RestartLifeCommand : CompositeCommand(PluginMain, "remake") {

    private fun CommandContext.testPermission() {
        if (!PluginMain.hasCustomPermission(sender.user)) {
            throw PermissionDeniedException()
        }
    }
    private suspend fun CommandContext.testLimit(): Unit? {
        if (userDailyGamingLimit.isNone()) return Unit
        val user = sender.user ?: return Unit

        if (user.isUserOverLimit()) {
            if (dailyUserRecord[user.id]!! <= userDailyGamingLimit.get() + 1) {
                quote("您的今日游玩次数已用完，明天再来吧？")
            }
            return null
        }
        dailyUserRecord.compute(user.id) { _, count ->
            (count ?: 0) + 1
        }

        val status = sender.checkCooldownStatus()
        if (status.isWaiting) {
            quote("该功能目前还在冷却中哦~\n您还需要等待 ${status.remainingSeconds} 秒后 才能正常使用功能~")
           return null
        }

        return Unit
    }

    @Description("开始一场新的人生")
    @SubCommand
    suspend fun start(
        commandContext: CommandContext,
        @Name("初始颜值值")
        initialAppearance: Int = 0,
        @Name("初始智力值")
        initialIntelligent: Int = 0,
        @Name("初始体质值")
        initialStrength: Int = 0,
        @Name("初始家境值")
        initialMoney: Int = 0,
        @Name("初始快乐值")
        initialSpirit: Int = 0
    ) = commandContext.run {
        commandContext.run command@{
            testPermission()
            testLimit() ?: return@command


            val objectTalents = getTalents(true) ?: return@command

            val statusChange = objectTalents.map { it.status }
            val distribute = distributeValues(
                listOf(
                    initialAppearance,
                    initialStrength,
                    initialIntelligent,
                    initialMoney,
                    initialSpirit
                ), statusChange.sum()
            )

            var failed = false
            val engine = LifeEngine {
                appearance = distribute[0]
                strength = distribute[1]
                intelligent = distribute[2]
                money = distribute[3]
                spirit = distribute[4]
                try {
                    talents = objectTalents
                } catch (e: IllegalArgumentException) {
                    failed = true
                }
            }
            if (failed) {
                quote("选择了相互冲突的天赋，已停止！")
                return@command
            }


            if (sender.isNotUser()) {
                val lifeList = engine.toList()
                println(lifeList.joinToString { "${it.property.lifeAge}岁: ${it.name}" })
                return@command
            }
            val image = GameLayoutDrawer.createGamingImage(engine.life).toExternalResource().use {
                subject.uploadImage(it)
            }
            quote(buildMessageChain {
                +buildString {
                    appendLine("游戏结算：")
                    with(engine.ratingStatus) {
                        appendLine("颜值：${appearance.value} ${appearance.judge}")
                        appendLine("家境：${money.value} ${money.judge}")
                        appendLine("乐观：${spirit.value} ${spirit.judge}")
                        appendLine("智力：${intelligent.value} ${intelligent.judge}")
                        appendLine("力量：${strength.value} ${strength.judge}")
                        appendLine("总分：${sum.value} ${sum.judge}")
                    }
                }
                +image
            })
            GameSaveData.save(engine.life, sender.user)
        }
    }

    @Description("获取排行榜")
    @SubCommand
    suspend fun rank(
        commandContext: CommandContext,
        range: String = "day"
    ) = commandContext.run {
        testPermission()
        if (!GameConfig.enableGameSave) {
            quote("当前无法使用排行榜功能哦！\n请联系 Bot 主更改设置!")
            PluginMain.logger.error("Oops! 当 game.yml/enableGameSave 设定为 `false` 时，无法存储记录导致无可用排行榜数据")
            return@run
        }
        val data = when (range.lowercase()) {
            "hour", "h" -> GameSaveData.timeFilter(1.hours)
            "day", "d" -> GameSaveData.timeFilter(1.days)
            "week", "w" -> GameSaveData.timeFilter(7.days)
            "month", "m" -> GameSaveData.timeFilter(30.days)
            "all", "max", "a" -> GameSaveData.data
            else -> GameSaveData.timeFilter(1.days)
        }.sortedBy { it.content.score }

        val messageContent = buildString {
            for (i in 0..max(9, data.lastIndex)) {
                val info = data[i]
                appendLine("#${i + 1} [id ${info.id}] 用户 ${info.content.userNameBase64.decodeBase64()}(${info.id}) 的记录 评分：${info.content.score}")
                appendLine(
                    "创建时间： ${
                        Instant.ofEpochMilli(info.content.createAtTimestampUTC).atZone(ZoneId.systemDefault())
                            .toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    }"
                )
            }
        }
        quote(messageContent)
    }


    @Description("体验他人的人生")
    @SubCommand
    suspend fun recall(commandContext: CommandContext, id: Int) =
        commandContext.run command@{
            testPermission()
            val data = GameSaveData.data.find { it.id == id } ?: kotlin.run {
                quote("没有找到 id 为 $id 的数据哦！\n这可能是因为数据已过期，被系统永久删除力＞﹏＜")
                return@command
            }
            val content = data.content
            val image = GameLayoutDrawer.createGamingImage(content).toExternalResource().use {
                subject.uploadImage(it)
            }

            quote(buildMessageChain {
                +image
                with(content.property.attribute) {
                    appendLine("Ta 的人生模拟器评分：")
                    appendLine("颜值：${appearanceSummary.value} ${appearanceSummary.judge}")
                    appendLine("家境：${moneySummary.value} ${moneySummary.judge}")
                    appendLine("乐观：${spiritSummary.value} ${spiritSummary.judge}")
                    appendLine("智力：${intelligentSummary.value} ${intelligentSummary.judge}")
                    appendLine("力量：${strengthSummary.value} ${strengthSummary.judge}")
                    append("总分：${sumSummary.value} ${sumSummary.judge}")
                }
            })
        }


    @Description("开始一场新的人生（文字版）")
    @SubCommand
    suspend fun text(
        commandContext: CommandContext,
        @Name("初始颜值值")
        initialAppearance: Int = 0,
        @Name("初始智力值")
        initialIntelligent: Int = 0,
        @Name("初始体质值")
        initialStrength: Int = 0,
        @Name("初始家境值")
        initialMoney: Int = 0,
        @Name("初始快乐值")
        initialSpirit: Int = 0
    ) = commandContext.run command@{
        testPermission()
        testLimit() ?: return@command
        val objectTalents = getTalents() ?: return@command

        val statusChange = objectTalents.map { it.status }
        val distribute = distributeValues(
            listOf(
                initialAppearance,
                initialStrength,
                initialIntelligent,
                initialMoney,
                initialSpirit
            ), statusChange.sum()
        )

        var failed = false
        val engine = LifeEngine {
            appearance = distribute[0]
            strength = distribute[1]
            intelligent = distribute[2]
            money = distribute[3]
            spirit = distribute[4]
            try {
                talents = objectTalents
            } catch (e: IllegalArgumentException) {
                failed = true
            }
        }
        if (failed) {
            quote("选择了相互冲突的天赋，已停止！")
            return@command
        }


        val lifeList = engine.toList()
        val initialProperty = lifeList[0].property

        if (sender.isNotUser()) {
            println(lifeList.joinToString { "${it.property.lifeAge}岁: ${it.name}" })
        }

        sendForwardMessage {
            add(
                sender = commandContext.sender.user!!,
                message = PlainText(
                    """
您拥有以下天赋：
${engine.life.talents.joinToString("\n") { it.introduction }}

您的初始属性点如下：
智慧：${initialProperty.intelligent}, 力量：${initialProperty.strength}, 外貌：${initialProperty.appearance}
快乐：${initialProperty.spirit}, 家境：${initialProperty.money}
                            """.trimIndent()
                )
            )
            for ((base, i) in lifeList.chunked(20).withIndex()) {
                add(
                    sender = commandContext.sender.user!!,
                    message = PlainText(
                        buildString {
                            for ((offset, event) in i.withIndex()) {
                                val age = (base * 20) + offset
                                appendLine("${age}岁：${event.name}")
                            }
                        }
                    )
                )
            }
            add(
                sender = commandContext.sender.user!!, message = PlainText("以上为模拟结果。以下为结算：")
            )
            add(
                sender = commandContext.sender.user!!, message = PlainText(
                    buildString {
                        with(engine.ratingStatus) {
                            appendLine("颜值：${appearance.value} ${appearance.judge}")
                            appendLine("家境：${money.value} ${money.judge}")
                            appendLine("乐观：${spirit.value} ${spirit.judge}")
                            appendLine("智力：${intelligent.value} ${intelligent.judge}")
                            appendLine("力量：${strength.value} ${strength.judge}")
                            appendLine("总分：${sum.value} ${sum.judge}")
                        }
                    }
                ))
        }
        GameSaveData.save(engine.life, sender.user)
    }


    @Description("获取指定事件的详情信息")
    @SubCommand
    suspend fun event(commandContext: CommandContext, id: Int) = commandContext.run command@{
        testPermission()
        val event = UserEvent.Data[id] ?: run {
            quote("未找到 id 为 $id 的事件")
            return@command
        }
        val result = buildString {
            append("""
            |#${event.id}:
            | ⌈${event.eventName}⌋
            | 
            | 触发条件: ${
                event.include?.chineseDescription() ?: if (event.noRandom) "需要完成事件 ${
                    UserEvent.values.filter { libEvent -> libEvent.branch.any { it.second == event.id } }
                        .joinToString(" 或 ") { it.id.toString() }
                }"
                else "无条件"
            }""".trimMargin())
            appendLine()
            if (!event.postEvent.isNullOrEmpty()) {
                append("> ⌈${event.postEvent}⌋")
            }
            if (event.branch.isNotEmpty()) {
                for (branch in event.branch) {
                    appendLine(" 在 '${branch.first.chineseDescription(true)}' 的条件下发生事件 ${branch.second}")
                }
            }
            if (event.effect.isNotEmpty()) {
                appendLine("作用：")
                for (effect in event.effect) {
                    val changeDesc =
                        if (effect.value < 0) "减少 ${effect.value.absoluteValue}" else "增加 ${effect.value}"
                    appendLine("使 ${effect.key.chineseDesc} $changeDesc")
                }
            }
            deleteAt(lastIndex)
        }
        quote(result)
    }

    @Description("查询天赋的详细信息")
    @SubCommand
    suspend fun talent(commandContext: CommandContext, id: Int) = commandContext.run command@{
        testPermission()
        val talent = Talent.data[id] ?: run {
            quote("未找到 id 为 $id 的天赋")
            return@command
        }
        val result = buildString {
            appendLine("#${talent.id}")
            appendLine(talent.introduction)
            if (talent.exclude.isNotEmpty())
                appendLine("与 ${talent.exclude.joinToString { it.toString() }} 冲突")
            appendLine("在 ${talent.condition?.chineseDescription() ?: "初始阶段"} 触发天赋效果")
            appendLine("作用：")
            for (effect in talent.effect) {
                val changeDesc = if (effect.value < 0) "减少 ${effect.value.absoluteValue}" else "增加 ${effect.value}"
                appendLine("使 ${effect.key.chineseDesc} $changeDesc")
            }
            if (talent.status != 0) {
                val changeDesc =
                    if (talent.status < 0) "减少 ${talent.status.absoluteValue}" else "增加 ${talent.status}"
                appendLine("使 可分配天赋值 $changeDesc")
            }
            deleteAt(lastIndex)

        }
        quote(result)
    }


    private suspend fun CommandContext.getTalents(enableImage: Boolean = false): List<Talent>? {
        val selectList = LifeEngine.randomTalent()
        if (enableImage) {
            val image = GameLayoutDrawer.createTalentOptionImage(selectList).toExternalResource().use {
                subject.uploadImage(it)
            }
            quote(image)
        } else {
            quote(buildMessageChain {
                appendLine("请在抽取的随机天赋中共选择3个：")
                appendLine(" >直接输入对应的序号即可，数字间由英文逗号相隔(如: 1,2,3)")
                selectList.forEachIndexed { id, talent ->
                    appendLine("${id + 1}：${talent.name}")
                    appendLine("   ${talent.description}")
                }
            })
        }

        val nextMessage = nextMessageMemberOrNull(60_000L) { true }?.content ?: run {
            quote("已超时！自动停止任务！")
            return null
        }
        val arg = nextMessage.split("[,，]".toRegex())

        val selected = arg.mapNotNull { it.toIntOrNull() }.toSet()

        if (selected.isEmpty()) {
            quote("无可用天赋，已停止任务！")
            return null
        }
        if (arg.size > 3) {
            quote("最多只能选择3个天赋")
            return null
        }

        return selected.map { selectList.getOrElse(it - 1) { selectList.random() } }
    }

    private fun distributeValues(values: List<Int>, pointChange: Int = 0): List<Int> {
        val totalSum = values.sum()
        val maxAttributePoint = GameConfig.maxAttributePoint.toInt() + pointChange

        return when {
            totalSum > maxAttributePoint -> {
                val ratio = maxAttributePoint.toDouble() / totalSum
                val new = values.map { ((it * ratio).toInt()) }.toMutableList()
                val newSum = new.sum()
                if (newSum == maxAttributePoint) return new
                new[new.lastIndex] += (maxAttributePoint - newSum)
                new
            }

            totalSum < 10 -> {
                distributeValues(
                    listOf(
                        (0..15).random(),
                        (0..15).random(),
                        (0..15).random(),
                        (0..15).random(),
                        (0..15).random()
                    )
                )
            }

            totalSum < maxAttributePoint -> {
                var remainingSum = maxAttributePoint - totalSum
                val valuesCopy = values.toMutableList()
                val zeroElementIndex =
                    valuesCopy.withIndex().associate { it.index to it.value }.filter { it.value == 0 }.keys
                if (zeroElementIndex.size == 1) {
                    valuesCopy[zeroElementIndex.first()] = remainingSum
                    return valuesCopy
                }
                for (index in zeroElementIndex) {
                    if (index == zeroElementIndex.last()) {
                        valuesCopy[index] = remainingSum
                        break
                    }
                    if (remainingSum == 0) break
                    val replaceValue = Random.nextInt(1, remainingSum + 1)
                    remainingSum -= replaceValue
                    valuesCopy[index] = replaceValue
                }
                valuesCopy
            }

            else -> values
        }
    }
}