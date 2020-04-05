package com.github.mmauro94.media_merger.group

import com.github.mmauro94.media_merger.Main
import java.io.File

interface Group<G> : Comparable<G> {

    fun outputName(): String?

    override fun toString(): String

    fun outputNameOrFallback() : String = outputName() ?: toString()

    val debugFile: File get() = File(Main.outputDir, "${outputNameOrFallback()}.debug.txt")
}