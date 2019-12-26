package com.github.mmauro94.shows_merger.show_provider

import com.github.mmauro94.shows_merger.askString
import com.github.mmauro94.shows_merger.show_info.ShowInfo
import com.github.mmauro94.shows_merger.show_info.ShowInfoException
import java.io.File

interface ShowProvider<S : ShowInfo> {
    @Throws(ShowInfoException::class)
    fun searchShow(query: String): List<S>

    companion object {
        fun apiKey(service: String) : String {
            val env = "${service}_API_KEY"
            val file = "${service.toLowerCase()}_api_key"

            var apiKey: String? = System.getenv(env)
            if (apiKey.isNullOrBlank()) {
                val f = File(file)
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

