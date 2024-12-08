package com.github.hatoyuze.restarter.game.data

import com.github.hatoyuze.restarter.mirai.ResourceManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int


@Serializable
data class AgeSupportEvents(val age: Int, val events: MutableMap<Int, Double>) {
    @Serializable
    private data class AgesEventMapping0(
        val age: Int,
        val event: List<JsonPrimitive>
    ) {
        fun translate(): MutableMap<Int, Double> {
            return event.associate {
                if (it.isString) {
                    val element = it.content.split('*')
                    element[0].toInt() to (element.getOrNull(1)?.toDouble() ?: 1.0)
                }else {
                    it.int to 1.0
                }
            }.toMutableMap()
        }
    }

    companion object {
        val data by lazy {
            val jsonContent =
                ResourceManager.getResource("data/ages.json") ?: error("Can not find resources: ages")
            val json = Json {
                ignoreUnknownKeys = true
            }
            json.decodeFromString<Map<String,AgesEventMapping0>>(jsonContent)
                .mapKeys { it.key.toInt() }
                .mapValues { AgeSupportEvents(it.key,it.value.translate()) }
                .toMutableMap()
        }
    }
}