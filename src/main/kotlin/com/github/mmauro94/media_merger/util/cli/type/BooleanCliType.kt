package com.github.mmauro94.media_merger.util.cli.type

object BooleanCliType : CliType<Boolean>() {

    override fun parse(str: String) = when {
        str.lowercase() == "y" -> true
        str.lowercase() == "n" -> false
        else -> null
    }

    override fun toString(item: Boolean) = if (item) "yes" else "no"

    override fun defaultToString(default: Boolean) = if (default) "Y/n" else "y/N"
}