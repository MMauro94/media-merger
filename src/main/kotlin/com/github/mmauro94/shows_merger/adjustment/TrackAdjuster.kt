package com.github.mmauro94.shows_merger.adjustment

import com.github.mmauro94.shows_merger.InputFile
import com.github.mmauro94.shows_merger.Track
import java.io.File

/**
 * A class specialized in adjusting a [Track]
 * @param track The track to adjust
 * @param adjustment The adjustment that has to be done
 * @param outputFile The output file
 */
abstract class TrackAdjuster<T>(
    val track: Track,
    val adjustment: Adjustment<T>,
    val outputFile: File
) {

    /**
     * Convenience property that exposes [adjustment]'s [Adjustment.data]
     */
    protected val data = adjustment.data

    /**
     * Performs the adjustment
     */
    protected abstract fun doAdjust(): Boolean

    /**
     * Adjusts the given [track] with the [adjustment]
     *
     * Returns a [Track] instance representing the track on the newly adjusted file,
     * or null if the adjustment didn't have to be done. (See [Adjustment.isValid])
     *
     * If a file with the same name is already present, the actual adjustment is not done,
     * but its [Track] is returned anyway, basically acting as a cache.
     */
    fun adjust(): Track? {
        val res = when {
            outputFile.exists() -> true
            !adjustment.isValid() -> false
            else -> doAdjust()
        }
        return if (res) {
            return InputFile.parse(outputFile).tracks.single()
        } else null

    }
}