package com.github.mmauro94.shows_merger.audio_adjustment

import com.github.mmauro94.shows_merger.Track

/**
 * Class that contains a list of [AbstractAudioAdjustment] and the starting [inputTrack]
 */
data class AudioAdjustments(
    val inputTrack: Track,
    val adjustments: List<AbstractAudioAdjustment<*>>
) {

    /**
     * Adjusts by starting with the first adjustment in the [adjustments] list with the [inputTrack] as input, then
     * calls the next one with the output of the previous one until all adjustments are done.
     *
     * If an adjustment shouldn't adjust (see [AbstractAudioAdjustment.shouldAdjust]), it is simply skipped.
     *
     * Returns the a [Track] for the output of the last adjustment. If no adjustments were made, returns `null`.
     */
    fun adjust(): Track? {
        var track: Track = this.inputTrack
        for ((i, adj) in adjustments.withIndex()) {
            val newInput = adj.adjust(track, "${i + 1}/${adjustments.size}")
            if (newInput != null) {
                track = newInput
            }
        }
        return if(track == inputTrack) null else track
    }
}
