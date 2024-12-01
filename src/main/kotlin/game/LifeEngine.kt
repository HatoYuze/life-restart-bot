package com.github.hatoyuze.restarter.game

import com.github.hatoyuze.restarter.game.data.AgeSupportEvents
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.data.UserEvent
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.Life

class LifeEngine(builder: LifeEngineBuilder.() -> Unit) : Sequence<LifeEvent> {
    private val life = Life(
        userEventHashMap = eventMap,
        ageUserEventHashMap = ageMap,
        talentHashMap = talentMap
    )

    init {
        val initial = LifeEngineBuilder().apply(builder)
        val attr = Attribute(
            -1,
            initial.appearance,
            initial.intelligent,
            initial.strength,
            initial.money,
            initial.spirit,
            1,
            talents = initial.talents.map { it.id }.toMutableList()
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
        private val eventMap = UserEvent.data
        private val ageMap = AgeSupportEvents.data
        private val talentMap = Talent.data

        fun randomTalent(): List<Talent> = List(10) { talentMap.values.random() }

        fun searchTalent(id: Int): Talent = talentMap.entries.find { it.key == id }!!.value
    }

    val iterator = object : Iterator<LifeEvent> {
        override fun hasNext(): Boolean {
            return !life.isLifeEnd()
        }

        override fun next(): LifeEvent {
            val next = life.next()
            return LifeEvent(
                name = next.joinToString("\n") { it.first.eventName },
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