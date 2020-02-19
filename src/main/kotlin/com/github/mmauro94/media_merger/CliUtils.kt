package com.github.mmauro94.media_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.media_merger.util.find
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

val CLI_SCANNER = Scanner(System.`in`)

fun askLanguage(defaultLanguage: MkvToolnixLanguage? = null): MkvToolnixLanguage {
    var lang: MkvToolnixLanguage? = null
    while (lang == null) {
        if (defaultLanguage != null) {
            print("Please give language [${defaultLanguage.iso639_2}]: ")
        } else {
            print("Please give language: ")
        }
        val l = CLI_SCANNER.nextLine().trim()
        if (l.isNotEmpty()) {
            lang = MkvToolnixLanguage.find(l)
            if (lang == null) {
                System.err.println("Invalid language code!")
            }
        } else {
            if (defaultLanguage == null) {
                System.err.println("No language given")
            } else {
                lang = defaultLanguage
            }
        }
    }
    return lang
}

fun <T, C : MutableCollection<T>> askThings(
    question: String,
    parser: (String) -> T?,
    collectionCreator: () -> C,
    defaultValue: C? = null,
    thingToString: (T) -> String = { it.toString() },
    separator: Pair<String, Regex> = ", " to Regex(",\\s*")
): C {
    while (true) {
        print("$question ")
        if (!defaultValue.isNullOrEmpty()) {
            print("[" + defaultValue.joinToString(separator = separator.first, transform = thingToString) + "] ")
        }
        val l: String = CLI_SCANNER.nextLine()
        if (l.isEmpty()) {
            if (defaultValue !== null) {
                return defaultValue
            } else {
                System.err.println("No default value!")
            }
        } else {
            val things: C = collectionCreator()
            var canRet = true
            l.split(Regex(",\\s*")).forEach {
                val thing = parser(it)
                if (thing == null) {
                    System.err.println("Invalid value \"$it\"")
                    canRet = false
                } else {
                    things.add(thing)
                }
            }
            if (canRet && things.isNotEmpty()) {
                return things
            }
        }
    }
}

fun askLanguages(
    question: String = "Select languages",
    defaultValue: LinkedHashSet<MkvToolnixLanguage>? = null
): LinkedHashSet<MkvToolnixLanguage> {
    return askThings(
        question = question,
        parser = { MkvToolnixLanguage.find(it) },
        collectionCreator = { LinkedHashSet() },
        defaultValue = defaultValue,
        thingToString = { it.iso639_2 }
    )
}

fun menu(
    map: LinkedHashMap<String, () -> Unit>,
    premenu: () -> Unit = {},
    exitAfterSelection: (Int) -> Boolean = { true }
) = menu({ map }, premenu, exitAfterSelection)

fun menu(
    mapF: () -> LinkedHashMap<String, () -> Unit>,
    premenu: () -> Unit = {},
    exitAfterSelection: (Int) -> Boolean = { true }
) {
    var selection = -1
    while (selection != 0) {
        premenu()
        val map = mapF()
        map.keys.forEachIndexed { i, str ->
            println("${i + 1}) $str")
        }
        println("0) Exit")
        print("Selection: ")
        selection = CLI_SCANNER.nextLine().toIntOrNull() ?: continue
        println()
        if (selection != 0) {
            val sel = map.asSequence().drop(selection - 1).firstOrNull()
            if (sel == null) {
                System.err.println("Invalid selection!")
            } else {
                sel.value()
                if (exitAfterSelection(selection)) {
                    return
                }
            }
        }
    }
}

fun askYesNo(question: String, default: Boolean): Boolean {
    var ret: Boolean? = null
    while (ret == null) {
        print(question)
        if (default) print(" [Y/n] ")
        else print(" [y/N] ")
        val v = CLI_SCANNER.nextLine().trim()
        when {
            v.isEmpty() -> ret = default
            v.toLowerCase() == "y" -> ret = true
            v.toLowerCase() == "n" -> ret = false
        }
    }
    return ret
}

fun askInt(question: String, min: Int? = null, max: Int? = null, default: Int? = null): Int {
    var ret: Int? = null
    while (ret == null) {
        print("$question ")
        if (default != null) {
            print("[$default] ")
        }
        val v = CLI_SCANNER.nextLine().trim()
        if (v.isEmpty() && default != null) {
            ret = default
        } else {
            val i = v.toIntOrNull()
            if (i != null && (min == null || i >= min) && (max == null || i <= max)) {
                ret = i
            }
        }
    }
    return ret
}


fun askString(question: String, defaultValue: String = ""): String {
    print("$question ")
    if (defaultValue.isNotBlank()) {
        print("[$defaultValue] ")
    }
    val l = CLI_SCANNER.nextLine().trim()
    return if (l.isBlank()) defaultValue else l
}

fun askEnum(question: String, enums: List<String>, defaultValue: String = ""): String {
    val selection = askString(question + " (" + enums.joinToString(", ") + ")", defaultValue)
    return if (selection in enums) {
        selection
    } else {
        System.err.println("Invalid value! Must be one of " + enums.map { "'$it'" }.joinToString { ", " })
        askEnum(question, enums, defaultValue)
    }
}