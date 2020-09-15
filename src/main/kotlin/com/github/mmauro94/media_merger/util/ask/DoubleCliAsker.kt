package com.github.mmauro94.media_merger.util.ask

object DoubleCliAsker : AbstractCliAsker<Double>() {
    override fun parse(str: String, default: Double?) = str.toDoubleOrNull()
}

fun Double.Companion.ask(
    question: String,
    default: Double? = null,
    isValid: Double.() -> Boolean = { true },
    itemToString: Double.() -> String = { DoubleCliAsker.itemToString(this) },
    defaultToString: Double.() -> String = { DoubleCliAsker.defaultToString(this) }
): Double {
    return DoubleCliAsker.ask(question, default, isValid, itemToString, defaultToString)
}