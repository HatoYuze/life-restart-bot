package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.game.data.ConditionExpression
import kotlin.test.Test

class Test{
    @Test
    fun test() {
        println(
           ConditionExpression.parseExpression(
                "(EVT?[10009])&((INT?[10001,10110,10111])|(AGE>50))"
            ).also { println(it) }

        )
    }
}