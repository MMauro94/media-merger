package com.github.mmauro94.media_merger.util.cli.type

import com.github.mmauro94.media_merger.util.cli.CliAsker

abstract class CliType<T : Any> {
    abstract fun parse(str: String): T?
    open fun toString(item: T): String = item.toString()
    open fun defaultToString(default: T): String = toString(default)

    fun asker(
        isValid: T.() -> Boolean = { true },
        itemToString: T.() -> String = { this@CliType.toString(this) },
        defaultToString: T.() -> String = { this@CliType.defaultToString(this) }
    ): CliAsker<T> {
        return CliAsker(this, isValid, itemToString, defaultToString)
    }

    fun ask(
        question: String,
        default: T? = null,
        isValid: T.() -> Boolean = { true },
        itemToString: T.() -> String = { this@CliType.toString(this) },
        defaultToString: T.() -> String = { this@CliType.defaultToString(this) }
    ): T {
        return asker(isValid, itemToString, defaultToString).ask(question, default)
    }

    fun <C : MutableCollection<T>> toCollection(
        collectionCreator: () -> C,
        separator: Pair<String, Regex> = ", " to Regex(",\\s*")
    ): CollectionCliType<T, C> {
        return CollectionCliType(this, collectionCreator, separator)
    }

    fun toLinkedHashSet(
        separator: Pair<String, Regex> = ", " to Regex(",\\s*")
    ): CollectionCliType<T, LinkedHashSet<T>> {
        return toCollection({ LinkedHashSet() }, separator)
    }
}