package com.github.mmauro94.media_merger.group.movie.info

import java.time.Year

abstract class MovieInfo {

    /**
     * The name of the movie
     */
    abstract val name: String

    /**
     * The year of release of the movie
     */
    abstract  val year : Year?

    override fun toString(): String {
        return if(year != null) {
            "$name ($year)"
        } else {
            name
        }
    }
}