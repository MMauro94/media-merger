package com.github.mmauro94.media_merger

interface Grouper<G : Group<G>> {

    fun detectGroup(filename: String) : G?
}