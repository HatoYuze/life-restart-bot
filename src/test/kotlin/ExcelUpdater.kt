package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.game.data.ConditionExpression
import com.github.hatoyuze.restarter.game.data.UserEvent
import com.github.hatoyuze.restarter.game.data.serialization.ReferEventId
import com.github.hatoyuze.restarter.game.entity.AttributeType
import com.github.hatoyuze.restarter.mirai.ResourceManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.api.drop
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.io.NameRepairStrategy
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis

private val json = Json {
    prettyPrint = true
}

class ExcelUpdater {
    //$id	event	grade	postEvent	effect:CHR	effect:INT	effect:STR	effect:MNY	effect:SPR	effect:LIF	effect:AGE	NoRandom	include	exclude	branch[]	branch[]	branch[]
    data class Event(
        @ColumnName("\$id") val id: Int,
        val event: String,
        val grade: Int? = 0,
        val postEvent: String? = null,

        val include: String = "",
        val exclude: String = ""
    )

    @Test
    fun read() {
        ResourceManager.isTesting = true
        println("Current work dir -> ${System.getProperty("user.dir")}")

        var events: List<UserEvent>
        measureTimeMillis {
            events = resolveXlsx()
        }.also {
            println("Resolved events used $it ms")
        }
        val result = events.associateBy { it.id }
        TestGame().data()
        val new = events.filter { UserEvent.Data[it.id] == null }.size

        val keep = UserEvent.data.filter { result[it.key] == null }
        events = events.toMutableList().also { it.addAll(keep.values) }
        println("Upload event count: $new")

        val jsonContent: String
        measureTimeMillis {
            jsonContent = json.encodeToString(result)
        }.also {
            println("Build json used $it ms")
        }

        val objPath = System.getProperty("user.dir") + "/src/main/resources/data/events1.json"
        File(objPath).also { it.createNewFile() }.writeText(jsonContent)
        println("New content was written to $objPath")
    }

    private fun resolveXlsx(): List<UserEvent> {
        val df = DataFrame.readExcel(
            // Download from -> https://raw.githubusercontent.com/VickScarlet/lifeRestart/refs/heads/master/data/zh-cn/events.xlsx
            "https://ghproxy.net/https://raw.githubusercontent.com/VickScarlet/lifeRestart/refs/heads/master/data/zh-cn/events.xlsx",
            nameRepairStrategy = NameRepairStrategy.MAKE_UNIQUE,
            firstRowIsHeader = true,
            // That is too bad
            //  But dataframe make it keep bad
            //  IDK why, it can not ignore the empty line, I must specify the length of the data
            rowsCount = 1720
        )
            .drop(1) // comment line
            .sortBy(Event::id)
            .rows()
        return df.map {
            val noRandom = (it["NoRandom"] as? Double ?: 0.0) == 1.0
            val grade = (it["grade"] as? Double)?.toInt() ?: 0
            val effect: MutableMap<AttributeType, Int> = mutableMapOf()
            (it["effect:CHR"] as? Double)?.toInt()?.let { data -> effect[AttributeType.CHR] = data }
            (it["effect:INT"] as? Double)?.toInt()?.let { data -> effect[AttributeType.INT] = data }
            (it["effect:STR"] as? Double)?.toInt()?.let { data -> effect[AttributeType.STR] = data }
            (it["effect:MNY"] as? Double)?.toInt()?.let { data -> effect[AttributeType.MNY] = data }
            (it["effect:SPR"] as? Double)?.toInt()?.let { data -> effect[AttributeType.SPR] = data }
            (it["effect:LIF"] as? Double)?.toInt()?.let { data -> effect[AttributeType.LIF] = data }
            (it["effect:AGE"] as? Double)?.toInt()?.let { data -> effect[AttributeType.AGE] = data }
            val branch = listOf(
                it["branch[]"],
                it["branch[]1"],
                it["branch[]2"]
            ).mapNotNull { data -> deserializeRefer(data as? String) }
            UserEvent(
                it[Event::id],
                it[Event::event],
                include = ConditionExpression.parseExpression(it[Event::include].replace("AEVT", "AET")),
                exclude = ConditionExpression.parseExpression(it[Event::exclude].replace("AEVT", "AET")),
                grade = grade,
                postEvent = it[Event::postEvent].nullIfEmpty(),
                noRandom = noRandom,
                branch = branch,
                effect = effect
            )
        }
    }

    private fun deserializeRefer(content: String?): Pair<ConditionExpression, ReferEventId>? {
        !content.isNullOrEmpty() || return null
        val referSymbol = content!!.lastIndexOf(':')
        return if (referSymbol == -1) {
            return null
        } else {
            ConditionExpression.parseExpression(
                content.substring(0, referSymbol)
            ) to content.substring(referSymbol + 1).toInt()
        }
    }

    private fun String?.nullIfEmpty() = if (this?.isEmpty() == true) null else this

}