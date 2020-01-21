package com.github.mmauro94.shows_merger.subtitles

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.cuts.Cut
import com.github.mmauro94.shows_merger.cuts.Cuts
import java.io.File

data class Subtitle<TEXT>(val lines: List<SubtitleLine<TEXT>>) {

    operator fun times(stretchFactor: StretchFactor): Subtitle<TEXT> {
        return Subtitle(lines.map { it.times(stretchFactor) })
    }

    private fun getLinesForCut(cut: Cut): List<SubtitleLine<TEXT>> {
        return lines
            .filter { it.time.intersects(cut.time) }
            .map {
                it.copy(
                    time = it.time.restrictIn(cut.time)
                )
            }
    }

    fun applyCuts(cuts: Cuts): Subtitle<TEXT> {
        return Subtitle(cuts.cuts.flatMap {
            getLinesForCut(it)
        })
    }

    companion object {
        fun getFactory(extension: String): ((File) -> Subtitle<*>)? {
            return when (extension) {
                "srt" -> { f -> Subtitle.parseSrt(f) }
                else -> null
            }
        }
    }
}