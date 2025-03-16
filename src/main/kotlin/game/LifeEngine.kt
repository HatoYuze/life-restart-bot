package com.github.hatoyuze.restarter.game

import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.ILife.Companion.talentManager
import com.github.hatoyuze.restarter.game.entity.impl.Life
import com.github.hatoyuze.restarter.mirai.config.GameSaveData
import net.mamoe.mirai.console.command.CommandContext

class LifeEngine(initial: LifeEngineBuilder) : Sequence<LifeEvent> {
    internal val life = Life()

    constructor(builder: LifeEngineBuilder.() -> Unit) : this(LifeEngineBuilder().apply(builder))

    init {
        val talents = initial.talents.map {
            if (it.replacement != null) {
                var tryCount = 0
                var replaceResult = it.replacement.replace()
                while (replaceResult in initial.talents) {
                    replaceResult = it.replacement.replace()
                    if (tryCount >= 10) {
                        throw IllegalStateException("在试图避免天赋重复的过程中发生了意料之外的问题，" +
                            "这可能是因为指定天赋的所有替换项都已被包含")
                    }
                    tryCount++
                }
                replaceResult
            } else it
        }


        val attr = Attribute(
            -1,
            initial.appearance,
            initial.intelligent,
            initial.strength,
            initial.money,
            initial.spirit,
            1,
            talents = talents.map { it.id }.toMutableList(),
            tms = initial.tms
        )
        life.restartLife(attr)
    }

    val ratingSummary by lazy {
        require(!this.iterator.hasNext()) {
            "未完成推演无法进行评分"
        }
        life.property.attribute as IRatingStatus
    }


    companion object {

        fun randomTalent(): List<Talent> = talentManager.talentRandom(10)

    }

    val iterator = object : Iterator<LifeEvent> {
        override fun hasNext(): Boolean {
            return life.hasNext()
        }

        override fun next(): LifeEvent {
            val next = life.next()
            return LifeEvent(
                name = next.desc(),
                property = with(life.property.attribute) {
                    LifeEvent.Attribute(
                        appearance,
                        intelligent,
                        strength,
                        spirit,
                        lifeAge,
                        money
                    )
                }
            )
        }
    }

    override fun iterator(): Iterator<LifeEvent> = iterator
}

inline fun CommandContext.LifeEngine(builder: LifeEngineBuilder.() -> Unit): LifeEngine {
    val lifeEngineBuilder = LifeEngineBuilder()
    lifeEngineBuilder.builder()
    lifeEngineBuilder.tms = GameSaveData.flushTmsData(sender.user?.id ?: -1)
    return LifeEngine(lifeEngineBuilder)
}