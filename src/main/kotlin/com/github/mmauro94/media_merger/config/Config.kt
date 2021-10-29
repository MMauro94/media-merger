package com.github.mmauro94.media_merger.config

import com.beust.klaxon.JsonObject
import com.beust.klaxon.KlaxonException
import com.github.mmauro94.media_merger.util.ConsoleReporter
import com.github.mmauro94.media_merger.util.JAR_LOCATION
import com.github.mmauro94.media_merger.util.json.KLAXON
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Config data class
 *
 * @param defaultLanguages the default languages
 * @param defaultAdditionalLanguagesToKeep the default additional languages to keep
 * @param languagesDetectWhitelist a list of languages that can be detected from the file/folder names.
 */
data class Config(
    val defaultLanguages: List<MkvToolnixLanguage>? = null,
    val defaultAdditionalLanguagesToKeep: List<MkvToolnixLanguage> = emptyList(),
    val languagesDetectWhitelist: List<MkvToolnixLanguage> = emptyList(),
    val ffmpeg: FFMpegConfig = FFMpegConfig(),
    val apiKeys: Map<String, String> = emptyMap(),
    var infoLanguage: MkvToolnixLanguage? = null,
    val episodeRenameFormat: String = "%(showName) %(seasonNumber00)x%(episodeNumber00) - %(episodeName)",
    val movieRenameFormat: String = "%(name) (%(year))"
) {

    /**
     * Function that, given a service name, detects or asks the user for an API key.
     * It checks first if a config `apiKeys/`[service] (all lower case).
     * It nothing is found checks for an environment variable named [service]_API_KEY (all upper case).
     * If both don't exist, throws [IllegalStateException].
     */
    fun apiKeyForService(service: String): String {
        //If in config, return it
        apiKeys[service.lowercase()]?.let { return it }

        //Otherwise check environment variables
        System.getenv("${service.uppercase()}_API_KEY").let {
            return if (!it.isNullOrBlank()) it else throw IllegalStateException("API key for $service not found! Please specify it in the config.json under apiKeys/${service.lowercase()} or in an environment variable named ${service.uppercase()}_API_KEY")
        }
    }

    companion object {

        private fun JsonObject.mergeIn(prev: JsonObject) {
            for ((key, value) in this) {
                if (key in prev) {
                    val prevValue = prev.getValue(key)
                    if (value is JsonObject && prevValue is JsonObject) {
                        prevValue.mergeIn(value)
                        continue
                    }
                }
                prev[key] = value
            }
        }

        /**
         * The config file that should be placed with the jar
         */
        val SYSTEM_CONFIG_FILE = File(JAR_LOCATION, "config.json")

        /**
         * Parses the `config.json` configuration files.
         * The [configFiles] must be a list of configuration files, starting from the less relevant one (e.g. system-wide), to the most relevant one)
         *
         * @return a [Config] instance
         * @throws ConfigParseException if there is an error reading or parsing the config files
         */
        fun parse(configFiles: List<File>, reporter: ConsoleReporter): Config {
            var config: JsonObject? = null
            for (file in configFiles) {
                if (file.exists()) {
                    reporter.log.debug("Parsing config file ${file.absolutePath}")
                    val json = file.reader().use { r ->
                        KLAXON.parseJsonObject(r)
                    }
                    if (config == null) config = json
                    else {
                        json.mergeIn(config)
                    }
                } else {
                    reporter.log.debug("Searched config file didn't exist: ${file.absolutePath}")
                }
            }

            return if (config == null) {
                reporter.log.debug("No config files found! Using default config")
                Config()
            } else {
                reporter.log.debug("Merged config file: " + config.toJsonString())

                try {
                    KLAXON.parseFromJsonObject<Config>(config) ?: Config()
                } catch (ioe: IOException) {
                    throw ConfigParseException("Unable to read config", ioe)
                } catch (ke: KlaxonException) {
                    throw ConfigParseException("Unable to parse config: ${ke.message}", ke)
                }
            }
        }
    }
}