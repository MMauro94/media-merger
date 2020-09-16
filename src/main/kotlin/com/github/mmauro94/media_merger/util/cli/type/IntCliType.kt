package com.github.mmauro94.media_merger.util.cli.type

object IntCliType : CliType<Int>() {
    override fun parse(str: String) = str.toIntOrNull()
}