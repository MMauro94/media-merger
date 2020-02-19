package com.github.mmauro94.media_merger.group

import com.github.mmauro94.media_merger.Main

enum class Service {
    TVDB, TMDB;

    fun apiKey(): String {
        return Main.config.apiKeyForService(name)
    }
}