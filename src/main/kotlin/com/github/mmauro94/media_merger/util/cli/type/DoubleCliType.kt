package com.github.mmauro94.media_merger.util.cli.type

object DoubleCliType : CliType<Double>() {
    override fun parse(str: String) = str.toDoubleOrNull()
}
