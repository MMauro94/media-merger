package com.github.mmauro94.media_merger.video_part

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.KlaxonException
import com.github.mmauro94.media_merger.config.FFMpegBlackdetectConfig
import com.github.mmauro94.media_merger.util.DurationSpan
import com.github.mmauro94.media_merger.util.json.KLAXON

data class CachedBlackSegments(private val map: MutableMap<CachedBlackSegmentsConfig, CachedBlackSegmentsForConfig> = mutableMapOf()) {

    operator fun get(config: CachedBlackSegmentsConfig) = map.getOrPut(config, { CachedBlackSegmentsForConfig() })

    operator fun set(config: CachedBlackSegmentsConfig, range: DurationSpan?, blackSegments: List<DurationSpan>) {
        map.getOrPut(config, { CachedBlackSegmentsForConfig() })[range] = blackSegments
    }

    fun simplify() = apply {
        map.values.forEach { it.simplify() }
    }

    fun toJsonArray() = JsonArray(map.map { (config, blackSegments) ->
        JsonObject(
            mapOf(
                "config" to config,
                "black_segments" to blackSegments.toJsonArray()
            )
        )
    })

    companion object {
        fun fromJsonArray(arr: JsonArray<*>): CachedBlackSegments {
            return CachedBlackSegments(arr.associate {
                if (it is JsonObject) {
                    val jConfig = it.obj("config") ?: throw KlaxonException("Each item must have a config")
                    val config = KLAXON.parseFromJsonObject<CachedBlackSegmentsConfig>(jConfig) ?: throw KlaxonException("Invalid config")

                    val jBlackSegments = it.array<JsonObject>("black_segments") ?: throw KlaxonException("Each item must have a black_segments")
                    val blackSegments = CachedBlackSegmentsForConfig.fromJsonArray(jBlackSegments)

                    config to blackSegments
                } else throw KlaxonException("Each item must be a JsonObject")
            }.toMutableMap())
        }
    }
}

