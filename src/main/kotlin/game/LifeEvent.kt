package com.github.hatoyuze.restarter.game

data class LifeEvent(
    val name:String,
    val property: Attribute
) {
    data class Attribute(
        val appearance:Int,
        val intelligent:Int,
        val strength:Int,
        val spirit:Int,
        val lifeAge:Int,
        val money:Int
    )
}