package com.github.hatoyuze.restarter.game

import com.github.hatoyuze.restarter.game.entity.Judgement

data class RatingStatus(
    val appearance: Judgement,
    val money: Judgement,
    val spirit: Judgement,
    val intelligent: Judgement,
    val strength: Judgement,
    val age: Judgement,
    val sum: Judgement
)