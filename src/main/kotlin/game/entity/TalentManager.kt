package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.Talent
import kotlinx.serialization.Serializable
import kotlin.math.floor

@Serializable
class TalentManager private constructor(
    val talentHashMap: Map<Int,Talent>,
    private val percentageGrade3: Double = 0.1,
    private val percentageGrade2: Double = 0.2,
    private val percentageGrade1: Double = 0.333,
) {
    constructor(talentHashMap: Map<Int, Talent>) : this(talentHashMap, 0.1, 0.2, 0.333)

    /**
     * @return 不符合条件 或 不存在时为 null
     * */
    fun talentTakeEffect(talentId: Int, property: Attribute): Talent? {
        val talent = talentHashMap[talentId] ?: return null
        return talent.condition?.let {
            if (!it.judgeAll(property)) null
            else talent
        }
    }

    // won't show talentHashMap.
    override fun toString(): String {
        return "Talents{" +
            "percentageGrade3=" + percentageGrade3 +
            ", percentageGrade2=" + percentageGrade2 +
            ", percentageGrade1=" + percentageGrade1 +
            '}'
    }

    fun talentRandom(listSize: Int): List<Talent> {
        val talentList: MutableList<Talent> = ArrayList()
        val talentClassedByGrade = HashMap<Int, MutableList<Talent>>()
        for (i in 0..3) talentClassedByGrade[i] = ArrayList()
        val iterator: Iterator<*> = talentHashMap.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next() as Int
            val value = talentHashMap[key] ?: continue
            talentClassedByGrade[value.grade]!!.add(value)
        }
        for (i in 0 until listSize) {
            var grade: Int
            val gradeRandom = Math.random()
            grade = when {
                gradeRandom <= percentageGrade3 -> 3
                gradeRandom <= percentageGrade2 -> 2
                gradeRandom <= percentageGrade1 -> 1
                else -> 0
            }
            val len = talentClassedByGrade[grade]!!.size
            val talentRandom = floor(len * Math.random()).toInt() % len

            talentList.add(talentClassedByGrade[grade]!![talentRandom])
            talentClassedByGrade[grade]!!.removeAt(talentRandom)
        }
        return talentList
    }
}