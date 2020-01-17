package com.github.mmauro94.shows_merger.video_part

import java.time.Duration

/**
 * A match between two video parts of two different files.
 *
 * Ensures that they are of the same type.
 */
data class VideoPartMatch(val input: VideoPart, val target: VideoPart) {
    init {
        require(input.type == target.type)
    }

    val type = input.type
}

/**
 * Tries to match two [VideoParts] of two different files.
 *
 * Returns `null` if no match can be found.
 */
fun VideoParts.matchWithTarget(targets: VideoParts): List<VideoPartMatch>? {
    if (this.parts.isNotEmpty() && this.parts.size == targets.parts.size && this.parts.first().type == this.parts.first().type) {
        val zip = this.scenes().zip(targets.scenes())
        val numberOfAcceptableErrors = zip.count { (input, target) -> (input.duration - target.duration).abs() < Duration.ofMillis(150) }
        if (numberOfAcceptableErrors / zip.size.toFloat() > 0.8) {
            return this.parts.zip(targets.parts).map { (input, target) ->
                VideoPartMatch(input, target)
            }
        }
    }
    return null
}