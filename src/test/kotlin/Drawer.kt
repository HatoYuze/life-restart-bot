package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.draw.GameLayoutDrawer
import com.github.hatoyuze.restarter.game.LifeEngine
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.TalentManager
import com.github.hatoyuze.restarter.game.entity.impl.Life
import com.github.hatoyuze.restarter.game.entity.impl.LifeSave
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    val json = Json {
        prettyPrint = true
    }


    @Test
    fun game() {
        TestGame().data()
        TalentManager.talentRandom(3).map { it.id }.toMutableList()
        val life = Life()
        life.restartLife(
            Attribute(
                -1,
                3,4,5,6,2,
                1,
                talents = mutableListOf(1049, 1050, 1057)
            )

        )

        measureTimeMillis {
            GameLayoutDrawer.createGamingImage(life).also {
                println(it.absolutePath)
            }

            val lifeSave = LifeSave.translate(life)
            println(json.encodeToString(lifeSave))
            GameLayoutDrawer.createGamingImage(lifeSave).also {
                println(it.absolutePath)
            }
        }.also {
            println("Drawing used $it ms")
        }
    }
}
