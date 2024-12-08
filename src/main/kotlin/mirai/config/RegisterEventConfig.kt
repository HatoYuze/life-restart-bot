package com.github.hatoyuze.restarter.mirai.config

import com.github.hatoyuze.restarter.PluginMain.logger
import com.github.hatoyuze.restarter.game.data.*
import com.github.hatoyuze.restarter.game.entity.AttributeType
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object RegisterEventConfig : AutoSavePluginConfig("register") {
    @ValueDescription("是否启用自定义事件")
    val enabledCustomizedEvent: Boolean by value(false)

    @ValueDescription(
        """
        自定义的事件，可参考 README.md 提供的教程格式编写，
        在编写完事件后，需要在 `ages` 属性中注册事件，否则这些事件*永远不会*被触发
        
        关于 `id`: 您需要保证每一个 `id` 是唯一的，尽力使其保持一定的顺序
        在内置的事件数据中，其id有以下顺序:
        - `1xxxx`(范围: 10000~11502) -> 日常生活中的事件
        - `2xxxx`(范围: 20007~21457) -> 一些后续事件，并不会被随机直接选中 或 日常生活的部分事件（可能需要天赋）
        - `3xxxx`(范围: 30001~30002) -> 异世界生活？
        - `4xxxx`(范围: 40001~40084) -> 修仙事件
        
        我们建议您按照以上顺序继续 编写/补充， 如果您认为自己设计的事件很有创意，也可以向原项目提出 pr
    """
    )
    val events: Set<UserEvent> by value(
        setOf(
            UserEvent(
                id = 50001,
                eventName = "你在使用社交软件水群的时候，发现有群友在使用聊天机器人。",
                include = SimpleConditionExpression(
                    AttributeType.AGE,
                    SimpleConditionExpression.Requirement.LESS,
                    listOf(60)
                ),
                exclude = SimpleConditionExpression(
                    AttributeType.EVT,
                    SimpleConditionExpression.Requirement.RANGE_EXCLUDES,
                    listOf(50001, 50002)
                ),
                postEvent = "你感到很好奇。",
                branch = listOf(
                    ComplexConditionExpression(
                        ConditionOperator.AND, listOf(
                            SimpleConditionExpression(
                                AttributeType.INT,
                                SimpleConditionExpression.Requirement.GREATER,
                                listOf(5)
                            ),
                            SimpleConditionExpression(
                                AttributeType.MNY,
                                SimpleConditionExpression.Requirement.GREATER,
                                listOf(3)
                            )
                        )
                    ) to 50002
                )
            ),
            UserEvent(
                id = 50002,
                eventName = "你开始自己学着编写自己的 QQ 机器人, 由于各种各样的费用你用了很多钱。",
                noRandom = true,
                grade = 1,
                effect = mapOf(
                    AttributeType.MNY to -1,
                    AttributeType.INT to 1,
                    AttributeType.SPR to 1,
                )
            ),
            UserEvent(
                id = 50003,
                eventName = "由于你使用了非官方机器人接口，违反了腾讯的用户协议，你被举报了",
                include = SimpleConditionExpression(
                    AttributeType.EVT,
                    SimpleConditionExpression.Requirement.RANGE_CONTAINS,
                    listOf(50002)
                ),
                exclude = SimpleConditionExpression(
                    AttributeType.EVT,
                    SimpleConditionExpression.Requirement.RANGE_CONTAINS,
                    listOf(50005)
                ),
                postEvent = "不过这并没有对你的生活造成影响。",
                branch = listOf(
                    ComplexConditionExpression(
                        ConditionOperator.OR,
                        listOf(
                            SimpleConditionExpression(
                                AttributeType.MNY,
                                SimpleConditionExpression.Requirement.GREATER,
                                listOf(10)
                            ),
                            SimpleConditionExpression(
                                AttributeType.EVT,
                                SimpleConditionExpression.Requirement.RANGE_CONTAINS,
                                listOf(50005)
                            )
                        )
                    )
                        to 50004
                )
            ),
            UserEvent(
                id = 50004,
                eventName = "腾讯认为你使用机器人获利，将你告上了法庭，并要求你缴纳非法所得。",
                postEvent = "腾讯胜诉了。",
                noRandom = true,
                effect = mapOf(
                    AttributeType.MNY to -3
                )
            ),
            UserEvent(
                id = 50005,
                eventName = "你使用自己创作的机器人赚了很多钱。",
                include = SimpleConditionExpression(
                    AttributeType.EVT,
                    SimpleConditionExpression.Requirement.RANGE_CONTAINS,
                    listOf(50002)
                ),
                effect = mapOf(
                    AttributeType.MNY to 2,
                    AttributeType.SPR to 1
                )
            ),
            UserEvent(
                id = 50005,
                eventName = "你放弃了自己部署的机器人，因为你觉得这太费钱了。",
                include = SimpleConditionExpression(
                    AttributeType.EVT,
                    SimpleConditionExpression.Requirement.RANGE_CONTAINS,
                    listOf(50002)
                ),
                effect = mapOf(
                    AttributeType.MNY to 1,
                    AttributeType.SPR to -1
                )
            )
        )
    )

    @ValueDescription(
        """
        事件可以被触发的年龄(范围为 0 到 500 岁,且为整数)，尾部的表达式按照以下规律
        - start..end -> start ≤ x < end
        - start..=end -> start ≤ x ≤ end
        - ..end	-> 0 ≤ x < end
        - .. -> 0 ≤ x ≤ 500
        - ..=end -> x ≤ end
    """
    )
    val enableAge: Map<String, Set<Int>> by value(
        mapOf(
            "12..=50" to setOf(50001, 50002, 50003, 50004, 50005)
        )
    )

    private fun resolveRange(expression: String): IntRange {
        val identifiersIndex = expression.indexOf("..")
        val identifiersEqualsIndex = expression.indexOf("=")
        require(expression.length >= 2) { "范围表达式过短" }
        return when {
            // 以 .. 开始的表达式
            identifiersIndex == 0 && expression.length == 2 -> IntRange(0, 500)
            identifiersIndex == 0 && expression[3] == '=' -> IntRange(0, expression.substringAfter('=').toInt())
            identifiersIndex == 0 -> IntRange(0, expression.substringAfterLast('.').toInt() - 1)

            // ..=
            identifiersEqualsIndex == identifiersIndex + 2 -> {
                IntRange(
                    expression.substringBefore('.').toInt(),
                    expression.substringAfterLast('=').toInt()
                )
            }
            // a..b
            else -> {
                IntRange(
                    expression.substringBefore('.').toInt(),
                    expression.substringAfterLast('.').toInt() - 1
                )
            }
        }
    }

    fun handleEvent() {
        if (!enabledCustomizedEvent) return
        events.forEach {
            UserEvent.data[it.id] = it
        }
        val validEventId = UserEvent.data.keys
        var success = 0
        for ((range0, events) in enableAge) {
            val range = resolveRange(range0)
            if (range.first < 0 || range.last > 500) {
                logger.error("`$range0` 表达式超过了支持的岁数范围，将自动忽略！")
                continue
            }
            success += handleEventsImpl(events, validEventId, range)
        }
        logger.info("成功载入 $success 个自定义事件！")
    }

    private fun handleEventsImpl(
        events: Set<Int>,
        validEventId: MutableSet<Int>,
        range: IntRange
    ): Int {
        var success = 0
        for (eventId in events) {
            if (eventId !in validEventId) {
                logger.warning("事件id $eventId 并不是一个有效的事件，已自动忽略！")
                continue
            }
            success++
            range.forEach {
                AgeSupportEvents.data[it] = AgeSupportEvents.data[it]?.apply {
                    this.events[eventId] = 1.0
                } ?: return@forEach
            }
        }
        return success
    }
}