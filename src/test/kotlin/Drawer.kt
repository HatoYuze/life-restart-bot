package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.draw.GameLayoutDrawer
import com.github.hatoyuze.restarter.game.LifeEngine
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class Drawer {
    @Test
    fun talents() {
        TestGame().data()

        measureTimeMillis {
            runBlocking {
                val file = GameLayoutDrawer.createTalentOptionImage(LifeEngine.randomTalent())
                println(file.absolutePath)
            }
        }.also {
            println("Drawing used $it ms")
        }


    }

}