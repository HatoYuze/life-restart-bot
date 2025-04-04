package com.github.hatoyuze.restarter.game.entity.impl

import com.github.hatoyuze.restarter.game.data.UserEvent
import com.github.hatoyuze.restarter.game.entity.*
import com.github.hatoyuze.restarter.game.entity.AttributeType.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.nameCardOrNick
import java.time.Instant
import java.util.*
import com.github.hatoyuze.restarter.game.data.UserEvent.Data as GlobalEventLibrary

@Serializable
data class LifeSave(
    @SerialName("s")
    val score: Int,
    @SerialName("a")
    val totalAge: Int,
    @SerialName("e")
    val events0: List<Int>,
    @SerialName("t")
    val talents0: List<Int>,
    // 为什么要使用 Base64 呢，这需要问问 mirai 序列化 yamlkt 的独特处理了
    // 一些特殊的字符可能无法被序列化，导致故障
    @SerialName("un64")
    val userNameBase64: String,
    @SerialName("ui")
    val userId: Long,
    @SerialName("c")
    val createAtTimestampUTC: Long,
    // attribute values,
    @SerialName("ta")
    val totalAttributes: List<Int>,
) : ILife {
    @Transient
    val events = events0.map { GlobalEventLibrary[it] ?: error("存档文件中的事件 id=$it 丢失") }

    private val attributeStatus by lazy {
        var lastAttribute = LifeAttribute(
            appearance = totalAttributes[APPEARANCE_INDEX],
            intelligent = totalAttributes[INTELLIGENT_INDEX],
            strength = totalAttributes[STRENGTH_INDEX],
            money = totalAttributes[MONEY_INDEX],
            spirit = totalAttributes[SPIRIT_INDEX],
            lifeAge = -1
        )
        events.reversed().map {
            it.inverseApply(lastAttribute).also { lastAttribute = it }
        }.reversed()
    }

    private val lifeEventList by lazy {
        val eventChain = mutableListOf<UserEvent>()
        val expectBranchEvent = mutableListOf<Int>()
        var inTalentsChain = false

        fun MutableList<ExecutedEvent>.judgeTail(index: Int, event: UserEvent) {
            when {
                inTalentsChain -> {
                    eventChain.add(event)
                    inTalentsChain = false
                }

                event.isTalentEvent -> {
                    eventChain.add(event)
                    inTalentsChain = true
                }
                // branch.isEmpty() 表明这是事件链的末尾
                event.branch.isEmpty() && eventChain.isEmpty() ->
                    add(ExecutedEvent(event, listOf(), attributeStatus[index]))

                event.branch.isEmpty() -> {
                    add(ExecutedEvent(eventChain.first(), eventChain.drop(1), attributeStatus[index]))
                    eventChain.clear()
                    expectBranchEvent.clear()
                }

                else -> {
                    expectBranchEvent.addAll(event.branch.map { it.second })
                    eventChain.add(event)
                }
            }
        }

        fun MutableList<ExecutedEvent>.execute(index: Int, event: UserEvent) {
            when {
                expectBranchEvent.isEmpty() -> {} // 上一级并不存在链，该项也不可能为链元素
                event.id !in expectBranchEvent -> { // 没有进入事件分支，进入了新的链
                    if (eventChain.isNotEmpty()) {
                        add(ExecutedEvent(eventChain.first(), eventChain.drop(1), attributeStatus[index]))
                    }
                    eventChain.clear()
                    expectBranchEvent.clear()
                }
            }
        }
        mutableListOf<ExecutedEvent>().apply {
            for ((index, event) in events.withIndex()) {
                execute(index, event)
                judgeTail(index, event)
            }
            // 用于存储最后一项事件，如果最后一个事件不是 `10000 你死了` 将会报错
            add(ExecutedEvent(eventChain.first(), eventChain.drop(1), attributeStatus.last()))
        }
    }

    @Transient
    var internalIterator0 = lifeEventList.listIterator()

    @Transient
    private var _internalIteratorIndex = 0
    private fun UserEvent.inverseApply(attribute: LifeAttribute): LifeAttribute {
        fun LifeAttribute.impl(key: AttributeType, value: Int) {
            when (key) {
                AGE -> lifeAge -= value
                CHR -> appearance -= value
                INT -> intelligent -= value
                STR -> strength -= value
                MNY -> money -= value
                SPR -> spirit -= value
                LIF -> lifeAge -= value
                EVT, AET, TLT, TMS -> {} // Won't change anything. Because these attributes only act on choosing events
                else -> error("Unsupported function: try applying $key effect")
            }
        }

        if (effect.isEmpty()) return attribute
        val result = attribute.copy()
        for ((key, value) in effect) {
           result.impl(key, value)
        }
        return result
    }
    companion object {
        private val encoder = Base64.getEncoder()
        private val decoder = Base64.getDecoder()

        fun String.decodeBase64() = decoder.decode(this).decodeToString()

        /**
         * 会消耗 [life] 迭代器直到迭代完成
         * */
        fun translate(life: Life, context: User? = null): LifeSave {
            if (!life.isLifeEnd()) {
                while (life.hasNext()) {
                    life.next()
                }
            }
            return with(life.property.attribute) {
                println("events: "+events.size)
                LifeSave(
                    score = sumSummary.value,
                    totalAge = age,
                    events0 = events,
                    talents0 = talents,
                    // I want to use kotlin.io.encoding.Base64 but it gives a console warning when called, oops.
                    userNameBase64 = encoder.encode((context?.nameCardOrNick ?: "Console").toByteArray()).decodeToString(),
                    userId = context?.id ?: -1,
                    createAtTimestampUTC = Instant.now().toEpochMilli(),
                    totalAttributes = listOf(appearance, intelligent, strength, money, spirit),
                )
            }
        }

        private const val APPEARANCE_INDEX = 0
        private const val INTELLIGENT_INDEX = 1
        private const val STRENGTH_INDEX = 2
        private const val MONEY_INDEX = 3
        private const val SPIRIT_INDEX = 4
    }

    override val property: Property
        get() {
            val attributeData = lifeEventList[_internalIteratorIndex].attribute
            return Property(
                Attribute(
                    age = _internalIteratorIndex,
                    appearance = attributeData.appearance,
                    intelligent = attributeData.intelligent,
                    strength = attributeData.strength,
                    money = attributeData.money,
                    spirit = attributeData.spirit
                )
            )
        }

    // need better implementation
    override val highestData: MutableMap<AttributeType, Int> = mutableMapOf<AttributeType, Int>(
        CHR to attributeStatus.maxOf { it.appearance },
        INT to attributeStatus.maxOf { it.intelligent },
        STR to attributeStatus.maxOf { it.strength },
        MNY to attributeStatus.maxOf { it.money },
        SPR to attributeStatus.maxOf { it.spirit },
        AGE to attributeStatus.lastIndex // age is based-zero
    )

    private fun reset() {
        internalIterator0 = lifeEventList.listIterator()
        _internalIteratorIndex = 0
    }
    override fun isLifeEnd(): Boolean =
        !hasNext()

    override fun hasNext(): Boolean {
        if (!internalIterator0.hasNext()) {
            reset()
            return false
        }
        return true
    }

    override fun next(): ExecutedEvent {
        return internalIterator0.next().also {
            _internalIteratorIndex++
        }
    }

    override fun iterator(): Iterator<ExecutedEvent> {
        if (!internalIterator0.hasNext()) {
            reset()
        }
        return internalIterator0
    }
}
