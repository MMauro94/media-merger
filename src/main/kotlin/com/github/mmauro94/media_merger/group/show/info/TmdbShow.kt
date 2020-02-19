package com.github.mmauro94.media_merger.group.show.info

import com.github.mmauro94.media_merger.group.show.provider.TmdbShowProvider
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import java.time.Instant
import java.time.Year
import java.time.ZoneId

/**
 * A show downloaded by TMDB
 */
data class TmdbShow(private val tmdbShow: BaseTvShow) : ShowInfo() {
    val id = tmdbShow.id!!

    override val givenName = tmdbShow.name ?: ""

    override val name = ShowInfo.computeName(
        "TMDB",
        id.toString(),
        tmdbShow.name,
        tmdbShow.first_air_date?.let { Year.from(Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault())) }
    )

    override fun toString() = super.toString()

    override fun episodeInfo(season: Int, episode: Int): EpisodeInfo {
        return TmdbShowProvider.episodeInfo(this, season, episode)
    }
}