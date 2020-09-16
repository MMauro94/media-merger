package com.github.mmauro94.media_merger.util.cli.type

import com.github.mmauro94.media_merger.util.find
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage

object MkvToolnixLanguageCliType : CliType<MkvToolnixLanguage>() {
    override fun parse(str: String) = MkvToolnixLanguage.find(str)
    override fun toString(item: MkvToolnixLanguage) = item.iso639_2
}