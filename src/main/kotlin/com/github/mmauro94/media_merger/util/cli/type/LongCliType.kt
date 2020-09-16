package com.github.mmauro94.media_merger.util.cli.type

object LongCliType : CliType<Long>() {
    override fun parse(str: String) = str.toLongOrNull()
}

