package com.github.mmauro94.shows_merger.subtitles

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.cuts.Cut
import com.github.mmauro94.shows_merger.cuts.Cuts
import com.github.mmauro94.shows_merger.subtitles.srt.SRTSubtitle
import com.github.mmauro94.shows_merger.util.DurationSpan
import java.io.File

abstract class Subtitle<LINE : SubtitleLine<*>>(val lines: List<LINE>) {

    protected abstract fun LINE.changeTime(time: DurationSpan): LINE
    protected operator fun LINE.times(stretchFactor: StretchFactor): LINE {
        return changeTime(time * stretchFactor)
    }

    protected abstract fun withLines(lines: List<LINE>): Subtitle<LINE>

    operator fun times(stretchFactor: StretchFactor): Subtitle<LINE> {
        return withLines(lines.map { it.times(stretchFactor) })
    }

    private fun getLinesForCut(cut: Cut): List<LINE> {
        return lines
            .filter { it.time.intersects(cut.time) }
            .map {
                it.changeTime(it.time.restrictIn(cut.time))
            }
    }

    fun applyCuts(cuts: Cuts): Subtitle<LINE> {
        return withLines(cuts.cuts.flatMap {
            getLinesForCut(it)
        })
    }

    abstract fun save(outputFile: File)

    companion object {
        private val TYPES: List<SubtitleCompanion<*>> = listOf(SRTSubtitle)

        fun getFactory(extension: String): ((File) -> Subtitle<*>)? {
            val parser = TYPES.firstOrNull { it.extension == extension }
            return if (parser != null) {
                { f -> parser.parse(f) }
            } else null
        }
    }
}