package com.github.mmauro94.media_merger

interface Group<G> : Comparable<G> {

    fun outputName() : String?
}