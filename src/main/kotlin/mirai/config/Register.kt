package com.github.hatoyuze.restarter.mirai.config

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.annotations.TomlComments
import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.PluginMain.logger
import com.github.hatoyuze.restarter.game.data.*
import com.github.hatoyuze.restarter.game.data.serialization.ConditionExpressionSerializer
import com.github.hatoyuze.restarter.game.data.serialization.ReferEventId
import com.github.hatoyuze.restarter.game.data.serialization.UserEventBranchSerializer
import com.github.hatoyuze.restarter.game.entity.AttributeType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.io.path.absolutePathString


@Serializable
data class TomlRegisterConfig (
    val enabledCustomizedEvent: Boolean = false,
    val events: Set<UserEventConfig> = emptySet(),
    @TomlComments(
        "事件可以被触发的年龄(范围为 0 到 500 岁,且为整数)，键值的表达式按照以下规律",
        "- start..end\t-> start ≤ x < end",
        "- start..=end\t-> start ≤ x ≤ end",
        "- ..end\t-> 0 ≤ x < end",
        "- ..\t-> 0 ≤ x ≤ 500",
        "- ..end\t-> 0 ≤ x < end",
        "- ..\t-> 0 ≤ x ≤ 500",
        "- ..=end\t-> x ≤ end"
    )
    val enableAge: Map<String, Set<Int>> = emptyMap()
) {
    fun handleEvent() {
        if (!enabledCustomizedEvent) return
        var success = 0

        for ((range0, settingEventIds) in enableAge) {
            val range = resolveRange(range0)
            if (range.first < 0 || range.last > 500) {
                logger.error("`$range0` 表达式超过了支持的岁数范围，将自动忽略！")
                continue
            }
            success += handleEventsImpl(settingEventIds, events, range)
        }
        logger.info("成功载入 $success 个自定义事件！")
    }

    private fun handleEventsImpl(
        settingEventIds: Set<Int>,
        validEventId: Set<UserEventConfig>,
        range: IntRange
    ): Int {
        var success = 0
        for (eventId in settingEventIds) {
            val evt = validEventId.find { it.id  == eventId }
            if (evt == null) {
                logger.warning("事件id $eventId 并不是一个有效的事件，已自动忽略！")
                continue
            }
            success++
            range.forEach {
                AgeSupportEvents.data[it] = AgeSupportEvents.data[it]?.apply {
                    this.events[eventId] = evt.weight
                } ?: return@forEach
            }
        }
        return success
    }

    @Serializable
    data class UserEventConfig(
        val id: Int,
        @SerialName("event")
        val eventName: String,
        @Serializable(with = ConditionExpressionSerializer::class)
        val include: ConditionExpression? = NoCondition,
        @Serializable(with = ConditionExpressionSerializer::class)
        val exclude: ConditionExpression? = NoCondition,

        @SerialName("NoRandom")
        val noRandom: Boolean = false,
        val grade: Int = 0,
        val postEvent: String? = null,
        val effect: Map<AttributeType, Int> = mapOf(),
        @Serializable(with = UserEventBranchSerializer::class)
        val branch: List<Pair<ConditionExpression, ReferEventId>> = listOf(),
        val weight: Double = 1.0
    ) {
        fun apply(): UserEvent {
            val event = UserEvent(id, eventName, include, exclude, noRandom, grade, postEvent, effect, branch, false)
            UserEvent.data[id] = event
            return event
        }
    }
    companion object {
        private fun resolveRange(expression: String): IntRange {
            val identifiersIndex = expression.indexOf("..")
            val identifiersEqualsIndex = expression.indexOf("=")
            require(expression.length >= 2) { "范围表达式过短" }
            return when {
                // 以 .. 开始的表达式
                identifiersIndex == 0 && expression.length == 2 -> IntRange(0, 501)
                identifiersIndex == 0 && expression[3] == '=' -> IntRange(0, expression.substringAfter('=').toInt() + 1)
                identifiersIndex == 0 -> IntRange(0, expression.substringAfterLast('.').toInt())

                // ..=
                identifiersEqualsIndex == identifiersIndex + 2 -> {
                    IntRange(
                        expression.substringBefore('.').toInt(),
                        expression.substringAfterLast('=').toInt() + 1
                    )
                }
                // a..b
                else -> {
                    IntRange(
                        expression.substringBefore('.').toInt(),
                        expression.substringAfterLast('.').toInt()
                    )
                }
            }
        }

        val default = TomlRegisterConfig(
            enabledCustomizedEvent = true,
            events = setOf(
                UserEventConfig(
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
                UserEventConfig(
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
                UserEventConfig(
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
                        ) to 50004
                    )
                ),
                UserEventConfig(
                    id = 50004,
                    eventName = "腾讯认为你使用机器人获利，将你告上了法庭，并要求你缴纳非法所得。",
                    postEvent = "腾讯胜诉了。",
                    noRandom = true,
                    effect = mapOf(
                        AttributeType.MNY to -3
                    )
                ),
                UserEventConfig(
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
                UserEventConfig(
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
            ),
            enableAge = mapOf(
                "12..=50" to setOf(50001, 50002, 50003, 50004, 50005)
            )
        )
    }
}

class Register(path: String = PluginMain.configFolderPath.absolutePathString(), name: String = "register.toml") {
    val file: File
    val toml = Toml(inputConfig = TomlInputConfig(allowEmptyToml = true), outputConfig = TomlOutputConfig(explicitTables = true))
    val config: TomlRegisterConfig
    internal val event: Set<TomlRegisterConfig.UserEventConfig>


    init {
        File(path).mkdirs()
        file = File(path).resolve(name)
        logger.info("INTO ${file.path}")
        if (file.createNewFile()) {
            logger.warning("没有找到有效的配置文件，已创建新的配置文件 -> ${file.path}")
            file.writeText(toml.encodeToString(TomlRegisterConfig.default))
            config = TomlRegisterConfig.default
        }else {
            config = toml.decodeFromString<TomlRegisterConfig>(file.readText())
                .also { logger.info("成功读取到 ${it.events.size} 个自定义事件") }
        }

        event = config.events
    }

}
