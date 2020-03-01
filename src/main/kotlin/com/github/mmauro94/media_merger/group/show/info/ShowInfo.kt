package com.github.mmauro94.media_merger.group.show.info

import java.time.Year
import com.github.mmauro94.media_merger.group.GroupInfoException

/**
 * Interface that holds the info of a shot
 */
abstract class ShowInfo {

    /**
     * The raw name given by the show info provider
     */
    abstract val givenName: String

    /**
     * A smart name generated that should include more info (e.g. the year) and that is displayed to the user in the CLI
     */
    abstract val name: String

    /**
     * The year of first airing of the show, or null
     */
    abstract val year: Year?

    /**
     * Returns the information about a particular episode
     * @throws GroupInfoException if the info cannot be returned
     */
    abstract fun episodeInfo(season: Int, episode: Int): EpisodeInfo

    override fun toString() = name

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

