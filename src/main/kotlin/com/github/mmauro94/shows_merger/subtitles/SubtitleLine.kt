package com.github.mmauro94.shows_merger.subtitles

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.util.DurationSpan

data class SubtitleLine<out TEXT>(val time: DurationSpan, val text: TEXT) {

    operator fun times(stretchFactor: StretchFactor) : SubtitleLine<TEXT> {
        return SubtitleLine(time * stretchFactor, text)
    }
}