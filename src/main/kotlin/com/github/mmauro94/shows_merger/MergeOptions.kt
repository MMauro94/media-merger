package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.shows_merger.show_info.ShowInfo
import java.time.Duration

object MergeOptions {

    var TV_SHOW: ShowInfo? = null

    val ADDITIONAL_LANGUAGES_TO_KEEP = mutableSetOf<MkvToolnixLanguage>()
    val MAIN_LANGUAGES = mutableSetOf<MkvToolnixLanguage>()
}