package com.github.mmauro94.media_merger.util

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.Track
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.mkvtoolnix_wrapper.merge.MkvMergeCommand
import java.io.File
import java.io.FileOutputStream
import java.util.*

val OUT = System.out

/**
 * Finds an [MkvToolnixLanguage] that has [language] as its ISO63-2 code.
 * If it cannot be found, it falls back to the ISO639-1 code.
 * If it cannot be found, it falls back to the name.
 * Returns `null` if no language is found.
 */
fun MkvToolnixLanguage.Companion.find(language: String): MkvToolnixLanguage? {
    return all[language]
        ?: all.values.singleOrNull { it.iso639_1 == language }
        ?: all.values.singleOrNull { it.name.equals(language, ignoreCase = true) }
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


/**
 * Replaces invalid chars for filenames with close unicode matches
 */
fun String.filesystemCharReplace(): String {
    return replace(':', '꞉')
        .replace('/', '／')
        .replace('\\', '＼')
        .replace('?', '？')
        .replace(Regex("\"([^\"]+)\""), "‟$1”")
        .replace('\"', '＂')
        .replace('*', '∗')
        .replace('<', '❮')
        .replace('>', '❯')
}

fun <T> File.findWalkingUp(allowWorkingDir: Boolean, finder: (File) -> T?): T? {
    return if (absoluteFile == Main.workingDir) {
        //If current if working dir we must stop recursion
        //We either return null or allow one last finder call, base on allowWorkingDir
        if (allowWorkingDir) finder(this)
        else null
    } else {
        //If the current directory is not the working dir, we can keep finding and bubbling up
        require(absolutePath.contains(Main.workingDir.absolutePath))

        val found = finder(this)
        val parent = absoluteFile.parentFile?.absoluteFile
        if (found == null && parent != null) {
            //Keep recursing up if not found anything and parent exists
            parent.findWalkingUp(allowWorkingDir, finder)
        } else found
    }
}

fun File.appendWriter(): FileOutputStream {
    return FileOutputStream(this, true)
}

fun newTmpFile(): File {
    return File(System.getProperty("java.io.tmpdir")!!, UUID.randomUUID().toString())
}