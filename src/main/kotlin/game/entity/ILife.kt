package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.entity.impl.Life
import com.github.hatoyuze.restarter.game.entity.impl.LifeSave

interface ILife : Iterator<ExecutedEvent>, Sequence<ExecutedEvent> {
    fun isLifeEnd(): Boolean
    val property: Property
    val talents: List<Talent>
        get() = when(this) {
            is Life -> property.attribute.talents.map { Talent.data[it] ?: error("天赋 $it 丢失") }
            is LifeSave -> talents0.map { Talent.data[it] ?: error("天赋 $it 丢失") }
            else -> error("Unsupported talents object")
        }
    val highestData: MutableMap<AttributeType, Int>
}