package com.github.hatoyuze.restarter.game.entity

import com.github.hatoyuze.restarter.game.data.Talent
import com.github.hatoyuze.restarter.game.data.Talent.Companion.data as talentHashMap

interface ITalentManager {
    /**
     * 实现对 天赋效果条件 的判断作用, 当符合条件时 返回 [talentId] 对应的 [Talent]
     * @return 不符合条件 或 不存在时为 null
     * */
    fun talentTakeEffect(talentId: Int, property: Attribute): Talent?

    /**
     * 随机抽取天赋
     * @param excludeIds 不会被抽中的 天赋
     * */
    fun talentRandom(listSize: Int, excludeIds: Set<Int> = emptySet()): List<Talent>

    companion object {
        val INSTANCE = TalentManager
    }
}


abstract class TalentManager : ITalentManager {
    abstract val percentageGrade3: Double
    abstract val percentageGrade2: Double
    abstract val percentageGrade1: Double

    override fun talentTakeEffect(talentId: Int, property: Attribute): Talent? {
        val talent = talentHashMap[talentId] ?: return null
        return talent.takeIf { talent.condition?.judgeAll(property) != false }
    }

    override fun toString(): String {
        return "TalentManager {" +
            "percentageGrade3=$percentageGrade3" +
            ", percentageGrade2=$percentageGrade2" +
            ", percentageGrade1=$percentageGrade1" +
            "}"
    }

    override fun talentRandom(listSize: Int, excludeIds: Set<Int>): List<Talent> {
        val talentList = mutableListOf<Talent>()
        val talentClassedByGrade = (0..3).associateWithTo(mutableMapOf()) { grade ->
            talentHashMap.values
                .filter { it.grade == grade && it.id !in excludeIds }
                .toMutableList()
        }

        repeat(listSize) {
            val availableGrades = (3 downTo 0).filter { grade ->
                talentClassedByGrade[grade]?.isNotEmpty() == true
            }
            if (availableGrades.isEmpty()) return@repeat

            val cumulativeProb = mutableListOf<Pair<Int, Double>>().apply {
                var cumulative = 0.0
                val totalProb = availableGrades.sumOf { getGradeProbability(it) }
                availableGrades.forEach { grade ->
                    val prob = getGradeProbability(grade) / totalProb
                    cumulative += prob
                    add(grade to cumulative)
                }
            }

            val random = Math.random()
            val selectedGrade = cumulativeProb.find { (_, prob) -> random <= prob }?.first
                ?: availableGrades.last()

            talentClassedByGrade[selectedGrade]?.let { talents ->
                talents.removeAt((Math.random() * talents.size).toInt()).also { talentList.add(it) }
            }
        }

        return talentList
    }

    private fun getGradeProbability(grade: Int): Double = when (grade) {
        3 -> percentageGrade3
        2 -> percentageGrade2
        1 -> percentageGrade1
        0 -> 1 - (percentageGrade1 + percentageGrade2 + percentageGrade3)
        else -> 0.0
    }

    companion object: TalentManager() {
        override val percentageGrade3: Double = 0.1
        override val percentageGrade2: Double = 0.2
        override val percentageGrade1: Double = 0.333
    }
}


data class CustomizedTalentManager(
    override val percentageGrade3: Double,
    override val percentageGrade2: Double,
    override val percentageGrade1: Double
) : TalentManager()