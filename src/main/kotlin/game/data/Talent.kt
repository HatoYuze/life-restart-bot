package com.github.hatoyuze.restarter.game.data

import com.github.hatoyuze.restarter.game.data.serialization.ConditionExpressionSerializer
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.AttributeType
import com.github.hatoyuze.restarter.mirai.ResourceManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlin.random.Random

@Serializable
data class Talent @JvmOverloads constructor(
    val description: String,
    val grade: Int,
    val id: Int,
    val name: String,

    // 互相排斥的天赋
    @SerialName("exclusive")
    val exclusive0: List<JsonPrimitive> = listOf(),
    val effect: Map<AttributeType, Int> = emptyMap(),
    @Serializable(with = ConditionExpressionSerializer::class)
    val condition: ConditionExpression? = null,
    val replacement: Replacement0? = null,
    // 选择该天赋后初始可用属性点变化值
    val status: Int = 0
) {
    @Transient
    val exclusive = exclusive0.map { it.content.toInt() }

    val introduction: String
        get() = buildString {
            val gradeDescription = when (grade) {
                1 -> "【蓝色】"
                2 -> "【紫色】"
                3 -> "【橙色】"
                else -> "【白色】"
            }
            append(gradeDescription)
            appendLine(name)
            append("\t")
            append(description)
        }

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
        fun replace(): Talent =
            when {
                talent.isEmpty() && grade.isEmpty() -> data.values.random()
                grade.isNotEmpty() -> {
                    data.values.filter { it.grade in grade }.random()
                }

                else -> {
                    // *n 表示总计算作有 n 份
                    val id = talent
                        .associate {
                            if (it.isString) {
                                val content = it.content
                                val point = content.indexOf('*')
                                content.substring(0, point).toInt() to it.content.substring(point + 1).toDouble()
                            } else it.int to 1.0
                        }
                        .randomSelectWeight()

                    data[id] ?: error("Talent id $id no found")
                }
            }

        private companion object {
            fun <T> Map<T, Double>.randomSelectWeight(): T {
                require(isNotEmpty()) { "The items list cannot be empty." }
                val totalShares = values.sumOf { it }
                require(totalShares > 0) { "The sum of shares must be greater than zero." }


                val randomShareIndex = Random.nextDouble(totalShares) + 1
                var cumulativeShares = 0.0

                for ((item, probability) in this) {
                    cumulativeShares += probability
                    if (randomShareIndex <= cumulativeShares) {
                        return item
                    }
                }

                return this.keys.random()
            }
        }
    }

    companion object {
        val data by lazy {
            val jsonContent =
                ResourceManager.getResource("data/talents.json") ?: error("Can not find resources: talents")
            val json = Json {
                ignoreUnknownKeys = true
            }
            json.decodeFromString<HashMap<String, Talent>>(jsonContent).mapKeys { it.key.toInt() }
        }
    }
}


