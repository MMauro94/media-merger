package com.github.mmauro94.media_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.media_merger.show.info.ShowInfo

object MergeOptions {

    var TV_SHOW: ShowInfo? = null

    val ADDITIONAL_LANGUAGES_TO_KEEP = mutableSetOf<MkvToolnixLanguage>()
    val MAIN_LANGUAGES = mutableSetOf<MkvToolnixLanguage>()
}