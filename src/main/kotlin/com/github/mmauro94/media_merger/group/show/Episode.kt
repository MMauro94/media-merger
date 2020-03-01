package com.github.mmauro94.media_merger.group.show

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.group.show.info.EpisodeInfo
import com.github.mmauro94.media_merger.util.filesystemCharReplace
import com.github.mmauro94.media_merger.util.namedFormat
import java.util.*

/**
 * Represents an episode, composed of a [season] number and an [episode] number. Can optionally have an [episodeInfo].
 */
class Episode(
    val season: Int,
    val episode: Int,
    val episodeInfo: EpisodeInfo?
) : Group<Episode> {

    override fun toString() = outputName() ?: "Season $season, Episode $episode"

    override fun hashCode() = Objects.hash(season, episode)

    override fun equals(other: Any?) = other is Episode && season == other.season && episode == other.episode

    /**
     * The name to output to the file. This function also takes care of stripping invalid characters such as '?' or ':'
     * by replacing them with similar characters.
     */
    override fun outputName(): String? {
        return episodeInfo?.let {
            Main.config.episodeRenameFormat.namedFormat(
                mapOf(
                    "showName" to it.show.givenName,
                    "showYear" to (it.show.year?.toString() ?: ""),
                    "seasonNumber" to it.seasonNumber,
                    "episodeNumber" to it.episodeNumber,
                    "episodeName" to (it.name ?: "Episode ${it.episodeNumber}")
                )
            ).filesystemCharReplace()
        }
    }

    override fun compareTo(other: Episode) = COMPARATOR.compare(this, other)

    companion object {
        /**
         * Comparator ordering first by season number, than by episode number
         */
        private val COMPARATOR = compareBy<Episode> { it.season }.thenComparing { ei -> ei.episode }
    }
}



