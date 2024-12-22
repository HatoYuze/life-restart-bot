package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.draw.GameLayoutDrawer
import com.github.hatoyuze.restarter.game.LifeEngine
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.Life
import com.github.hatoyuze.restarter.game.entity.TalentManager
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

    @Test
    fun game() {
        TestGame().data()
        TalentManager.talentRandom(3).map { it.id }.toMutableList()
        val life = Life()
        life.restartLife(
            Attribute(
                -1,
                (5..15).random(),
                (5..15).random(),
                (5..15).random(),
                (5..15).random(),
                (5..15).random(),
                1,
                talents = mutableListOf(1049, 1050, 1057)
            )

        )
        measureTimeMillis {
            GameLayoutDrawer.createGamingImage(life).also {
                println(it.absolutePath)
            }
        }.also {
            println("Drawing used $it ms")
        }
    }
}