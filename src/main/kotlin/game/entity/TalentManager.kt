package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.Talent
import kotlin.math.floor
import com.github.hatoyuze.restarter.game.data.Talent.Companion.data as talentHashMap

interface ITalentManager {
    /**
     * 实现对 天赋效果条件 的判断作用, 当符合条件时 返回 [talentId] 对应的 [Talent]
     * @return 不符合条件 或 不存在时为 null
     * */
    fun talentTakeEffect(talentId: Int, property: Attribute): Talent?

    /**
     * 随机抽取天赋
     * */
    fun talentRandom(listSize: Int): List<Talent>

    companion object {
        val INSTANCE = TalentManager
    }
}


object TalentManager : ITalentManager {
    private const val PERCENT_GRADE_3: Double = 0.1
    private const val PERCENT_GRADE_2: Double = 0.2
    private const val PERCENT_GRADE_1: Double = 0.333

    override fun talentTakeEffect(talentId: Int, property: Attribute): Talent? {
        val talent = talentHashMap[talentId] ?: return null
        return talent.condition?.let {
            if (!it.judgeAll(property)) null
            else talent
        } ?: talent
    }

    // won't show talentHashMap.
    override fun toString(): String {
        return "TalentManager {" +
            "percentageGrade3=" + PERCENT_GRADE_3 +
            ", percentageGrade2=" + PERCENT_GRADE_2 +
            ", percentageGrade1=" + PERCENT_GRADE_1 +
            '}'
    }

    override fun talentRandom(listSize: Int): List<Talent> {
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
                gradeRandom <= PERCENT_GRADE_3 -> 3
                gradeRandom <= PERCENT_GRADE_2 -> 2
                gradeRandom <= PERCENT_GRADE_1 -> 1
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