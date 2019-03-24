package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage

fun askLanguage(defaultLanguage: MkvToolnixLanguage? = null): MkvToolnixLanguage {
    var lang: MkvToolnixLanguage? = null
    while (lang == null) {
        if (defaultLanguage != null) {
            print("Please give language [${defaultLanguage.iso639_2}]: ")
        } else {
            print("Please give language: ")
        }
        val l = scanner.nextLine().trim()
        if (!l.isEmpty()) {
            lang = MkvToolnixLanguage.all[l]
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

fun askLanguages(): LinkedHashSet<MkvToolnixLanguage> {
    var langs: LinkedHashSet<MkvToolnixLanguage>? = null
    while (langs == null) {
        print("Please give languages: ")
        val l = scanner.nextLine().trim()
        if (!l.isEmpty()) {
            val temp = l.split(Regex(",\\s*"))
                .asSequence()
                .map {
                    val r = MkvToolnixLanguage.all[it]
                    if (r == null) {
                        System.err.println("Invalid language $it")
                    }
                    r
                }
            if (!temp.contains(null)) {
                langs = temp.filterNotNull().toCollection(LinkedHashSet())
            }
        } else {
            langs = linkedSetOf()
        }
    }
    return langs
}

fun menu(
    map: LinkedHashMap<String, () -> Unit>,
    premenu: () -> Unit = {},
    exitAfterSelection: (Int) -> Boolean = { true }
) {
    var selection = -1
    while (selection != 0) {
        premenu()
        println()
        map.keys.forEachIndexed { i, str ->
            println("${i + 1}) $str")
        }
        println("0) Exit")
        print("Selection: ")
        selection = scanner.nextLine().toIntOrNull() ?: continue
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
        val v = scanner.nextLine().trim()
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
        print(question)
        if (default != null) {
            print(" [$default] ")
        }
        val v = scanner.nextLine().trim()
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