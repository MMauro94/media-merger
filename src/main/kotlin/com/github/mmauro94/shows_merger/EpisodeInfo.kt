package com.github.mmauro94.shows_merger

import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.entities.TvShow
import java.io.File
import java.util.*
import java.util.regex.Pattern

class EpisodeInfo(
    val season: Int,
    val episode: Int,
    val info: Pair<BaseTvShow, TvEpisode>?
) : Comparable<EpisodeInfo> {
    override fun toString() = outputName() ?: "Season $season, Episode $episode"

    override fun hashCode() = Objects.hash(season, episode)

    override fun equals(other: Any?) = other is EpisodeInfo && season == other.season && episode == other.episode

    fun outputName(): String? {
        return info?.let { i ->
            String.format(
                "%s %02dx%02d - %s",
                i.first.name,
                i.second.season_number,
                i.second.episode_number,
                i.second.name ?: "Episode ${i.second.episode_number}"
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

    override fun compareTo(other: EpisodeInfo) = COMPARATOR.compare(this, other)

    companion object {
        private val COMPARATOR = compareBy<EpisodeInfo> { it.season }.thenComparing { ei -> ei.episode }
    }
}


private val PATTERN = Pattern.compile("(?:(\\d+)x(\\d+)|S(\\d+)E(\\d+))")!!

fun String.detectEpisodeInfo(info: Pair<BaseTvShow, (Int) -> TvSeason?>?): EpisodeInfo? {
    val m = PATTERN.matcher(this)
    return if (m.find()) {
        val s = (m.group(1) ?: m.group(3))?.toInt() ?: throw IllegalStateException()
        val e = (m.group(2) ?: m.group(4))?.toInt() ?: throw IllegalStateException()
        if (m.find()) null else {
            val p = info?.let { (show, selectSeason) ->
                val season = selectSeason(s)
                if (season != null) {
                    val tve = season.episodes.find { it.episode_number == e }
                    if (tve != null) {
                        show to tve
                    } else {
                        System.err.println("Unable to find episode $e of season $s")
                        null
                    }
                } else null
            }
            EpisodeInfo(s, e, p)
        }
    } else null
}

val tmdb by lazy {
    var apiKey: String? = System.getenv("TMDB_API_KEY")
    if (apiKey.isNullOrBlank()) {
        val f = File(("tmdb_api_key"))
        if (f.exists()) {
            apiKey = f.readText().trim()
        }
    }
    if (apiKey.isNullOrBlank()) {
        System.err.println("Unable to find TMDB API key! Set the TMDB_API_KEY environment variable or put a file named 'tmdb_api_key' in the working dir")
        apiKey = askString("Provide api key manually:")
    }
    if (apiKey.isNullOrBlank()) null
    else Tmdb(apiKey!!)
}