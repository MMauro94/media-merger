package com.github.mmauro94.media_merger.util.cli.type

object StringCliType : CliType<String>() {
    override fun parse(str: String) = str
}