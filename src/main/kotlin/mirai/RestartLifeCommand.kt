@file:OptIn(ConsoleExperimentalApi::class)

package com.github.hatoyuze.restarter.mirai

import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.game.LifeEngine
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.data.UserEvent
import com.github.hatoyuze.restarter.game.entity.Life.Companion.talents
import com.github.hatoyuze.restarter.mirai.config.GameConfig.maxAttributePoint
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.isNotUser
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.content
import kotlin.math.absoluteValue
import kotlin.random.Random

object RestartLifeCommand : CompositeCommand(PluginMain, "remake") {

    @Description("开始一场新的人生")
    @SubCommand
    suspend fun start(
        commandContext: CommandContext,
        @Name("初始颜值值")
        initialAppearance: Int = 0,
        @Name("初始体质值")
        initialStrength: Int = 0,
        @Name("初始智力值")
        initialIntelligent: Int = 0,
        @Name("初始家境值")
        initialMoney: Int = 0,
        @Name("初始快乐值")
        initialSpirit: Int = 0
    ) = commandContext.run command@{
        if (!PluginMain.hasCustomPermission(sender.user)) {
            return@command
        }
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
    }


    @Description("获取指定事件的详情信息")
    @SubCommand
    suspend fun event(commandContext: CommandContext, id: Int) = commandContext.run command@{
        if (!PluginMain.hasCustomPermission(sender.user)) {
            return@command
        }
        val event = UserEvent.data[id] ?: run {
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
                    UserEvent.data.values.filter { libEvent -> libEvent.branch.any { it.second == event.id } }
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
        if (!PluginMain.hasCustomPermission(sender.user)) {
            return@command
        }
        val talent = Talent.data[id] ?: run {
            quote("未找到 id 为 $id 的天赋")
            return@command
        }
        val result = buildString {
            appendLine("#${talent.id}")
            appendLine(talent.introduction)
            if (talent.exclusive.isNotEmpty())
                appendLine("与 ${talent.exclusive.joinToString { it.toString() }} 冲突")
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


    private suspend fun CommandContext.getTalents(): List<Talent>? {
        val selectList = LifeEngine.randomTalent()
        quote(buildMessageChain {
            appendLine("请在抽取的随机天赋中共选择3个：")
            appendLine(" >直接输入对应的序号即可，数字间由英文逗号相隔(如: 1,2,3)")
            selectList.forEachIndexed { id, talent ->
                appendLine("${id + 1}：${talent.name}")
                appendLine("   ${talent.description}")
            }
        })

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
        val maxAttributePoint = maxAttributePoint + pointChange

        return when {
            totalSum > maxAttributePoint -> {
                val ratio = maxAttributePoint.toDouble() / totalSum
                val new = values.map { ((it * ratio).toInt()) }.toMutableList()
                val newSum = new.sum()
                if (newSum == maxAttributePoint) return new
                new[new.lastIndex] += (maxAttributePoint - newSum)
                new
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