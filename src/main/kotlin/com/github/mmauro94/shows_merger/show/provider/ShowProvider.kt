package com.github.mmauro94.shows_merger.show.provider

import com.github.mmauro94.shows_merger.askString
import com.github.mmauro94.shows_merger.show.info.ShowInfo
import com.github.mmauro94.shows_merger.show.info.ShowInfoException
import com.github.mmauro94.shows_merger.util.JAR_LOCATION
import java.io.File

/**
 * An interface that allows to download [ShowInfo] of a particular type
 * @param S the type of [ShowInfo] this provider downloads
 */
interface ShowProvider<S : ShowInfo> {

    /**
     * Searches for a show with the given [query] and returns a list of results.
     * @throws ShowInfoException if the search gives an error
     */
    fun searchShow(query: String): List<S>

    companion object {

        /**
         * Function that, given a service name, detects or asks the user for an API key.
         * It checks first an environment variable named [service]_API_KEY.
         * If nothing is found it checks for a file named [service]_api_key in the [JAR_LOCATION] folder.
         * If even the file is not found, the user is asked to provide one from the CLI.
         */
        fun apiKey(service: String) : String {
            val env = "${service}_API_KEY"
            val file = "${service.toLowerCase()}_api_key"

            var apiKey: String? = System.getenv(env)
            if (apiKey.isNullOrBlank()) {
                val f = File(JAR_LOCATION, file)
                if (f.exists()) {
                    apiKey = f.readText().trim()
                }
            }
            if (apiKey.isNullOrBlank()) {
                System.err.println("Unable to find $service API key! Set the $env environment variable or put a file named '$file' in the working dir")
                apiKey = askString("Provide API key manually:")
            }
            return apiKey
        }
    }
}

