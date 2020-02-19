package com.github.mmauro94.media_merger.subtitles

import com.github.mmauro94.media_merger.StretchFactor
import com.github.mmauro94.media_merger.cuts.Cut
import com.github.mmauro94.media_merger.cuts.Cuts
import com.github.mmauro94.media_merger.subtitles.srt.SRTSubtitle
import com.github.mmauro94.media_merger.util.DurationSpan
import java.io.File

/**
 * Abstract class representing a whole subtitle
 * @param TEXT the type [SubtitleText] this subtitle uses
 * @param items the items of the subtitle
 */
abstract class Subtitle<TEXT : SubtitleText<*>>(val items: List<TEXT>) {

    /**
     * Function that should create a new instance of [TEXT] with the given time
     */
    protected abstract fun TEXT.changeTime(time: DurationSpan): TEXT

    /**
     * Function that multiplies a [TEXT] by the given [stretchFactor].
     */
    protected operator fun TEXT.times(stretchFactor: StretchFactor): TEXT {
        return changeTime(time * stretchFactor)
    }

    /**
     * Returns a new [Subtitle] instance with the given [items]
     */
    protected abstract fun withLines(items: List<TEXT>): Subtitle<TEXT>

    /**
     * Function that multiplies this [Subtitle] by the given [stretchFactor].
     */
    operator fun times(stretchFactor: StretchFactor): Subtitle<TEXT> {
        return withLines(items.map { it.times(stretchFactor) })
    }

    /**
     * Returns all the lines with the modified times for the given [cut].
     * @see Cut
     */
    private fun getLinesForCut(cut: Cut): List<TEXT> {
        return items
            .filter { it.time.intersects(cut.time) }
            .mapNotNull {
                val newTime = (it.time + cut.offset).restrictIn(cut.targetTime)
                if (newTime != null) it.changeTime(newTime)
                else newTime
            }
    }

    /**
     * Returns a new subtitle with the [cuts] applied.s
     */
    fun withCuts(cuts: Cuts): Subtitle<TEXT> {
        return withLines(cuts.cuts.flatMap {
            getLinesForCut(it)
        })
    }

    /**
     * Saves the subtitle to the specified [outputFile]
     */
    abstract fun save(outputFile: File)

    companion object {
        /**
         * Lists of all supported types of subtitles
         */
        private val TYPES: List<SubtitleCompanion<*>> = listOf(SRTSubtitle)

        /**
         * Returns a function that parses the subtitle with the given [extension] with the appropriate parser.
         * Returns `null` if no appropriate parser is found.
         */
        fun getFactory(extension: String): ((File) -> Subtitle<*>)? {
            val parser = TYPES.firstOrNull { it.extension == extension }
            return if (parser != null) {
                { f -> parser.parse(f) }
            } else null
        }

        /**
         * Parses the given subtitle [file].
         * Returns `null` if no appropriate parser is found.
         */
        fun parse(file: File): Subtitle<*>? {
            return getFactory(file.extension)?.invoke(file)
        }
    }
}