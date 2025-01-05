package com.github.hatoyuze.restarter.game

import com.github.hatoyuze.restarter.game.data.Talent


class LifeEngineBuilder {
    var appearance: Int = 5
    var intelligent: Int = 5
    var strength: Int = 5
    var money: Int = 5
    var spirit: Int = 5
    var tms: Int = 0
    var talents = listOf<Talent>()
        set(value) {
            val exclusive = mutableListOf<Int>()
            value.onEach { exclusive.addAll(it.exclude) }
            require(value.none { it.id in exclusive }) { "选择了互相排斥的天赋！" }
            field = value
        }
}