package com.github.mmauro94.shows_merger.config

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.shows_merger.util.JAR_LOCATION
import java.io.File
import java.io.IOException

/**
 * Config data class
 *
 * @param defaultLanguages the default languages
 * @param defaultAdditionalLanguagesToKeep the default additional languages to keep
 */
data class Config(
    val defaultLanguages: List<MkvToolnixLanguage>? = null,
    val defaultAdditionalLanguagesToKeep: List<MkvToolnixLanguage>? = null,
    val ffmpegHardwareAcceleration : String? = "auto"
) {
    companion object {

        /**
         * The config file
         */
        private val CONFIG_FILE = File(JAR_LOCATION, "config.json")

        /**
         * Parses the `config.json` configuration file.
         * The file should be present in [JAR_LOCATION]
         *
         * @return a [Config] instance or `null` if the config doesn't exist
         * @throws ConfigParseException if there is an error reading or parsing the config file
         */
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