package com.github.mmauro94.shows_merger.subtitles

import com.github.mmauro94.shows_merger.util.DurationSpan

/**
 * An abstract class representing a subtitle text
 * @param TEXT the type of class representing the text
 * @param time the time at which the text should appear
 * @param text the text of the subtitle
 */
abstract class SubtitleText<TEXT>(val time: DurationSpan, val text: TEXT)