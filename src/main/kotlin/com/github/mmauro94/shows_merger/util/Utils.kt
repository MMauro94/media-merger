package com.github.mmauro94.shows_merger.util

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.shows_merger.Main
import java.io.File

fun MkvToolnixLanguage.Companion.find(language: String): MkvToolnixLanguage? {
    return all[language] ?: all.values.singleOrNull { it.iso639_1 == language }
}

val JAR_LOCATION : File = File(Main::class.java.protectionDomain.codeSource.location.toURI()).parentFile