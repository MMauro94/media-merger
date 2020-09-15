package com.github.mmauro94.media_merger.util.ask

object IntCliAsker : AbstractCliAsker<Int>() {
    override fun parse(str: String, default: Int?) = str.toIntOrNull()
}

fun Int.Companion.ask(
    question: String,
    default: Int? = null,
    isValid: Int.() -> Boolean = { true },
    itemToString: Int.() -> String = { IntCliAsker.itemToString(this) },
    defaultToString: Int.() -> String = { IntCliAsker.defaultToString(this) }
): Int {
    return IntCliAsker.ask(question, default, isValid, itemToString, defaultToString)
}