package com.github.hatoyuze.restarter.mirai.config

import com.github.hatoyuze.restarter.game.entity.CustomizedTalentManager
import com.github.hatoyuze.restarter.game.entity.ITalentManager
import com.github.hatoyuze.restarter.game.entity.TalentManager
import com.github.hatoyuze.restarter.mirai.ResourceManager
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object GameConfig : AutoSavePluginConfig("game") {

    // https://github.com/mamoe/mirai/issues/1825
    @Serializable
    data class Limit(
        @ValueDescription("每个用户一天内（UTC+8 00:00 时刷新）的模拟人生最高次数。为 -1 时无限制")
        val userDailyGamingLimit: Int = -1,
        @ValueDescription("冷却的作用单位，可选为 GROUP 或者 SENDER\n - 选用 SUBJECT 表示冷却为整个 联系人 对象(可能为群聊 或者 好友)\n - 选用 SENDER 表示冷却指令发送者频率")
        val frequencyType: ContactType,
        @ValueDescription("执行指令冷却的时长，单位为秒\n为 -1 时表示无冷却")
        val frequencyLimitSeconds: Int = -1,
    ) {
        @Serializable
        enum class ContactType{
            SUBJECT, SENDER
        }
        companion object {
            val userDailyGamingLimit: Optional<Int>
                get() {
                    val data = limit.userDailyGamingLimit
                    return when  {
                        data < 0 -> Optional.empty()
                        else -> Optional.of(data)
                    }
                }
            val frequencyType: ContactType
                get() =  limit.frequencyType
            val frequencyLimitSeconds: Optional<Duration>
                get() {
                    val data = limit.frequencyLimitSeconds
                    return when  {
                        data < 0 -> Optional.empty()
                        else -> Optional.of(data.seconds)
                    }
                }

            fun Optional<*>.isNone() = this.getOrNull() == null
        }
    }

    @ValueDescription("最大总属性点，用户分配的属性点最终一定为该值（默认为20）")
    val maxAttributePoint: UInt by value(20.toUInt())

    @ValueDescription("在内存中缓存运行过程中创建的临时事件")
    val enableCacheTemporaryEvent: Boolean by value(false)

    @ValueDescription("缓存每一次游戏的结果，为 false 时不再缓存，且无法使用排行榜等功能")
    val enableGameSave: Boolean by value(true)

    @ValueDescription("缓存数据的过期时间，过期后这些数据将会被永久删除，单位为小时。为 -1 时永远不会删除。\n需要启用 enableGameSave 功能")
    val dataExpireTime: Int by value(-1)

    @ValueDescription("绘图使用的字体，如需自定义需要在此处填写字体文件的绝对路径\n默认下使用 HarmonyOS Sans Regular 字体")
    val defaultFont: String by value("")

    @ValueDescription("临时存储文件的目录\n在插件退出时，这些图片会被清除, 为空时表示插件的 data 目录")
    val cachePath: String by value("")

    @ValueDescription("关于人生模拟器的相关频率限制")
    val limit: Limit by value(Limit(userDailyGamingLimit = -1,  frequencyType = Limit.ContactType.SUBJECT, frequencyLimitSeconds = -1))

    @ValueDescription("生成的 JPG 人生模拟器图片压缩质量，越高质量越好，同时用时越久\n值需要大于 0，范围为 0 到 100，默认为 70")
    val jpgQuality: UInt by value(70.toUInt())

    @ValueDescription("默认的快乐值，为 -1 时表示需要调用方提供，反之则总是使用这个值为初始快乐值\n自 0.5.2 以后将默认设置为 5, 不需要调用方提供快乐值")
    val defaultSpiritPoint: UInt by value(5u)

    @Serializable
    data class TalentPoolSetting(
        val excludeIds: List<Int>,
        val prob1: Double = 0.333,
        val prob2: Double = 0.2,
        val prob3: Double = 0.1,
    ) {
        init {
            require(prob1 + prob2 + prob3 <= 1.0) { "天赋等级的概率不能为大于 1 的数" }
        }

        fun ofTalentManger() =
            CustomizedTalentManager(prob3, prob2, prob1)
    }


    @ValueDescription("天赋池设定\n" +
        " excludeIds - 始终不会被再次抽中的天赋\n" +
        " prob1 - 天赋等级为 1 (蓝色) 的出现概率\n" +
        " prob2 - 天赋等级为 2 (红色) 的出现概率\n" +
        " prob3 - 天赋等级为 3 (紫色) 的出现概率\n" +
        " 注： 上述概率之和不可大于 1, 白色天赋出现概率为 1 减去上述数据之和")
    val talentsPool: TalentPoolSetting by value(TalentPoolSetting(listOf(2032)))

    enum class PaginatedSendingWays {
        NEVER_PAGINATED,
        FORWARDING,
        FOLLOWING
    }

    @ValueDescription("分页绘图设定，可选以下值\n" +
        " NEVER_PAGINATED - 从不进行分页 (不推荐，可能会导致无法分配内存/发出信息)\n" +
        " FORWARDING - 将分页绘制后的图片装入“合并转发信息”发送\n" +
        " FOLLOWING - 直接将分页后的多个图片放进同一张图片中")
    val paginatedSetting: PaginatedSendingWays by value(PaginatedSendingWays.FOLLOWING)



    // ////////////////////////////////////////////////
    // //////////// Resolved Attributes  //////////////
    // ///////////////////////////////////////////////

    val isPaginatedEnable get() = paginatedSetting != PaginatedSendingWays.NEVER_PAGINATED

    val usingTalentManager: ITalentManager by lazy {
        if (ResourceManager.isTesting) {
            return@lazy TalentManager
        }
        return@lazy talentsPool.ofTalentManger()
    }


    val defaultSpirit: Optional<Int> by lazy {
        if (ResourceManager.isTesting) {
            return@lazy Optional.of(5)
        }

        if (defaultSpiritPoint < 0u) Optional.empty<Int>()
        else Optional.of(defaultSpiritPoint.toInt())
    }

    val quality: Int by lazy {
        if (ResourceManager.isTesting) 70
        else min(jpgQuality.toInt(),100)
    }
    fun String?.ifNull(replacement: String) = if (this.isNullOrEmpty()) replacement else this
}