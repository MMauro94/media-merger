package com.github.mmauro94.media_merger.group.movie

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.group.movie.info.MovieInfo
import com.github.mmauro94.media_merger.util.filesystemCharReplace
import com.github.mmauro94.media_merger.util.namedFormat

class Movie(val movieInfo: MovieInfo?) : Group<Movie> {

    override fun toString() = outputName() ?: "Movie"

    override fun equals(other: Any?) = other is Movie

    override fun hashCode() = 1

    override fun outputName(): String? {
        return movieInfo?.let { movie ->
            Main.config.movieRenameFormat.namedFormat(
                mapOf(
                    "name" to movie.name,
                    "year" to (movie.year?.toString() ?: "")
                )
            ).filesystemCharReplace()
        }
    }

    override fun compareTo(other: Movie) = 0
}