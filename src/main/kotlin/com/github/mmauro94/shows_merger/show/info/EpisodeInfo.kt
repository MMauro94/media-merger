package com.github.mmauro94.shows_merger.show.info

/**
 * Info for an episode
 */
data class EpisodeInfo (
    val show : ShowInfo,
    val seasonNumber : Int,
    val episodeNumber : Int,
    val name : String?
)