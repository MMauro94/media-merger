package com.github.mmauro94.media_merger.show

import com.github.mmauro94.media_merger.Group
import com.github.mmauro94.media_merger.show.info.EpisodeInfo
import com.github.mmauro94.media_merger.show.info.ShowInfo
import com.github.mmauro94.media_merger.show.info.ShowInfoException
import java.util.*
import java.util.regex.Pattern

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
        return episodeInfo?.let { i ->
            String.format(
                "%s %02dx%02d - %s",
                i.show.givenName,
                i.seasonNumber,
                i.episodeNumber,
                i.name ?: "Episode ${i.episodeNumber}"
            )
                .replace(':', '꞉')
                .replace('/', '／')
                .replace('\\', '＼')
                .replace('?', '？')
                .replace(Regex("\"([^\"]+)\""), "‟$1”")
                .replace('\"', '＂')
                .replace('*', '∗')
                .replace('<', '❮')
                .replace('>', '❯')
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



