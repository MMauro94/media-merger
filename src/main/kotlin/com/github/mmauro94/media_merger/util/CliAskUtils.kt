package com.github.mmauro94.media_merger.util

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage

inline fun <T : Any> ask(
    question: String,
    parser: (String) -> T?,
    default: T? = null,
    isValid: T.() -> Boolean = { true },
    defaultToString: T.() -> String = { toString() }
): T {
    if (default != null) {
        require(default.isValid())
    }
    while (true) {
        print("$question ")
        if (default != null) {
            print("[${default.defaultToString()}] ")
        }
        val line = CLI_SCANNER.nextLine().trim()
        if (line.isEmpty() && default != null) {
            return default
        } else {
            val t = parser(line)
            if (t != null && t.isValid()) {
                return t
            }
        }
    }
}


fun <T, C : MutableCollection<T>> askMultiple(
    question: String,
    parser: (String) -> T?,
    collectionCreator: () -> C,
    default: C? = null,
    isValid: C.() -> Boolean = { true },
    itemToString: T.() -> String = { toString() },
    separator: Pair<String, Regex> = ", " to Regex(",\\s*")
): C {
    return ask(
        question = question,
        parser = {
            val things: C = collectionCreator()
            it.split(separator.second).forEach { item ->
                val thing = parser(item)
                if (thing == null) {
                    System.err.println("Invalid value \"$item\"")
                    return@ask null
                } else {
                    things.add(thing)
                }
            }
            if (things.isNotEmpty()) things else null
        },
        default = default,
        isValid = isValid,
        defaultToString = { joinToString(separator = separator.first, transform = itemToString) }
    )
}

fun askYesNo(question: String, default: Boolean? = null): Boolean {
    return ask(
        question = question,
        default = default,
        parser = {
            when {
                it.isEmpty() -> default
                it.toLowerCase() == "y" -> true
                it.toLowerCase() == "n" -> false
                else -> null
            }
        },
        defaultToString = {
            if (this) "Y/n"
            else "y/N"
        }
    )
}

fun askString(
    question: String,
    default: String = "",
    isValid: String.() -> Boolean = { true }
): String = ask(
    question = question,
    parser = { it },
    isValid = isValid,
    default = if(default.isEmpty()) null else default
)

fun askInt(
    question: String,
    default: Int? = null,
    isValid: Int.() -> Boolean = { true }
): Int = ask(question = question, parser = { it.toIntOrNull() }, isValid = isValid, default = default)

fun askDouble(
    question: String,
    default: Double? = null,
    isValid: Double.() -> Boolean = { true }
): Double = ask(question = question, parser = { it.toDoubleOrNull() }, isValid = isValid, default = default)

fun askLanguages(
    question: String,
    defaultValue: LinkedHashSet<MkvToolnixLanguage>? = null,
    isValid: LinkedHashSet<MkvToolnixLanguage>.() -> Boolean = { true }
): LinkedHashSet<MkvToolnixLanguage> {
    return askMultiple(
        question = question,
        parser = { MkvToolnixLanguage.find(it) },
        collectionCreator = { LinkedHashSet() },
        default = defaultValue,
        isValid = isValid,
        itemToString = { iso639_2 }
    )
}

