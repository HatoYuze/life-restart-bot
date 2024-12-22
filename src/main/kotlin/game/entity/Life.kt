package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.AgeSupportEvents
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.data.UserEvent
import com.github.hatoyuze.restarter.game.data.UserEvent.Companion.ofEvent
import kotlinx.serialization.Serializable
import com.github.hatoyuze.restarter.game.data.UserEvent.Companion.data as GlobalEventLibrary

@Serializable
data class Life(
    private var triggerTalent: MutableList<Int> = mutableListOf(),
    var property: Property
) : Iterator<ExecutedEvent>, Sequence<ExecutedEvent> {
    constructor() : this(mutableListOf(), Property())

    fun restartLife(attribute: Attribute) {
        property.restartProperty(attribute)
        triggerTalent = ArrayList()
        doTalent()
    }

    /**
     * @return 成功执行的天赋
     * */
    private fun doTalent(): List<Talent> {
        val resultTalents = mutableListOf<Talent>()
        val talentsList = getUnfinishedTalent()
        talentsList.forEach { value ->
            val talent = TalentManager.talentTakeEffect(value, property.attribute) ?: return@forEach
            resultTalents.add(talent)
            talent.let {
                triggerTalent.add(value)
                property.takeEffect(it)
            }
        }
        return resultTalents
    }

    private fun nextEvents(): List<UserEvent> {
        with(property.attribute) {
            AttributeType.AGE += 1
        }
        val ageUserEvent = property.getAgeData()
        val userEventId = randomUserEvent(ageUserEvent)
        val finishedEvents = finishUserEvent(userEventId)

        val talentsEvent = doTalent().map { it.ofEvent() }
        finishedEvents.addAll(0, talentsEvent)

        return finishedEvents
    }

    val age: Int
        get() {
            return property.attribute.age
        }

    private fun finishUserEvent(userEventId: Int): MutableList<UserEvent> {
        val userEventList = ArrayList<UserEvent>()
        with(property.attribute) {
            AttributeType.EVT += userEventId
            val userEvent =
                GlobalEventLibrary[userEventId] ?: throw NullPointerException("Unknown event id $userEventId")

            userEventList.add(userEvent)
            userEvent.applyEffect(this)

            userEvent.branch.let { branches ->
                branches.forEach branch@{ (condition, refer) ->
                    if (refer == -1 || refer == 0) return@branch
                    if (condition.judgeAll(this)) {
                        finishUserEvent(refer).forEach {
                            userEventList.add(it)
                            if (it.id == DEAD_EVENT_ID) {
                                return@branch
                            }
                        }
                    }
                }
            }
        }

        return userEventList
    }

    private fun randomUserEvent(ageUserEvent: AgeSupportEvents): Int {
        val events = ageUserEvent.events
        val validEvents = events.filter { entry -> checkUserEvent(entry.key) }
            .mapValues { entry -> entry.value }

        if (validEvents.isEmpty()) {
            // 没有可继续的事件，强制死亡
            return 30003
        }

        val totalWeight = validEvents.values.sum()
        val randomCut = totalWeight * Math.random()

        var cumulativeWeight = 0.0
        for ((key, weight) in validEvents) {
            cumulativeWeight += weight
            if (randomCut <= cumulativeWeight) {
                return key
            }
        }

        return validEvents.keys.last()
    }

    private fun checkUserEvent(userEventId: Int): Boolean {
        val userEvent = GlobalEventLibrary[userEventId] ?: error("Event id $userEventId does NOT exist")
        return when {
            userEvent.noRandom -> false
            userEvent.exclude != null && userEvent.exclude.judgeAll(property.attribute) -> false
            else -> userEvent.include?.judgeAll(property.attribute) != false
        }
    }

    private fun getUnfinishedTalent(): List<Int> {
        val talentsList = property.attribute.talents
        return talentsList.filter { !triggerTalent.contains(it) }
    }

    private fun isLifeEnd(): Boolean {
        return property.attribute.isEnd()
    }

    override fun iterator(): Iterator<ExecutedEvent> {
        return this
    }

    override fun hasNext(): Boolean {
        return !isLifeEnd()
    }

    override fun next(): ExecutedEvent {
        val events = nextEvents()
        return ExecutedEvent(
            events.first(),
            events.drop(1)
        )
    }

    companion object {
        val Life.talents: List<Talent> get() = property.attribute.talents.map { Talent.data[it]!! }
        private const val DEAD_EVENT_ID = 10000
    }
}