package com.github.mmauro94.media_merger.group.movie.info

import com.github.mmauro94.media_merger.group.show.provider.TmdbShowProvider
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import java.time.Instant
import java.time.Year
import java.time.ZoneId

/**
 * A movie downloaded by TMDB
 */
data class TmdbMovie(private val tmdbMovie: BaseMovie) : MovieInfo() {
    val id = tmdbMovie.id!!

    override val name = tmdbMovie.title ?: tmdbMovie.original_title ?: throw Exception("Movie $id with no title")

    override val year = tmdbMovie.release_date?.let { Year.of(1900 + it.year) }

    override fun toString() = super.toString()
}