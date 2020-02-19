package com.github.mmauro94.media_merger.group

interface Group<G> : Comparable<G> {

    fun outputName() : String?

    override fun toString(): String
}