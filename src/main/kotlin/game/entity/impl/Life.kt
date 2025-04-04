package com.github.hatoyuze.restarter.game.entity.impl

import com.github.hatoyuze.restarter.game.data.AgeSupportEvents
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.data.UserEvent
import com.github.hatoyuze.restarter.game.data.UserEvent.Data.ofEvent
import com.github.hatoyuze.restarter.game.entity.*
import com.github.hatoyuze.restarter.game.entity.ILife.Companion.talentManager
import kotlinx.serialization.Serializable
import com.github.hatoyuze.restarter.game.data.UserEvent.Data as GlobalEventLibrary

@Serializable
data class Life(
    private var triggerTalent: MutableList<Int> = mutableListOf(),
    override var property: Property
) : ILife {
    constructor() : this(mutableListOf(), Property())


    override val highestData: MutableMap<AttributeType, Int>
        get() =
            if (isLifeEnd()) property.attribute.highestData
            else throw NotImplementedError("You shouldn't get 'highestData' from com.github.hatoyuze.restarter.game.entity.impl.Life, because the highest data is still dynamic.")

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
            val talent = talentManager.talentTakeEffect(value, property.attribute) ?: return@forEach
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
        val talentsEvent = doTalent().map { it.ofEvent() }

        val finishedEvents = finishUserEvent(userEventId, talentsEvent)

        return finishedEvents
    }

    val age: Int
        get() {
            return property.attribute.age
        }

    private fun finishUserEvent(userEventId: Int, talentsEvent: List<UserEvent> = emptyList()): MutableList<UserEvent> {
        val userEventList = ArrayList<UserEvent>()
        with(property.attribute) {
            // 优先执行天赋信息
            for (event in talentsEvent) {
                AttributeType.EVT += event.id
                userEventList.add(event)
            }
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
        val validEvents = events
            .filter { entry -> checkUserEvent(entry.key) }
            .mapValues { entry -> entry.value }

        if (validEvents.isEmpty()) {
            // 没有可继续的事件，强制死亡
            return UserEvent.REPLACEABLE_EVENT_ID
        }
        fun randomEvent(): Int {
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
        var eventResult = randomEvent()

        if (property.attribute.age < 100 && eventResult in property.attribute.events) {
            for (i in 0 until 3) {
                if (eventResult !in property.attribute.events) {
                    break
                }
                eventResult = randomEvent()
            }
        }
        return eventResult
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

    override fun isLifeEnd(): Boolean {
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
            events.drop(1),
            LifeAttribute(property.attribute)
        )
    }

    companion object {
        private const val DEAD_EVENT_ID = 10000
    }
}