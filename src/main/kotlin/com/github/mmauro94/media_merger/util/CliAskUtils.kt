package com.github.mmauro94.media_merger.util

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import java.math.BigDecimal
import java.time.Duration

inline fun <T : Any> ask(
    question: String,
    parser: (String) -> T?,
    default: T? = null,
    isValid: T.() -> Boolean = { true },
    noinline itemToString: T.() -> String = { toString() },
    defaultToString: T.() -> String = { itemToString(this) }
): T {
    if (default != null) {
        require(default.isValid())
    }
    while (true) {
        print(ansi().fgDefault().a(question))
        if (default != null) {
            print(ansi().fgBrightBlue().a(" [${default.defaultToString()}]"))
        }
        print(ansi().fgDefault().a(" ").saveCursorPosition())
        val line = CLI_SCANNER.nextLine().trim()
        val ret = if (line.isEmpty() && default != null) {
            default
        } else {
            val t = parser(line)
            if (t != null && t.isValid()) {
                t
            } else null
        }

        if (AnsiConsole.out() == System.out) {
            print(ansi().restoreCursorPosition().eraseLine(Ansi.Erase.FORWARD))
            if (ret != null) {
                println(ansi().fgGreen().a(ret.itemToString()).reset())
                return ret
            } else {
                println(ansi().fgRed().a(line).reset())
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
                    return@ask null
                } else {
                    things.add(thing)
                }
            }
            if (things.isNotEmpty()) things else null
        },
        default = default,
        isValid = isValid,
        itemToString = { joinToString(separator = separator.first, transform = itemToString) }
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
        itemToString = { if (this) "yes" else "no" },
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
    default = if (default.isEmpty()) null else default
)

fun askInt(
    question: String,
    default: Int? = null,
    isValid: Int.() -> Boolean = { true },
    itemToString: Int.() -> String = { toString() },
    defaultToString: Int.() -> String = { itemToString(this) }
): Int = ask(
    question = question,
    parser = { it.toIntOrNull() },
    isValid = isValid,
    default = default,
    itemToString = itemToString,
    defaultToString = defaultToString
)


fun askLong(
    question: String,
    default: Long? = null,
    isValid: Long.() -> Boolean = { true },
    itemToString: Long.() -> String = { toString() },
    defaultToString: Long.() -> String = { itemToString(this) }
): Long = ask(
    question = question,
    parser = { it.toLongOrNull() },
    isValid = isValid,
    default = default,
    itemToString = itemToString,
    defaultToString = defaultToString
)

fun askDouble(
    question: String,
    default: Double? = null,
    isValid: Double.() -> Boolean = { true },
    itemToString: Double.() -> String = { toString() },
    defaultToString: Double.() -> String = { itemToString(this) }
): Double = ask(
    question = question,
    parser = { it.toDoubleOrNull() },
    isValid = isValid,
    default = default,
    itemToString = itemToString,
    defaultToString = defaultToString
)

fun askBigDecimal(
    question: String,
    default: BigDecimal? = null,
    isValid: BigDecimal.() -> Boolean = { true },
    itemToString: BigDecimal.() -> String = { toPlainString() },
    defaultToString: BigDecimal.() -> String = { itemToString(this) }
): BigDecimal = ask(
    question = question,
    parser = { it.toBigDecimalOrNull() },
    isValid = isValid,
    default = default,
    itemToString = itemToString,
    defaultToString = defaultToString
)

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

fun askDuration(
    question: String,
    default: Duration,
    isValid: Duration.() -> Boolean = { !isNegative }
): Duration {
    return ask(
        question = question,
        parser = { it.parseTimeStringOrNull() ?: it.toLongOrNull()?.let { s -> Duration.ofSeconds(s) } },
        default = default,
        isValid = isValid,
        itemToString = { toTimeString() }
    )
}
