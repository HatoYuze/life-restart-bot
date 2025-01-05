package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.UserEvent
import kotlinx.serialization.Serializable
import kotlin.math.max

data class Judgement(val grade: Int, val value: Int, val judge: String) {
}

@Serializable
data class LifeAttribute(
    var appearance: Int,
    var intelligent: Int,
    var strength: Int,
    var spirit: Int,
    var lifeAge: Int,
    var money: Int
) {
    constructor(attribute: Attribute) : this(
        appearance = attribute.appearance,
        intelligent = attribute.intelligent,
        strength = attribute.strength,
        spirit = attribute.spirit,
        lifeAge = attribute.lifeAge,
        money = attribute.money
    )
}


data class ExecutedEvent(
    val mainEvent: UserEvent,
    // 部分事件拥有 `:` 的指向描述
    //  对于这些事件，同一岁中会出现多个事件，这些事件理应也被展示.
    val subEvents: List<UserEvent>,
    val attribute: LifeAttribute
) {
    fun desc() = buildString {
        appendLine(mainEvent.eventName)
        subEvents.forEach {
            appendLine(it.eventName)
        }
        deleteAt(lastIndex)
    }


    companion object {
        private fun UserEvent.lineCount() = eventName.split('\n').size
        fun ExecutedEvent.linesCount(): Int {
            var mainLineCount = mainEvent.lineCount()
            if (showPostEvent()) {
                mainLineCount += mainEvent.postEvent?.split('\n')?.size ?: 0
            }
            return mainLineCount + subEvents.sumOf { sub -> sub.lineCount() }
        }
        fun ExecutedEvent.showPostEvent() =
            mainEvent.postEvent != null &&
                (subEvents.isEmpty() || (mainEvent.isTalentEvent && subEvents.size == 1))

        fun ExecutedEvent.maxGrade() = max(mainEvent.grade, subEvents.maxOfOrNull { it.grade } ?: 0)
    }
}