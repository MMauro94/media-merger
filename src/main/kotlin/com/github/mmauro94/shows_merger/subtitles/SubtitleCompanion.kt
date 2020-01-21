package com.github.mmauro94.shows_merger.subtitles

import java.io.File

interface SubtitleCompanion<T : Subtitle<*>> {

    val extension: String

    fun parse(file: File): T
}