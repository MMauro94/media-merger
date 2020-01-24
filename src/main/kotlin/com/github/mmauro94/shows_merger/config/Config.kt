package com.github.mmauro94.shows_merger.config

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.shows_merger.util.JAR_LOCATION
import java.io.File
import java.io.IOException


data class Config(
    val defaultLanguages: List<MkvToolnixLanguage>? = null,
    val defaultAdditionalLanguagesToKeep: List<MkvToolnixLanguage>? = null
) {
    companion object {

        private val CONFIG_FILE = File(JAR_LOCATION, "config.json")

        fun parse(): Config? {
            return if (CONFIG_FILE.exists()) {
                try {
                    Klaxon()
                        .converter(MkvToolnixLanguageConverter)
                        .parse<Config>(CONFIG_FILE)
                } catch (ioe: IOException) {
                    throw ConfigParseException("Unable to read config.json", ioe)
                } catch (ke: KlaxonException) {
                    throw ConfigParseException("Unable to parse config.json: ${ke.message}", ke)
                }
            } else null
        }
    }
}