package com.github.mmauro94.media_merger.util

import org.fusesource.jansi.Ansi.ansi
import java.util.*

val CLI_SCANNER = Scanner(System.`in`)

fun <T> menu(
    title: String,
    items: Map<String, () -> T>
): T {
    return menu(title, items.toList())
}

fun <T> menu(
    title: String,
    items: List<Pair<String, () -> T>>
): T {
    return select(
        question = ansi().bgBrightGreen().fgBlack().render(title).reset().toString(),
        options = items,
        long = true,
        nameProvider = { it.first }
    ).second()
}




