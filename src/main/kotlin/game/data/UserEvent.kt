package com.github.hatoyuze.restarter.game.data

import com.github.hatoyuze.restarter.game.data.serialization.ConditionExpressionSerializer
import com.github.hatoyuze.restarter.game.data.serialization.ReferEventId
import com.github.hatoyuze.restarter.game.data.serialization.UserEventBranchSerializer
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.AttributeType
import com.github.hatoyuze.restarter.mirai.ResourceManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


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
    val branch: List<Pair<ConditionExpression, ReferEventId>> = listOf()
) {
    fun applyEffect(attribute: Attribute) {
        with(attribute) {
            for ((type, changedValue) in effect) {
               type += changedValue
            }
        }
    }
    companion object {
        val data by lazy {
            val jsonContent =
                ResourceManager.getResource("data/events.json") ?: error("Can not find resources: events")
            val json = Json {
                ignoreUnknownKeys = true
            }
            json.decodeFromString<Map<String,UserEvent>>(jsonContent).mapKeys { it.key.toInt() }
        }
    }
}