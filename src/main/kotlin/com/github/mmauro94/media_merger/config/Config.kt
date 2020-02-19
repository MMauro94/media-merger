package com.github.mmauro94.media_merger.config

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.github.mmauro94.media_merger.askString
import com.github.mmauro94.media_merger.util.JAR_LOCATION
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
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
    val defaultAdditionalLanguagesToKeep: List<MkvToolnixLanguage> = emptyList(),
    val ffmpegHardwareAcceleration: String? = "auto",
    val apiKeys: Map<String, String> = emptyMap()
) {

    /**
     * Function that, given a service name, detects or asks the user for an API key.
     * It checks first if a config `apiKeys/`[service] (all lower case).
     * It nothing is found checks for an environment variable named [service]_API_KEY (all upper case).
     * If both don't exist, throws [IllegalStateException].
     */
    fun apiKeyForService(service: String): String {
        //If in config, return it
        apiKeys[service.toLowerCase()]?.let { return it }

        //Otherwise check environment variables
        System.getenv("${service.toUpperCase()}_API_KEY").let {
            return if (!it.isNullOrBlank()) it else throw IllegalStateException("API key for $service not found! Please specify it in the config.json under apiKeys/${service.toLowerCase()} or in an environment variable named ${service.toUpperCase()}_API_KEY")
        }
    }

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
        fun parse(): Config {
            return if (CONFIG_FILE.exists()) {
                try {
                    Klaxon()
                        .converter(MkvToolnixLanguageConverter)
                        .parse<Config>(CONFIG_FILE) ?: Config()
                } catch (ioe: IOException) {
                    throw ConfigParseException("Unable to read config.json", ioe)
                } catch (ke: KlaxonException) {
                    throw ConfigParseException("Unable to parse config.json: ${ke.message}", ke)
                }
            } else Config()
        }
    }
}