package com.github.mmauro94.media_merger.group.movie.provider

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.group.GroupInfoException
import com.github.mmauro94.media_merger.group.Service
import com.github.mmauro94.media_merger.group.movie.info.TmdbMovie
import com.uwetrottmann.tmdb2.Tmdb

/**
 * [MovieProvider] for the TMDB service.
 */
object TmdbMovieProvider : MovieProvider<TmdbMovie> {

    override val service = Service.TMDB

    private val tmdb by lazy { Tmdb(service.apiKey()) }

    override fun search(query: String): List<TmdbMovie> {
        val search = try {
            tmdb
                .searchService()
                .movie(query, 1, Main.mainLanguages.first().iso639_1 ?: "en", null, null, null, null)
                .execute()
        } catch (e: Exception) {
            throw GroupInfoException(e.message)
        }
        val body = search.body()

        return body?.results?.map {
            TmdbMovie(it)
        } ?: throw GroupInfoException(search.errorBody()?.string() ?: "")
    }
}