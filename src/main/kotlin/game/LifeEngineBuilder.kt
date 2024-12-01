package com.github.hatoyuze.restarter.game

import com.github.hatoyuze.restarter.game.data.Talent


class LifeEngineBuilder {
    var appearance: Int = 5
    var intelligent: Int = 5
    var strength: Int = 5
    var money: Int = 5
    var spirit: Int = 5
    var talents = listOf<Talent>()
}