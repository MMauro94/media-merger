package com.github.mmauro94.shows_merger.subtitles

import com.github.mmauro94.shows_merger.util.DurationSpan

open class PlainSubtitleLine(time: DurationSpan, text: String) : SubtitleLine<String>(time, text)