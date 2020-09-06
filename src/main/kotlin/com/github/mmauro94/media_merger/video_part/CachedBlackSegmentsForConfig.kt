package com.github.mmauro94.media_merger.video_part

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.KlaxonException
import com.github.mmauro94.media_merger.util.DurationSpan
import com.github.mmauro94.media_merger.util.json.KLAXON
import com.github.mmauro94.media_merger.util.restrictTo
import java.time.Duration
import java.util.*

data class CachedBlackSegmentsForConfig(private var map: SortedMap<DurationSpan?, List<DurationSpan>?> = sortedMapOf(COMPARATOR)) {

    operator fun get(range: DurationSpan?) = map.getValue(range)

    operator fun set(range: DurationSpan?, blackSegments: List<DurationSpan>) {
        map[range] = blackSegments
    }

    fun simplify() {
        val all = map[null]
        if (all != null) {
            map.keys.removeIf { it != null }
        } else {
            val newMap = sortedMapOf<DurationSpan?, List<DurationSpan>?>(COMPARATOR)

            for ((range, values) in map) {
                if (range != null) {
                    val last = newMap.keys.lastOrNull()
                    if (last != null) {
                        if (range.isContainedIn(last)) continue
                        else if (range.isConsecutiveOf(last) && values != null) {
                            val oldValues = newMap[last]
                            if (oldValues != null) {
                                newMap.remove(last)
                                newMap[DurationSpan(last.start, range.end)] = oldValues + values
                                continue
                            }
                        }
                    }
                    newMap[range] = values
                }
            }
            map = newMap
        }
    }

    fun takeOrComputeForRange(range: DurationSpan?, computer: (start: Duration, end: Duration?) -> List<DurationSpan>?): List<DurationSpan>? {
        val all = map[null]
        return if (all != null) {
            if (range == null) all
            else all.restrictTo(range.start, range.end)
        } else {
            val start = range?.start ?: Duration.ZERO!!
            val end = range?.end
            val ret = mutableListOf<DurationSpan>()


            var ended = false
            fun add(range: DurationSpan) {
                val span = if (map.containsKey(range)) {
                    map[range]
                } else {
                    computer(range.start, range.end).also {
                        map[range] = it
                    }
                }
                if (span != null) {
                    ret.addAll(span)
                } else {
                    ended = true
                }
            }

            val neededRanges = if (range == null) map.keys.filterNotNull()
            else map.keys.mapNotNull { it!!.intersection(range) }

            var lastEnd = start

            for (r in neededRanges) {
                if (lastEnd < r.start) {
                    add(DurationSpan(lastEnd, r.start))
                }
                add(r)
                lastEnd = r.end
            }
            if (!ended) {
                if (end == null) {
                    val computed = computer(lastEnd, null)
                    if (computed != null) {
                        ret.addAll(computed)
                    }
                    map[null] = ret
                } else if (lastEnd < end) {
                    add(DurationSpan(lastEnd, end))
                }
            }

            ret.restrictTo(start, end)
        }
    }

    fun toJsonArray() = JsonArray(map.map { (range, blackSegments) ->
        JsonObject(
            mapOf(
                "range" to range,
                "black_segments" to blackSegments
            )
        )
    })

    companion object {
        // Sorted by start lowest to highest. If equal, sorted by duration from highest to lowest.
        private val COMPARATOR = kotlin.Comparator { o1: DurationSpan?, o2: DurationSpan? ->
            when {
                o1 == null && o2 == null -> 0
                o1 == null -> -1
                o2 == null -> +1
                else -> when (val cmp = o1.start.compareTo(o2.start)) {
                    0 -> -o1.duration.compareTo(o2.duration)
                    else -> cmp
                }
            }
        }

        fun fromJsonArray(arr: JsonArray<*>): CachedBlackSegmentsForConfig {
            return CachedBlackSegmentsForConfig(arr.associate {
                if (it is JsonObject) {
                    val jRange = it.obj("range") ?: throw KlaxonException("Each item must have a range")
                    val range = KLAXON.parseFromJsonObject<DurationSpan>(jRange) ?: throw KlaxonException("Invalid range")

                    val jBlackSegments = it.array<JsonObject>("black_segments") ?: throw KlaxonException("Each item must have a black_segments")
                    val blackSegments = KLAXON.parseFromJsonArray<DurationSpan>(jBlackSegments) ?: throw KlaxonException("Invalid black_segments")

                    range to blackSegments
                } else throw KlaxonException("Each item must be a JsonObject")
            }.toSortedMap(COMPARATOR))
        }
    }
}