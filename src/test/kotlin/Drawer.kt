package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.draw.GameLayoutDrawer
import com.github.hatoyuze.restarter.game.LifeEngine
import com.github.hatoyuze.restarter.game.entity.Attribute
import com.github.hatoyuze.restarter.game.entity.TalentManager
import com.github.hatoyuze.restarter.game.entity.impl.Life
import com.github.hatoyuze.restarter.game.entity.impl.LifeSave
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
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
    fun performanceTesting() = runBlocking {
        TestGame().data()
        val gameSave = json.decodeFromString<LifeSave>(RUNTIME_SAVE_JSON)

        val list = mutableListOf(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
        repeat(10) { times ->
            measureTimeMillis {
                for (i in 0..50) {
                    GameLayoutDrawer.createGamingImage(gameSave)
                }
            }.also {
                println("used $it ms")
                list[times] = it
            }
        }
        println(list)
        println(list.sum() / 10)


    }

    @Test
    fun game() = runBlocking {
        TestGame().data()
        TalentManager.talentRandom(3).map { it.id }.toMutableList()
        val life = Life()
        life.restartLife(
            Attribute(
                -1,
                13, 14, 15, 16, 12,
                1,
                talents = mutableListOf(1048, 1147, 1141)
            )

        )

        measureTimeMillis {
            GameLayoutDrawer.createGamingImage(life).also {
                println(it.joinToString { it.absolutePath })
            }

            val lifeSave = LifeSave.translate(life)
            println(json.encodeToString(lifeSave))
            GameLayoutDrawer.createGamingImage(lifeSave).also {
                println(it.joinToString { it.absolutePath })
            }
            GameLayoutDrawer.createGamingImage(lifeSave).also {
                println(it.joinToString { it.absolutePath })
            }
        }.also {
            println("Drawing used $it ms")
        }
        Unit
    }


    companion object {
        const val RUNTIME_SAVE_JSON = """
{
    "s": 45,
    "a": 83,
    "e": [
        10001,
        10010,
        10003,
        10824,
        10860,
        10861,
        10870,
        10923,
        10038,
        10062,
        10040,
        10928,
        10885,
        10921,
        10940,
        10989,
        11023,
        10945,
        10980,
        10988,
        11077,
        11212,
        11099,
        11106,
        11125,
        11127,
        11168,
        10411,
        11165,
        10459,
        10416,
        10448,
        -98951,
        10451,
        11171,
        11240,
        11281,
        11384,
        10423,
        20423,
        11283,
        11369,
        11254,
        11282,
        -98950,
        11394,
        11386,
        11398,
        11285,
        11389,
        11264,
        10422,
        10424,
        11414,
        11443,
        10070,
        10791,
        10806,
        10795,
        10810,
        21326,
        10798,
        11470,
        10458,
        10794,
        11298,
        10457,
        11315,
        11418,
        11432,
        11445,
        11257,
        10443,
        11434,
        10444,
        10316,
        11427,
        11442,
        11499,
        10698,
        10312,
        10313,
        11422,
        11429,
        11317,
        11501,
        10713,
        11431,
        11310,
        10000
    ],
    "t": [
        1049,
        1050,
        1141
    ],
    "un64": "Q29uc29sZQ==",
    "ui": -1,
    "c": 1736656957218,
    "ta": [
        5,
        8,
        3,
        9,
        2
    ]
}"""
    }
}
