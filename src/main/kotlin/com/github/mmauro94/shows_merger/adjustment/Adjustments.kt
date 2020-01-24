package com.github.mmauro94.shows_merger.adjustment

import com.github.mmauro94.shows_merger.OperationCreationException
import com.github.mmauro94.shows_merger.Track
import com.github.mmauro94.shows_merger.adjustment.audio.AudioAdjuster

/**
 * Class that contains a list of [Adjustment]s and the starting [inputTrack]
 */
data class Adjustments(
    val inputTrack: Track,
    val adjustments: List<Adjustment<*>>
) {

    /**
     * Returns the correct adjuster for the given [inputTrack]
     */
    private fun Adjustment<*>.adjuster(inputTrack: Track): TrackAdjuster<*> {
        return when{
            inputTrack.isAudioTrack() -> audioAdjuster(inputTrack)
            inputTrack.isSubtitlesTrack() -> subtitleAdjuster(inputTrack)
            else -> throw OperationCreationException("Unable to find adjuster for track type")
        }
    }

    /**
     * Adjusts by starting with the first adjustment in the [adjustments] list with the [inputTrack] as input, then
     * calls the next one with the output of the previous one until all adjustments are done.
     *
     * If an adjustment shouldn't adjust (see [Adjustment.isValid]), it is simply skipped.
     *
     * Returns the a [Track] for the output of the last adjustment. If no adjustments were made, returns `null`.
     */
    fun adjust(): Track? {
        var track: Track = this.inputTrack
        for (adj in adjustments) {
            val newInput = adj.adjuster(track).adjust()
            if (newInput != null) {
                track = newInput
            }
        }
        return if(track === inputTrack) null else track
    }
}
