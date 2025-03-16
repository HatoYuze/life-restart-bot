package com.github.hatoyuze.restarter.game

import com.github.hatoyuze.restarter.game.entity.Judgement

interface IRatingStatus{
    val appearanceSummary: Judgement
    val moneySummary: Judgement
    val spiritSummary: Judgement
    val intelligentSummary: Judgement
    val strengthSummary: Judgement
    val ageSummary: Judgement
    val sumSummary: Judgement
}


val IRatingStatus.appearance: Judgement get() = appearanceSummary
val IRatingStatus.money: Judgement get() = moneySummary
val IRatingStatus.spirit: Judgement get() = spiritSummary
val IRatingStatus.intelligent: Judgement get() = intelligentSummary
val IRatingStatus.strength: Judgement get() = strengthSummary
val IRatingStatus.age: Judgement get() = ageSummary
val IRatingStatus.sum: Judgement get() = sumSummary
