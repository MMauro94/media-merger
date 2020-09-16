package com.github.mmauro94.media_merger.util.cli

import com.github.mmauro94.media_merger.util.CLI_SCANNER
import com.github.mmauro94.media_merger.util.cli.type.CliType
import com.github.mmauro94.media_merger.util.cli.type.StringCliType
import com.github.mmauro94.media_merger.util.cli.type.or
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

class CliAsker<T : Any>(
    val cliType: CliType<T>,
    val isValid: T.() -> Boolean = { true },
    val itemToString: T.() -> String = { cliType.toString(this) },
    val defaultToString: T.() -> String = { cliType.defaultToString(this) }
) {

    fun ask(question: String, default: T?): T {
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
                val t = if (line.isBlank()) default else cliType.parse(line)
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
}

inline fun <reified A : T, reified B : T, reified T : Any> CliAsker<A>.or(other: CliAsker<B>): CliAsker<T> {
    return CliAsker(
        cliType = cliType.or(other.cliType),
        isValid = {
            when (this) {
                is A -> this@or.isValid(this)
                is B -> other.isValid(this)
                else -> error("Item must be either instance of " + A::class.simpleName + " or " + B::class.simpleName + ", " + this::class.simpleName + " found")
            }
        },
        itemToString = {
            when (this) {
                is A -> this@or.itemToString(this)
                is B -> other.itemToString(this)
                else -> error("Item must be either instance of " + A::class.simpleName + " or " + B::class.simpleName + ", " + this::class.simpleName + " found")
            }
        },
        defaultToString = {
            when (this) {
                is A -> this@or.defaultToString(this)
                is B -> other.defaultToString(this)
                else -> error("Item must be either instance of " + A::class.simpleName + " or " + B::class.simpleName + ", " + this::class.simpleName + " found")
            }
        },
    )
}

inline fun <reified T : Any> CliAsker<T>.askOrNullifyIf(nullifyString: String, question: String, default: T? = null): T? {
    val ret = or(StringCliType.asker(isValid = { this == nullifyString })).ask(question, default)
    return if(ret is T) ret
    else null
}