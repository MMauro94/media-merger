package com.github.mmauro94.media_merger.util.ask

import com.github.mmauro94.media_merger.util.CLI_SCANNER
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

abstract class AbstractCliAsker<T : Any> {
    abstract fun parse(str: String, default: T?): T?
    open fun itemToString(item: T): String = item.toString()
    open fun defaultToString(default: T): String = itemToString(default)

    inline fun ask(
        question: String,
        default: T?,
        isValid: T.() -> Boolean = { true },
        itemToString: T.() -> String = { this@AbstractCliAsker.itemToString(this) },
        defaultToString: T.() -> String = { this@AbstractCliAsker.defaultToString(this) }
    ): T {
        if (default != null) {
            require(default.isValid())
        }
        while (true) {
            print(Ansi.ansi().fgDefault().a(question))
            if (default != null) {
                print(Ansi.ansi().fgBrightBlue().a(" [${default.defaultToString()}]"))
            }
            print(Ansi.ansi().fgDefault().a(" ").saveCursorPosition())
            val line = CLI_SCANNER.nextLine().trim()
            val ret = if (line.isEmpty() && default != null) {
                default
            } else {
                val t = parse(line, default)
                if (t != null && t.isValid()) {
                    t
                } else null
            }

            if (AnsiConsole.out() == System.out) {
                print(Ansi.ansi().restoreCursorPosition().eraseLine(Ansi.Erase.FORWARD))
                if (ret != null) {
                    println(Ansi.ansi().fgGreen().a(ret.itemToString()).reset())
                    return ret
                } else {
                    println(Ansi.ansi().fgRed().a(line).reset())
                }
            }
        }
    }

    fun <C : MutableCollection<T>> toMultiple(
        collectionCreator: () -> C,
        separator: Pair<String, Regex> = ", " to Regex(",\\s*")
    ): MultipleCliAsker<T, C> {
        return MultipleCliAsker(this, collectionCreator, separator)
    }

}