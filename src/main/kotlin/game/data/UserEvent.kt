package com.github.hatoyuze.restarter.game.data

import com.github.hatoyuze.restarter.game.data.SimpleConditionExpression.Requirement.GREATER
import com.github.hatoyuze.restarter.game.data.UserEvent.Data.TALENT_EVENT_CODE
import com.github.hatoyuze.restarter.game.data.serialization.ConditionExpressionSerializer
import com.github.hatoyuze.restarter.game.data.serialization.ReferEventId
import com.github.hatoyuze.restarter.game.data.serialization.UserEventBranchSerializer
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.AttributeType
import com.github.hatoyuze.restarter.mirai.ResourceManager
import com.github.hatoyuze.restarter.mirai.config.GameConfig.enableCacheTemporaryEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.absoluteValue

/**
 * 事件类型
 *
 * 一些特殊处理：
 * - 对于 [Talent] 转义而来的事件，将其加上 [TALENT_EVENT_CODE] 后得到 [isTalentEvent] 为 `true` 的事件
 * - 对于 [effect] 存在 [AttributeType.RDM] 的属性，执行时应将其进行转义，将 [AttributeType.RDM] 转移为真实属性
 *      而转义过后的事件 `id` 取其 `相反数 * 10` 后 加上对于的序列
 *      如 `id=19999`的事件中的 [AttributeType.RDM] 转义为 [AttributeType.INT], 则其 `id` 变为 `-199991`
 * */
@Serializable
data class UserEvent @JvmOverloads constructor(
    val id: Int,
    @SerialName("event")
    val eventName: String,
    @Serializable(with = ConditionExpressionSerializer::class)
    val include: ConditionExpression? = NoCondition,
    @Serializable(with = ConditionExpressionSerializer::class)
    val exclude: ConditionExpression? = NoCondition,

    // default value
    @SerialName("NoRandom")
    val noRandom: Boolean = false,
    val grade: Int = 0,
    val postEvent: String? = null,
    val effect: Map<AttributeType, Int> = mapOf(),
    @Serializable(with = UserEventBranchSerializer::class)
    val branch: List<Pair<ConditionExpression, ReferEventId>> = listOf(),
    @Transient val isTalentEvent: Boolean = false
) {
    fun applyEffect(attribute: Attribute) {
        with(attribute) {
            for ((type, changedValue) in effect) {
                type += changedValue
            }
        }
    }

    companion object Data {
        internal const val REPLACEABLE_EVENT_ID = 30003

        internal val data: MutableMap<Int, UserEvent> by lazy {
            val jsonContent = ResourceManager.getResource("data/events.json") ?: error("Can not find resources: events")
            val json = Json {
                ignoreUnknownKeys = true
            }
            json.decodeFromString<Map<String, UserEvent>>(jsonContent).mapKeys { it.key.toInt() }.toMutableMap()
                .apply {
                    this[REPLACEABLE_EVENT_ID] = UserEvent(
                        id = REPLACEABLE_EVENT_ID,
                        eventName = "你偶然间发现地平线竟然出现了无端的黑块...\\n你想要逃、   跑...\\n远在你动身前，世界崩塌了。",
                        noRandom = true,
                        grade = 2,
                        branch = listOf(
                            SimpleConditionExpression(AttributeType.STR, GREATER, listOf(Int.MIN_VALUE)) to 10000
                        )
                    )
                }
        }
        val values = data.values
        val keys = data.keys
        private val attributeTypeOptions = listOf(
            AttributeType.CHR, AttributeType.INT, AttributeType.STR, AttributeType.MNY, AttributeType.SPR
        )

        operator fun get(evtId: Int): UserEvent? {
            return if (evtId < 0) {
                val identify = (evtId % 10).absoluteValue

                val talentEvent =
                    data[evtId] ?: Talent.data[evtId - TALENT_EVENT_CODE]?.ofEvent()?.also {
                        if (enableCacheTemporaryEvent)
                            data[it.id] = it
                    }
                if (talentEvent != null) return talentEvent

                val realId = identify.absoluteValue / 10

                val attributeType = attributeTypeOptions.getOrNull(identify) ?: return null
                val originEvent = data[realId] ?: return null
                originEvent.copy(effect = originEvent.effect.mapKeys {
                    if (it.key == AttributeType.RDM) attributeType else it.key
                }).also {
                    if (enableCacheTemporaryEvent)
                        data[evtId] = it
                }
            } else data[evtId]?.wrapRandomEffect()
        }

        internal operator fun set(evtId: Int, newEvent: UserEvent) =
            data.set(evtId, newEvent)

        private fun UserEvent.wrapRandomEffect(): UserEvent {
            if (effect.isEmpty()) return this
            if (!effect.containsKey(AttributeType.RDM)) return this
            val identify = attributeTypeOptions.indices.random()
            val newId = (-id) * 10 + identify
            return copy(
                id = newId,
                effect = effect.mapKeys { if (it.key == AttributeType.RDM) attributeTypeOptions[identify] else it.key })
        }

        fun Talent.ofEvent(): UserEvent {
            return UserEvent(
                id = id + TALENT_EVENT_CODE,
                eventName = "天赋【${name}】发动: $description",
                noRandom = true,
                effect = effect,
                isTalentEvent = true
            ).wrapRandomEffect().also { if (enableCacheTemporaryEvent) data[it.id] = it }
        }

        private const val TALENT_EVENT_CODE = -100000
    }
}