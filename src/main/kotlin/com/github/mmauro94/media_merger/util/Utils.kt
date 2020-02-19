package com.github.mmauro94.media_merger.util

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.mkvtoolnix_wrapper.merge.MkvMergeCommand
import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.Track
import java.io.File
import java.util.*

/**
 * Finds an [MkvToolnixLanguage] that has [language] as its ISO63-2 code.
 * If it cannot be found, it falls back to the ISO639-1 code.
 * If it cannot be found, it falls back to the name.
 * Returns `null` if no language is found.
 */
fun MkvToolnixLanguage.Companion.find(language: String): MkvToolnixLanguage? {
    return all[language]
        ?: all.values.singleOrNull { it.iso639_1 == language }
        ?: all.values.singleOrNull { it.name.toLowerCase() == language.toLowerCase() }
}

/**
 * The location of the jar file
 */
val JAR_LOCATION: File = File(Main::class.java.protectionDomain.codeSource.location.toURI()).parentFile

/**
 * Adds the given value [V] to the list corresponding at item [K], creating one if not present.
 */
fun <K, V> MutableMap<K, MutableList<V>>.add(key: K, value: V) {
    if (!containsKey(key)) {
        put(key, ArrayList())
    }
    getValue(key).add(value)
}

/**
 * Adds all the given values [V] to the lists corresponding at the given items [K], creating them if not present.
 */
fun <K, V> MutableMap<K, MutableList<V>>.addAll(another: Map<K, List<V>>) {
    another.forEach { (k, v) ->
        if (!containsKey(k)) {
            put(k, ArrayList(v))
        } else {
            getValue(k).addAll(v)
        }
    }
}

/**
 * Adds a [Track] to a [MkvMergeCommand], also setting the [MkvMergeCommand.InputFile.TrackOptions.language].
 */
fun MkvMergeCommand.addTrack(track: Track, f: MkvMergeCommand.InputFile.TrackOptions.() -> Unit = {}) =
    this.addTrack(track.mkvTrack) {
        language = track.language
        f(this)
    }

/**
 * Sorts the given sequence by the given [sorters]. Puts true values first.
 */
fun <T> Sequence<T>.sortWithPreferences(vararg sorters: (T) -> Boolean) =
    this.sortedWith(compareBy(*sorters).reversed())
