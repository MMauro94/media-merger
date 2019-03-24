package com.github.mmauro94.shows_merger

import java.util.regex.Pattern

data class EpisodeInfo(val season: Int, val episode: Int) {
    override fun toString() = "Season $season, Episode $episode"
}


private val PATTERN = Pattern.compile("(?:(\\d+)x(\\d+)|S(\\d+)E(\\d+))")!!

fun String.detectEpisodeInfo(): EpisodeInfo? {
    val m = PATTERN.matcher(this)
    return if (m.find()) {
        val s = m.group(1) ?: m.group(3) ?: throw IllegalStateException()
        val e = m.group(2) ?: m.group(4) ?: throw IllegalStateException()
        if (m.find()) null else EpisodeInfo(s.toInt(), e.toInt())
    } else null
}