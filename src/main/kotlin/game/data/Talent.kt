package com.github.hatoyuze.restarter.game.data

import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.game.data.serialization.ConditionExpressionSerializer
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.AttributeType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int

@Serializable
data class Talent @JvmOverloads constructor(
    val description: String,
    val grade: Int,
    val id: Int,
    val name: String,

    val exclusive: List<String> = listOf(),
    val effect: Map<AttributeType, Int> = emptyMap(),
    @Serializable(with = ConditionExpressionSerializer::class)
    val condition: ConditionExpression? = null,
    val replacement: Replacement0? = null,
    // 选择该天赋后初始可用属性点变化值
    val status: Int = 0
) {
    fun applyEffect(attribute: Attribute) {
        with(attribute) {
            for ((type, changedValue) in effect) {
                type += changedValue
            }
        }
    }

    @Serializable
    data class Replacement0(
        val talent: List<JsonPrimitive> = emptyList(),
        val grade: List<Int> = emptyList()
    ) {
        fun replace() {
            when {
                talent.isEmpty() && grade.isEmpty() -> data.values.random()
                grade.isNotEmpty() -> {
                    data.values.filter { it.grade in grade }.random()
                }
                else -> {
                    // todo: 支持 如 1048*0.2 的表达式(为概率？)
                    val id = talent
                        .map { if (it.isString) it.content.substringBefore('*').toInt() else it.int }
                        .random()
                    data[id] ?: error("Talent id $id no found")
                }
            }

        }
    }

    companion object {
        val data by lazy {
            val jsonContent =
                PluginMain.getResource("data/talents.json") ?: error("Can not find resources: talents")
            val json = Json {
                ignoreUnknownKeys = true
            }
            json.decodeFromString<HashMap<String,Talent>>(jsonContent).mapKeys { it.key.toInt() }
        }
    }
}


