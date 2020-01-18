package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.merge.MkvMergeCommand
import java.math.BigDecimal
import java.time.Duration
import java.util.*
import kotlin.math.roundToLong

val scanner = Scanner(System.`in`)

val VIDEO_EXTENSIONS = listOf("avi", "mp4", "mkv", "mov", "ogv", "mpg", "mpeg", "m4v")
val AUDIO_EXTENSIONS = listOf("mp3", "ac3", "aac", "flac", "m4a", "oga")
val SUBTITLES_EXTENSIONS = listOf("srt", "ssa", "idx", "sub")
val EXTENSIONS_TO_IDENTIFY = VIDEO_EXTENSIONS + AUDIO_EXTENSIONS + SUBTITLES_EXTENSIONS

fun <K, V> MutableMap<K, MutableList<V>>.add(key: K, value: V) {
    if (!containsKey(key)) {
        put(key, ArrayList())
    }
    getValue(key).add(value)
}

fun <K, V> MutableMap<K, MutableList<V>>.addAll(another: Map<K, List<V>>) {
    another.forEach { (k, v) ->
        if (!containsKey(k)) {
            put(k, ArrayList(v))
        } else {
            getValue(k).addAll(v)
        }
    }
}

fun MkvMergeCommand.addTrack(track: Track, f: MkvMergeCommand.InputFile.TrackOptions.() -> Unit = {}) =
    this.addTrack(track.mkvTrack) {
        language = track.language
        f(this)
    }

fun <T> Sequence<T>.sortWithPreferences(vararg sorters: (T) -> Boolean) =
    this.sortedWith(compareBy(*sorters).reversed())

fun Duration?.humanStr() =
    if (this == null) "Unknown" else "${this.toHours()}h${this.toMinutesPart()}m${this.toSecondsPart()}s${this.toMillisPart()}ms"

fun Double.asSecondsDuration() =
    if (this == 0.0) null else Duration.ofSeconds(toLong(), ((this % 1) * 1000000000).toLong())!!

fun BigDecimal.asSecondsDuration() = Duration.ofNanos(this.setScale(9).unscaledValue().longValueExact())!!

fun Duration.toTotalSeconds(): String = BigDecimal.valueOf(toNanos(), 9).toPlainString()

fun Duration.makeMillisPrecision() = Duration.ofMillis((toNanos() / 1_000_000.0).roundToLong())!!

fun Duration.requireMillisPrecision() = require(nano % 1_000_000 == 0)

fun Iterable<Duration>.sum() : Duration = fold(Duration.ZERO) { acc, it -> acc + it }