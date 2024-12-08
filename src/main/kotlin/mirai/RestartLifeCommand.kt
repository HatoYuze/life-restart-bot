package com.github.hatoyuze.restarter.mirai

import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.game.LifeEngine
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.content

object RestartLifeCommand : CompositeCommand(PluginMain, "remake") {
    @SubCommand
    suspend fun start(commandContext: CommandContext) = commandContext.run command@{
        if (!PluginMain.hasCustomPermission(sender.user)) {
            return@command
        }
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
            return@command
        }
        val arg = if (nextMessage.contains("，")) nextMessage.split('，')
        else nextMessage.split(',')

        val selected = arg.mapNotNull { it.toIntOrNull() }.toSet()

        if (selected.isEmpty()) {
            quote("无可用天赋，已停止任务！")
            return@command
        }
        if (arg.size > 3) {
            quote("最多只能选择3个天赋")
            return@command
        }
        val talents = selected.map { selectList.getOrElse(it - 1) { selectList.random() } }
        // todo: 支持自动更改的天赋

        val engine = LifeEngine {
            appearance = (0..15).random()
            strength = (0..15).random()
            intelligent = (0..15).random()
            money = (0..15).random()
            spirit = (0..15).random()
            this.talents = talents
        }

        val lifeList = engine.toList()
        val initialProperty = lifeList[0].property

        sendForwardMessage {
            add(
                senderId = fromContact,
                senderName = from?.nick ?: "不愿意透露姓名的某人",
                message = PlainText(
                    """
                                您选择的天赋为：
                                ${talents.joinToString(", ") { it.name }}
                                
                                您的初始属性点如下：
                                智慧：${initialProperty.intelligent}, 力量：${initialProperty.strength}, 外貌：${initialProperty.appearance}
                                快乐：${initialProperty.spirit}, 家境：${initialProperty.money}
                            """.trimIndent()
                )
            )
            for ((base, i) in lifeList.chunked(20).withIndex()) {
                add(
                    senderId = fromContact,
                    senderName = from?.nick ?: "不愿意透露姓名的某人",
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
                senderId = fromContact,
                senderName = from?.nick ?: "不愿意透露姓名的某人", message = PlainText("以上为模拟结果。以下为结算：")
            )
            add(
                senderId = fromContact,
                senderName = from?.nick ?: "不愿意透露姓名的某人", message = PlainText(
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
}