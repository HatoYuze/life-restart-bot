package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.AgeSupportEvents
import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.data.UserEvent
import kotlinx.serialization.Serializable

@Serializable
class Property @JvmOverloads constructor(
    var attribute: Attribute = Attribute(),
    var ageEventHashMap: Map<Int, AgeSupportEvents>
) {
    fun restartProperty(attribute: Attribute) {
        this.attribute = attribute
    }

    fun takeEffect(event: UserEvent) {
        event.applyEffect(attribute)
    }

    fun takeEffect(talent: Talent) {
        talent.applyEffect(attribute)
    }

    fun getAgeData(): AgeSupportEvents {
        return ageEventHashMap[attribute.age] ?: error("No events match age ${attribute.age}")
    }
}