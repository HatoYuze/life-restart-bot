package com.github.hatoyuze.restarter.game.entity

import kotlinx.serialization.Serializable

@Serializable
enum class AttributeType(val chineseDesc: String) {
    AGE("年龄"),  //年龄
    CHR("颜值"),  //颜值
    INT("智力"),  //智力
    STR("体质"),  //体质
    MNY("家境"),  //家境
    SPR("快乐"),  //快乐
    TLT("天赋"),  //天赋
    LIF("寿命"),  //寿命
    EVT("事件的 id"),  //事件
    RDM("随机属性"),  //随机
    TMS("重生次数"),
    AET("上一个事件的 id"),  //前一个事件
}

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
private annotation class ProtectedSetter

@Serializable
data class Attribute @JvmOverloads constructor(
    @set:ProtectedSetter var age: Int = -1, // 年龄
    @set:ProtectedSetter var appearance: Int = 5, // 颜值 U+1F60E
    @set:ProtectedSetter var intelligent: Int = 5, // 智力 🧠 U+1F9E0
    @set:ProtectedSetter var strength: Int = 5, // 体质 💪 U+1F4AA
    @set:ProtectedSetter var money: Int = 5, // 家境 💵 U+1F4B5
    @set:ProtectedSetter var spirit: Int = 5, // 快乐 🥰 U+1F970
    @set:ProtectedSetter var lifeAge: Int = 1, // 生命 ❤️ U+2764
    @set:ProtectedSetter var tms: Int = 0,
    val talents: MutableList<Int> = mutableListOf(),
    val events: MutableList<Int> = mutableListOf(),
    @set:ProtectedSetter var additionAttr: Int = 0,
    val highestData: MutableMap<AttributeType, Int> =
        mutableMapOf<AttributeType, Int>(
            AttributeType.CHR to appearance,
            AttributeType.INT to intelligent,
            AttributeType.STR to strength,
            AttributeType.MNY to money,
            AttributeType.SPR to spirit,
        )
) {

    fun getPropInteger(prop: AttributeType): Int = when (prop) {
        AttributeType.AGE -> age
        AttributeType.CHR -> appearance
        AttributeType.INT -> intelligent
        AttributeType.STR -> strength
        AttributeType.MNY -> money
        AttributeType.SPR -> spirit
        AttributeType.LIF -> lifeAge
        AttributeType.TMS -> tms
        AttributeType.AET -> events.last()
        AttributeType.RDM -> {
            val randomNumber = (Math.random() * 5).toInt()
            when (randomNumber) {
                0 -> appearance
                1 -> intelligent
                2 -> strength
                3 -> money
                4 -> spirit
                else -> 0
            }
        }

        else -> 0
    }

    fun getPropArray(prop: AttributeType): List<Int> = when (prop) {
        AttributeType.EVT -> events
        AttributeType.TLT -> talents
        else -> ArrayList()
    }

    @OptIn(ProtectedSetter::class)
    operator fun AttributeType.plusAssign(value: Int) {
        when (this) {
            AttributeType.AGE -> age = (age + value).checkHigher(this, highestData)
            AttributeType.CHR -> appearance = (appearance + value).checkHigher(this, highestData)
            AttributeType.INT -> intelligent = (intelligent + value).checkHigher(this, highestData)
            AttributeType.STR -> strength = (strength + value).checkHigher(this, highestData)
            AttributeType.MNY -> money = (money + value).checkHigher(this, highestData)
            AttributeType.SPR -> spirit = (spirit + value).checkHigher(this, highestData)
            AttributeType.LIF -> lifeAge = lifeAge + value
            AttributeType.RDM -> repeat(value) {
                val randomNumber = (Math.random() * 5).toInt()
                when (randomNumber) {
                    0 -> appearance = (appearance + 1).checkHigher(this, highestData)
                    1 -> intelligent = (intelligent + 1).checkHigher(this, highestData)
                    2 -> strength = (strength + 1).checkHigher(this, highestData)
                    3 -> money = (money + 1).checkHigher(this, highestData)
                    4 -> spirit = (spirit + 1).checkHigher(this, highestData)
                }
            }

            AttributeType.EVT -> {
                if (value != -1 && !includeEvent(value)) events.add(value)
            }

            AttributeType.TLT -> if (value != -1 && !includeTalent(value)) talents.add(value)
            AttributeType.TMS -> tms += value
            else -> println("Can not set $this to $value")
        }
    }

    fun includeEvent(eventId: Int): Boolean = eventId in events
    fun includeTalent(talentId: Int): Boolean = talentId in talents
    fun isEnd(): Boolean = lifeAge <= 0

    val appearanceSummary: Judgement
        get() = when {
            appearance >= 11 -> Judgement(3, appearance, "逆天")
            appearance >= 9 -> Judgement(2, appearance, "罕见")
            appearance >= 7 -> Judgement(1, appearance, "优秀")
            appearance >= 4 -> Judgement(0, appearance, "普通")
            appearance >= 2 -> Judgement(0, appearance, "不佳")
            appearance >= 1 -> Judgement(0, appearance, "折磨")
            else -> Judgement(0, appearance, "地狱")
        }

    val intelligentSummary: Judgement
        get() = when {
            intelligent >= 501 -> Judgement(3, intelligent, "仙魂")
            intelligent >= 131 -> Judgement(3, intelligent, "元神")
            intelligent >= 21 -> Judgement(3, intelligent, "识海")
            intelligent >= 11 -> Judgement(3, intelligent, "逆天")
            intelligent >= 9 -> Judgement(2, intelligent, "罕见")
            intelligent >= 7 -> Judgement(1, intelligent, "优秀")
            intelligent >= 4 -> Judgement(0, intelligent, "普通")
            intelligent >= 2 -> Judgement(0, intelligent, "不佳")
            intelligent >= 1 -> Judgement(0, intelligent, "折磨")
            else -> Judgement(0, intelligent, "地狱")
        }

    val strengthSummary: Judgement
        get() = when {
            strength >= 2001 -> Judgement(3, strength, "仙体")
            strength >= 1001 -> Judgement(3, strength, "元婴")
            strength >= 401 -> Judgement(3, strength, "金丹")
            strength >= 101 -> Judgement(3, strength, "筑基")
            strength >= 21 -> Judgement(3, strength, "凝气")
            strength >= 11 -> Judgement(3, strength, "逆天")
            strength >= 9 -> Judgement(2, strength, "罕见")
            strength >= 7 -> Judgement(1, strength, "优秀")
            strength >= 4 -> Judgement(0, strength, "普通")
            strength >= 2 -> Judgement(0, strength, "不佳")
            strength >= 1 -> Judgement(0, strength, "折磨")
            else -> Judgement(0, strength, "地狱")
        }

    val ageSummary: Judgement
        get() = when {
            age >= 500 -> Judgement(3, age, "仙寿")
            age >= 100 -> Judgement(3, age, "修仙")
            age >= 95 -> Judgement(3, age, "不老")
            age >= 90 -> Judgement(2, age, "南山")
            age >= 80 -> Judgement(2, age, "杖朝")
            age >= 70 -> Judgement(1, age, "古稀")
            age >= 60 -> Judgement(1, age, "花甲")
            age >= 40 -> Judgement(0, age, "中年")
            age >= 18 -> Judgement(0, age, "盛年")
            age >= 10 -> Judgement(0, age, "少年")
            age >= 1 -> Judgement(0, age, "早夭")
            else -> Judgement(0, age, "胎死腹中")
        }

    val sumSummary: Judgement
        get() {
            // https://github.com/VickScarlet/lifeRestart/blob/master/src/modules/property.js#L198
            val sum =
                (highestData.chr + highestData.int + highestData.str + highestData.mny + highestData.spr) * 2 +
                    highestData.age / 2

            return when {
                sum >= 120 -> Judgement(3, sum, "传说")
                sum >= 110 -> Judgement(3, sum, "逆天")
                sum >= 100 -> Judgement(2, sum, "罕见")
                sum >= 80 -> Judgement(1, sum, "优秀")
                sum >= 60 -> Judgement(0, sum, "普通")
                sum >= 50 -> Judgement(0, sum, "不佳")
                sum >= 41 -> Judgement(0, sum, "折磨")
                else -> Judgement(0, sum, "地狱")
            }
        }

    val moneySummary: Judgement
        get() = if (money >= 11) {
            Judgement(3, money, "逆天")
        } else if (money >= 9) {
            Judgement(2, money, "罕见")
        } else if (money >= 7) {
            Judgement(1, money, "优秀")
        } else if (money >= 4) {
            Judgement(0, money, "普通")
        } else if (money >= 2) {
            Judgement(0, money, "不佳")
        } else if (money >= 1) {
            Judgement(0, money, "折磨")
        } else {
            Judgement(0, money, "地狱")
        }

    val spiritSummary: Judgement
        get() = if (spirit >= 11) {
            Judgement(3, spirit, "天命")
        } else if (spirit >= 9) {
            Judgement(2, spirit, "极乐")
        } else if (spirit >= 7) {
            Judgement(1, spirit, "幸福")
        } else if (spirit >= 4) {
            Judgement(0, spirit, "普通")
        } else if (spirit >= 2) {
            Judgement(0, spirit, "不幸")
        } else if (spirit >= 1) {
            Judgement(0, spirit, "折磨")
        } else {
            Judgement(0, spirit, "地狱")
        }

    companion object {
        private fun Int.checkHigher(type: AttributeType, highestMap: MutableMap<AttributeType, Int>) = this.apply {
            if ((highestMap[type] ?: 0) < this) {
                highestMap[type] = this
            }
        }

        val MutableMap<AttributeType, Int>.chr: Int
            get() = this[AttributeType.CHR] ?: 0
        val MutableMap<AttributeType, Int>.int: Int
            get() = this[AttributeType.INT] ?: 0
        val MutableMap<AttributeType, Int>.str: Int
            get() = this[AttributeType.STR] ?: 0
        val MutableMap<AttributeType, Int>.mny: Int
            get() = this[AttributeType.MNY] ?: 0
        val MutableMap<AttributeType, Int>.spr: Int
            get() = this[AttributeType.SPR] ?: 0
        val MutableMap<AttributeType, Int>.age: Int
            get() = this[AttributeType.AGE] ?: 0
    }
}