package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.UserEvent

data class Judgement(val grade: Int, val value: Int, val judge: String) {
    val evaluate: String
        get() = "$value $judge"
}


data class ExecutedEvent(
    val mainEvent: UserEvent,
    // 部分事件拥有 `:` 的指向描述
    //  对于这些事件，同一岁中会出现多个事件，这些事件理应也被展示.
    val subEvents: List<UserEvent>
) {
    fun desc() = buildString {
        appendLine(mainEvent.eventName)
        subEvents.forEach {
            appendLine(it.eventName)
        }
        deleteAt(lastIndex)
    }
}