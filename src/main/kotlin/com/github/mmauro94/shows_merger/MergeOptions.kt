package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.shows_merger.show_info.ShowInfo
import java.time.Duration

object MergeOptions {

    var TV_SHOW: ShowInfo? = null

    val OTHER_LANGUAGES_TO_KEEP = mutableSetOf<MkvToolnixLanguage>()
    val MAIN_LANGUAGES = mutableSetOf<MkvToolnixLanguage>()
    var MAX_DURATION_ERROR = Duration.ofSeconds(2)!!

    fun isDurationValid(duration: Duration, targetDuration: Duration): Boolean {
        return duration > targetDuration.minus(MAX_DURATION_ERROR) && duration < targetDuration.plus(MAX_DURATION_ERROR)
    }
}