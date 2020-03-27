package com.github.mmauro94.media_merger.adjustment

import com.github.mmauro94.media_merger.AdjustmentDetectionImpossible
import com.github.mmauro94.media_merger.Track
import com.github.mmauro94.media_merger.util.Reporter

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
            else -> throw IllegalStateException("Unable to find adjuster for track type")
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
    fun adjust(reporter: Reporter): Track? {
        var track: Track = this.inputTrack
        for ((i, adj) in adjustments.withIndex()) {
            val newInput = adj.adjuster(track).adjust(reporter.split(i, adjustments.size, "Adjusting $adj"))
            if (newInput != null) {
                track = newInput
            }
        }
        return if(track === inputTrack) null else track
    }
}
