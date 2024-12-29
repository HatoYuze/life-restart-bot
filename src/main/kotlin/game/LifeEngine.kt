package com.github.hatoyuze.restarter.game

import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.TalentManager
import com.github.hatoyuze.restarter.game.entity.impl.Life

class LifeEngine(builder: LifeEngineBuilder.() -> Unit) : Sequence<LifeEvent> {
    internal val life = Life()

    init {
        val initial = LifeEngineBuilder().apply(builder)
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
            talents = talents.map { it.id }.toMutableList()
        )
        life.restartLife(attr)
    }

    val ratingStatus by lazy {
        require(!this.iterator.hasNext()) {
            "未完成推演无法进行评分"
        }
        with(life.property.attribute) {
            RatingStatus(
                appearanceSummary,
                moneySummary,
                spiritSummary,
                intelligentSummary,
                strengthSummary,
                ageSummary,
                sumSummary
            )
        }
    }


    companion object {

        fun randomTalent(): List<Talent> = TalentManager.talentRandom(10)

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