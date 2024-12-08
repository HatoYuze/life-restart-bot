package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.game.data.ComplexConditionExpression
import com.github.hatoyuze.restarter.game.data.ConditionExpression
import com.github.hatoyuze.restarter.game.data.NoCondition
import com.github.hatoyuze.restarter.game.data.SimpleConditionExpression
import kotlin.test.Test

class ExpressionTest {
    private fun buildOriginString(value: ConditionExpression): String {
        return when (value) {
            is SimpleConditionExpression -> SimpleConditionExpression.originalExpression(value)
            is ComplexConditionExpression -> {
                value.expressions.joinToString(" " + value.operator!!.symbol + " ") {
                    "(${buildOriginString(it)})"
                }
            }

            NoCondition -> ""
        }
    }

    @Test
    fun test() {
        val expression = ConditionExpression.parseExpression(
            "(EVT?[10009]) & ( (INT?[10001,10110,10111]) | (AGE>50) )"
        )

        println(expression)
        println(buildOriginString(expression))
    }
}