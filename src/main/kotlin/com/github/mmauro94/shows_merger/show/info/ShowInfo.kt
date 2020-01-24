package com.github.mmauro94.shows_merger.show.info

import java.time.Year

/**
 * Interface that holds the info of a shot
 */
interface ShowInfo {

    /**
     * The raw name given by the show info provider
     */
    val givenName: String

    /**
     * A smart name generated that should include more info (e.g. the year) and that is displayed to the user in the CLI
     */
    val name: String

    /**
     * Returns the information about a particular episode
     * @throws ShowInfoException if the info cannot be returned
     */
    fun episodeInfo(season: Int, episode: Int): EpisodeInfo

    companion object {

        /**
         * Given some info, computes the name to show to the user
         * @param service the name of the service that downloaded this show
         * @param id the id that the service gave this show
         * @param name the given name for the show
         * @param year the year of the show
         */
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

