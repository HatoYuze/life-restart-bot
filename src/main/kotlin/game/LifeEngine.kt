package com.github.hatoyuze.restarter.game

import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.Life
import com.github.hatoyuze.restarter.game.entity.TalentManager

class LifeEngine(builder: LifeEngineBuilder.() -> Unit) : Sequence<LifeEvent> {
    private val life = Life()
    var talent: List<Talent>

    init {
        val initial = LifeEngineBuilder().apply(builder)
        val talents = initial.talents.map {
            if (it.replacement != null) {
                it.replacement.replace()
            } else it
        }

        talent = talents

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
        private val talentMap = Talent.data

        fun randomTalent(): List<Talent> = TalentManager.talentRandom(10)

        fun searchTalent(id: Int): Talent = talentMap.entries.find { it.key == id }!!.value
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