package com.github.mmauro94.media_merger.util.ask

object StringCliAsker : AbstractCliAsker<String>() {
    override fun parse(str: String, default: String?) = str
}

fun String.Companion.ask(
    question: String,
    default: String = "",
    isValid: String.() -> Boolean = { true },
    itemToString: String.() -> String = { StringCliAsker.itemToString(this) },
    defaultToString: String.() -> String = { StringCliAsker.defaultToString(this) }
): String {
    return StringCliAsker.ask(question, default, isValid, itemToString, defaultToString)
}