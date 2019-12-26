package com.github.mmauro94.shows_merger.show_info

import java.time.Year

interface ShowInfo {
    val givenName: String
    val name: String

    @Throws(ShowInfoException::class)
    fun episodeInfo(season: Int, episode: Int): EpisodeInfo

    companion object {
        fun computeName(service: String, id: String, name: String?, year: Year?): String {
            var n = name ?: ""
            val y = year?.let { "($it)" }
            if (y != null && !n.endsWith(y)) {
                n += " $y"
            }
            return "$n - $service ID:${id}"
        }
    }
}

