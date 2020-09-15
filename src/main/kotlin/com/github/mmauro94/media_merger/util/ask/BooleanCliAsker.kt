package com.github.mmauro94.media_merger.util.ask

object BooleanCliAsker : AbstractCliAsker<Boolean>() {

    override fun parse(str: String, default: Boolean?) = when {
        str.isEmpty() -> default
        str.toLowerCase() == "y" -> true
        str.toLowerCase() == "n" -> false
        else -> null
    }

    override fun itemToString(item: Boolean) = if (item) "yes" else "no"

    override fun defaultToString(default: Boolean) = if (default) "Y/n" else "y/N"
}

fun Boolean.Companion.ask(
    question: String,
    default: Boolean? = null,
    isValid: Boolean.() -> Boolean = { true },
    itemToString: Boolean.() -> String = { BooleanCliAsker.itemToString(this) },
    defaultToString: Boolean.() -> String = { BooleanCliAsker.defaultToString(this) }
): Boolean {
    return BooleanCliAsker.ask(question, default, isValid, itemToString, defaultToString)
}