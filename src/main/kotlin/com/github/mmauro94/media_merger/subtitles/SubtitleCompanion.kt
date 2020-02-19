package com.github.mmauro94.media_merger.subtitles

import java.io.File

/**
 * Class that each concrete [Subtitle] companion class should implement
 */
interface SubtitleCompanion<T : Subtitle<*>> {

    /**
     * The extension for this subtitle file
     */
    val extension: String

    /**
     * Parses the given [file] into a [Subtitle] instance
     */
    fun parse(file: File): T
}