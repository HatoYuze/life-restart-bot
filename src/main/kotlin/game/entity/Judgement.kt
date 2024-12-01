package com.github.hatoyuze.restarter.game.entity

data class Judgement(val grade: Int, val value: Int, val judge: String) {
    val evaluate: String
        get() = "$value $judge"
}
