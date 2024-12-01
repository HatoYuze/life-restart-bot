package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.AgeSupportEvents
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.data.UserEvent
import kotlinx.serialization.Serializable
import java.util.*

typealias UserEventToAge = Pair<UserEvent, Int>

@Serializable
data class Life(
    var triggerTalent: MutableList<Int> = mutableListOf(),
    var property: Property,
    var userEventHashMap: Map<Int, UserEvent>,
    var talents: TalentManager
) {
    constructor(userEventHashMap: Map<Int, UserEvent>,
                talentHashMap: Map<Int, Talent>,
                ageUserEventHashMap: Map<Int, AgeSupportEvents>) : this(
        mutableListOf(),
        Property(ageEventHashMap = ageUserEventHashMap),
        userEventHashMap,
        TalentManager(talentHashMap)
    )

    fun getUserEventList(): List<UserEvent> {
        val userEventList = property.attribute.events
        val ans = ArrayList<UserEvent>()
        userEventList.forEach { ans.add(userEventHashMap[it]!!) }
        return ans
    }

    fun restartLife(attribute: Attribute) {
        property.restartProperty(attribute)
        triggerTalent = ArrayList()
        doTalent()
    }

    fun doTalent() {
        val talentsList = getUnfinishedTalent()
        talentsList.forEach { value ->
            val talent = talents.talentTakeEffect(value, property.attribute)
            talent?.let {
                triggerTalent.add(value)
                property.takeEffect(it)
            }
        }
    }

    fun next(): List<UserEventToAge> {
        with(property.attribute) {
            AttributeType.AGE += 1
        }
        doTalent()
        val ageUserEvent = property.getAgeData()
        val userEventId = randomUserEvent(ageUserEvent)
        return finishUserEvent(userEventId)
    }

    val age: Int get() {
        return property.attribute.age
    }

    fun finishUserEvent(userEventId: Int): List<UserEventToAge> {
        val userEventList = ArrayList<UserEventToAge>()
        with(property.attribute) {
            AttributeType.EVT += userEventId
            val userEvent = userEventHashMap[userEventId]!!
            userEventList.add(UserEventToAge(userEvent, age))
            userEvent.applyEffect(this)

            userEvent.branch.let { branches ->
                branches.forEach { (condition, refer) ->
                    if (refer == -1) return@forEach
                    if (condition.judgeAll(this)) {
                        finishUserEvent(refer).forEach {
                            userEventList.add(it)
                        }
                    }
                }
            }
        }

        return userEventList
    }

    fun randomTalent(listSize: Int): List<Talent> {
        return talents.talentRandom(listSize)
    }

    fun getTalentManager(): TalentManager {
        return talents
    }


    fun randomUserEvent(ageUserEvent: AgeSupportEvents): Int {
        val ageUserEventHashMap = ageUserEvent.events
        val ageUserEventCheckedHashMap = HashMap<Int, Double>()
        ageUserEventHashMap.keys.forEach { key ->
            if (checkUserEvent(key)) {
                ageUserEventCheckedHashMap[key] = ageUserEventHashMap[key] ?: error("Unknown error, $ageUserEventHashMap excludes $key")
            }
        }
        var totalWeight = 0.0
        ageUserEventCheckedHashMap.keys.forEach { key ->
            totalWeight += ageUserEventHashMap[key] ?: 0.0
        }
        var randomWeight = totalWeight * Math.random()
        ageUserEventCheckedHashMap.keys.forEach { key ->
            randomWeight -= ageUserEventHashMap[key] ?: 0.0
            if (randomWeight <= 0) {
                return key
            }
        }
        return 0
    }

    fun checkUserEvent(userEventId: Int): Boolean {
        val userEvent = userEventHashMap[userEventId] ?: error("Event id $userEventId does NOT exist")
        return when{
            userEvent.noRandom -> false
            userEvent.exclude != null && userEvent.exclude.judgeAll(property.attribute) -> false
            else -> userEvent.include?.judgeAll(property.attribute) != false
        }
    }

    fun getUnfinishedTalent(): List<Int> {
        val talentsList = property.attribute.talents
        return talentsList.filter { !triggerTalent.contains(it) }
    }

    fun isLifeEnd(): Boolean {
        return property.attribute.isEnd()
    }
}