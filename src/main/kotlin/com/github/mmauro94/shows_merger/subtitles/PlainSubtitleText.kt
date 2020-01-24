package com.github.mmauro94.shows_merger.subtitles

import com.github.mmauro94.shows_merger.util.DurationSpan

/**
 * A simple implementation of [SubtitleText] using a plain [String] for the text.
 */
open class PlainSubtitleText(time: DurationSpan, text: String) : SubtitleText<String>(time, text)