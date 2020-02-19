package com.github.mmauro94.media_merger.show

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
) : Comparable<Episode> {
    override fun toString() = outputName() ?: "Season $season, Episode $episode"

    override fun hashCode() = Objects.hash(season, episode)

    override fun equals(other: Any?) = other is Episode && season == other.season && episode == other.episode

    /**
     * The name to output to the file. This function also takes care of stripping invalid characters such as '?' or ':'
     * by replacing them with similar characters.
     */
    fun outputName(): String? {
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


private val PATTERN = Pattern.compile("(?:(\\d+)x(\\d+)|S(\\d+)E(\\d+))", Pattern.CASE_INSENSITIVE)!!

fun String.detectEpisode(show: ShowInfo?): Episode? {
    val m = PATTERN.matcher(this)
    return if (m.find()) {
        val s = (m.group(1) ?: m.group(3))?.toInt() ?: throw IllegalStateException()
        val e = (m.group(2) ?: m.group(4))?.toInt() ?: throw IllegalStateException()
        if (m.find()) null else {
            val epInfo = try {
                show?.episodeInfo(s, e)
            } catch (e: ShowInfoException) {
                System.err.println(e.message)
                null
            }
            Episode(s, e, epInfo)
        }
    } else null
}

