package com.github.mmauro94.media_merger.group.movie

import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.group.movie.info.MovieInfo
import com.github.mmauro94.media_merger.util.filesystemCharReplace

class Movie(val movieInfo : MovieInfo?) : Group<Movie> {

    override fun toString() = outputName() ?: "Movie"

    override fun equals(other: Any?) = other is Movie

    override fun hashCode() = 1

    override fun outputName(): String? {
        return movieInfo?.let {
            if(it.year != null) {
                "${it.name} (${it.year})"
            } else {
                it.name
            }
        }?.filesystemCharReplace()
    }

    override fun compareTo(other: Movie) = 0
}