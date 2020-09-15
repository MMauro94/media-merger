package com.github.mmauro94.media_merger.util.ask

object LongCliAsker : AbstractCliAsker<Long>() {
    override fun parse(str: String, default: Long?) = str.toLongOrNull()
}

fun Long.Companion.ask(
    question: String,
    default: Long? = null,
    isValid: Long.() -> Boolean = { true },
    itemToString: Long.() -> String = { LongCliAsker.itemToString(this) },
    defaultToString: Long.() -> String = { LongCliAsker.defaultToString(this) }
): Long {
    return LongCliAsker.ask(question, default, isValid, itemToString, defaultToString)
}