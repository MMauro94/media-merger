package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import java.time.Duration

object MergeOptions {

    val OTHER_LANGUAGES_TO_KEEP = mutableSetOf<MkvToolnixLanguage>()
    val MAIN_LANGUAGES = mutableSetOf(
        MkvToolnixLanguage.all.getValue("eng"),
        MkvToolnixLanguage.all.getValue("ita")
    )
    val MAX_DURATION_ERROR = Duration.ofSeconds(3)!!


    fun isDurationValid(duration: Duration, targetDuration : Duration) : Boolean {
        return duration > targetDuration.minus(MAX_DURATION_ERROR) && duration < targetDuration.plus(MAX_DURATION_ERROR)
    }
}