package com.github.mmauro94.media_merger.group.movie

import com.github.mmauro94.media_merger.group.Grouper
import com.github.mmauro94.media_merger.group.movie.info.MovieInfo
import com.github.mmauro94.media_merger.group.movie.provider.TmdbMovieProvider

class MovieGrouper(val movie: MovieInfo?) : Grouper<Movie> {

    override fun detectGroup(filename: String): Movie? {
        return Movie(movie)
    }

    companion object {

        fun create(): MovieGrouper {
            return MovieGrouper(Grouper.select("movie", TmdbMovieProvider))
        }
    }
}