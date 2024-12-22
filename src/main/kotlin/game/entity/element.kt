package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.UserEvent
import kotlin.math.max

data class Judgement(val grade: Int, val value: Int, val judge: String) {
}

data class LifeAttribute(
    val appearance: Int,
    val intelligent: Int,
    val strength: Int,
    val spirit: Int,
    val lifeAge: Int,
    val money: Int
) {
    constructor(attribute: Attribute) : this(
        attribute.appearance,
        attribute.intelligent,
        attribute.strength,
        attribute.spirit,
        attribute.lifeAge,
        attribute.money
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
        fun ExecutedEvent.linesCount() = mainEvent.lineCount() + subEvents.sumOf { sub -> sub.lineCount() }
        fun ExecutedEvent.maxGrade() = max(mainEvent.grade, subEvents.maxOfOrNull { it.grade } ?: 0)
    }
}