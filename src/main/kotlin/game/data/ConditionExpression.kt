package com.github.hatoyuze.restarter.game.data

import com.github.hatoyuze.restarter.game.data.SimpleConditionExpression.Requirement.*
import com.github.hatoyuze.restarter.game.data.SimpleConditionExpression.Serializer.resolveSimpleExpression
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.AttributeType
import com.github.hatoyuze.restarter.game.entity.AttributeType.EVT
import com.github.hatoyuze.restarter.game.entity.AttributeType.TLT
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


// attr?[n] -> 某属性 == n(任意 [] 内的值) 时
// attr>n 某属性大于n
// attr<n
// 有多个 & 或 | 条件时，每个小分支需要用 () 包裹

@Serializable
sealed class ConditionExpression {
    abstract fun judgeAll(attr: Attribute): Boolean

    override fun toString(): String {
        return when (this) {
            is ComplexConditionExpression -> "ComplexConditionExpression (*${this.operator}) { ${this.expressions} }"
            is SimpleConditionExpression -> "SimpleConditionExpression { ${this.member} should be ${this.type} ${this.range} }"
            NoCondition -> "NoCondition"
        }
    }

    companion object {
        private fun nonZero(a: Int, b: Int, range: Int) = when {
            range == -1 -> a.takeIf { it != -1 } ?: b.takeIf { it != -1 } ?: -1
            a != -1 && a < range -> a
            b != -1 && b < range -> b
            else -> -1
        }
        fun parseExpression(expression0: String): ConditionExpression {
            if (expression0.isEmpty()) {
                return NoCondition
            }
            val expression = expression0.replace(" ", "")
            val index = findBalancedParentheses(expression) // next right bracket index
            if (index == -1) {
                return parseExpression("$expression0)") // 主动对齐括号
            }
            val pairExpressionIndex = nonZero(expression.indexOf('|'), expression.indexOf('&'), expression.indexOf('('))
            val isDoubleExpression = pairExpressionIndex != -1 && index == 0

            if (index <= 0 && !isDoubleExpression) {
                // 没有括号，直接解析为简单表达式
                return resolveSimpleExpression(expression)
            }
            // 解析左表达式
            val subExpression =
                if (isDoubleExpression) expression.substring(0, pairExpressionIndex)
                else expression.substring(1, index) // 去掉左括号
            val subExprCondition = parseExpression(subExpression)

            // 解析剩余的表达式
            val remainingExpression = expression.substring(if (!isDoubleExpression)index + 1 else pairExpressionIndex).trim()
            val operator = when {
                remainingExpression.startsWith("&") -> ConditionOperator.AND
                remainingExpression.startsWith("|") -> ConditionOperator.OR
                else -> return subExprCondition
            }
            // 组合子表达式和剩余表达式
            return ComplexConditionExpression(operator, mutableListOf(subExprCondition).also {
                if (remainingExpression.isNotEmpty()) it.add(parseExpression(remainingExpression.substring(1)))
                if (remainingExpression.isEmpty() && subExprCondition is ComplexConditionExpression) {
                    it.addAll(subExprCondition.expressions)
                }
            })
        }

        private fun findBalancedParentheses(expression: String): Int {
            var balance = 0
            for (i in expression.indices) {
                when (expression[i]) {
                    '(' -> balance++
                    ')' -> balance--
                }
                if (balance == 0) return i // 找到匹配的右括号
            }
            return -1 // 没有找到匹配的右括号
        }
    }
}

enum class ConditionOperator(val symbol: String) {
    OR("|"),
    AND("&")
}

@Serializable
data class ComplexConditionExpression(val operator: ConditionOperator?, val expressions: List<ConditionExpression>) :
    ConditionExpression() {
    override fun judgeAll(attr: Attribute): Boolean = when (operator) {
        ConditionOperator.OR, null -> expressions.any { it.judgeAll(attr) }
        ConditionOperator.AND -> expressions.all { it.judgeAll(attr) }
    }
}

data object NoCondition: ConditionExpression() {
    override fun judgeAll(attr: Attribute): Boolean {
        return true
    }

}


@Serializable(SimpleConditionExpression.Serializer::class)
class SimpleConditionExpression(
    val member: AttributeType,
    val type: Requirement,
    val range: List<Int>
) : ConditionExpression() {
    @Serializable
    enum class Requirement(val expression: Char) {
        GREATER('>'),
        RANGE_CONTAINS('?'),
        LESS('<'),
        RANGE_EXCLUDES('!'),
        ;

        companion object {
            fun resolveExpression(expression: Char) = Requirement.entries.find { it.expression == expression }
        }
    }

    private fun judge(attr: Attribute): Boolean {
        when(member) {
            EVT, TLT -> {
                val value = attr.getPropArray(member)
                return when (type) {
                    RANGE_CONTAINS -> value.any { range.contains(it) }
                    RANGE_EXCLUDES -> !value.any { range.contains(it) }
                    else -> false
                }
            }
            else -> {
                val value = attr.getPropInteger(member)
                return when (type) {
                    GREATER -> value > range.first()
                    RANGE_CONTAINS -> range.contains(value)
                    LESS -> value < range.first()
                    RANGE_EXCLUDES -> !range.contains(value)
                }
            }
        }


    }

    companion object Serializer : KSerializer<SimpleConditionExpression> {
        private const val ATTR_LENGTH = 3
        private const val TYPE_INDEX = 3
        private const val SIMPLE_VALUE_INDEX = 4

        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("SimpleConditionExpression", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): SimpleConditionExpression {
            // AGE?[1,2,3,4,5]
            val origin = decoder.decodeString()

            return resolveSimpleExpression(origin)
        }

        fun resolveSimpleExpression(origin: String): SimpleConditionExpression {
            val memberDesc0 = origin.substring(0, ATTR_LENGTH)
            val member = AttributeType.valueOf(memberDesc0)

            // AGE?[1,2,3,4,5]
            //    ^
            val requirementDesc0 = origin[TYPE_INDEX]
            val type = Requirement.resolveExpression(requirementDesc0)
                ?: throw IllegalStateException("$requirementDesc0 is NOT a valid requirement expression.")

            val range =
                if (type == RANGE_CONTAINS || type == RANGE_EXCLUDES) {
                    origin.substring(TYPE_INDEX + 2, origin.length - 1).split(',').map { it.toInt() }
                } else listOf(origin.substring(SIMPLE_VALUE_INDEX).toInt())

            return SimpleConditionExpression(
                member, type, range
            )
        }
        fun originalExpression(value: SimpleConditionExpression) = buildString {
            append(value.member)
            append(value.type.expression)
            when {
                value.type == RANGE_EXCLUDES || value.type == RANGE_CONTAINS -> append(
                    value.range.toString().replace(" ", "")
                )

                value.range.size == 1 -> append(value.range.first())
                else -> throw IllegalStateException("除了 `?` 和 `!` 判别符以外，不应该有包含多个对象的元素! -> ${value.range}")
            }
        }

        override fun serialize(encoder: Encoder, value: SimpleConditionExpression) {
            encoder.encodeString(
                originalExpression(value)
            )
        }

    }

    override fun judgeAll(attr: Attribute): Boolean =
        judge(attr)
}