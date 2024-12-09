package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.game.LifeEngine
import com.github.hatoyuze.restarter.game.data.AgeSupportEvents
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.data.UserEvent
import com.github.hatoyuze.restarter.game.entity.Life.Companion.talents
import com.github.hatoyuze.restarter.mirai.ResourceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class TestGame {
    @BeforeTest
    @Test
    fun data() {
        ResourceManager.isTesting = true
        println("load event:")
        var used = measureTime {
            UserEvent.data
        }
        println("Done. used $used")

        println("load ages:")
        used = measureTime {
            AgeSupportEvents.data
        }
        println("Done. used $used")

        println("load talent:")
        used = measureTime {
            Talent.data
        }
        println("Done. used $used")
    }

    @Test
    fun testExclusive() {
        val error = kotlin.runCatching {
            LifeEngine {
                appearance = (0..15).random()
                strength = (0..15).random()
                intelligent = (0..15).random()
                money = (0..15).random()
                spirit = (0..15).random()
                this.talents = listOf(Talent.data[1004]!!, Talent.data[1003]!!)
            }
        }.exceptionOrNull() ?: error("Won't throw an error")
        assert(error.message == "选择了互相排斥的天赋！")
    }

    @Test
    fun game() {
        val selectList = LifeEngine.randomTalent()
        println("请在抽取的随机天赋中共选择3个：")
        println(" >直接输入对应的序号即可，数字间由英文逗号相隔(如: 1,2,3)")
        selectList.forEachIndexed { id, talent ->
            println("${id + 1}：${talent.name}")
            println("   ${talent.description}")
        }
        runBlocking {
            delay(1.seconds)
            val input = "1,2"
            val selected = input.split(",").mapNotNull { it.toIntOrNull() }.toSet()

            val talents =
                selected.map { selectList.getOrElse(it - 1) { selectList.random() } }.plus(Talent.data[1004]!!)

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

            println(
                """
|您选择天赋为：
|${engine.life.talents.joinToString("\n") { it.introduction }}
|
|您的初始属性点如下：
|智慧：${initialProperty.intelligent}, 力量：${initialProperty.strength}, 外貌：${initialProperty.appearance}
|快乐：${initialProperty.spirit}, 家境：${initialProperty.money}""".trimMargin()
            )

            for ((base, i) in lifeList.chunked(20).withIndex()) {
                println()
                for ((offset, event) in i.withIndex()) {
                    val age = (base * 20) + offset
                    println("${age}岁：${event.name}")
                }
            }

            println()
            with(engine.ratingStatus) {
                println("颜值：${appearance.value} ${appearance.judge}")
                println("家境：${money.value} ${money.judge}")
                println("乐观：${spirit.value} ${spirit.judge}")
                println("智力：${intelligent.value} ${intelligent.judge}")
                println("力量：${strength.value} ${strength.judge}")
                println("总分：${sum.value} ${sum.judge}")
            }

        }
    }
}