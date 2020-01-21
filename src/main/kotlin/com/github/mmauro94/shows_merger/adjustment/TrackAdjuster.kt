package com.github.mmauro94.shows_merger.adjustment

import com.github.mmauro94.shows_merger.Track
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import java.io.File

abstract class TrackAdjustmer<T : AbstractAdjustment>(val adjustment: T) {


    /**
     * Changes the [FFmpegBuilder] with adjustment specific options
     * @param inputTrack the track that is being edited
     */
    protected abstract fun FFmpegBuilder.fillBuilder(inputTrack: Track)

    /**
     * Changes the [FFmpegOutputBuilder] with adjustment specific options
     * @param inputTrack the track that is being edited
     */
    protected abstract fun FFmpegOutputBuilder.fillOutputBuilder(inputTrack: Track)

    /**
     * Adjusts the given [inputTrack] with the [adjustment]
     *
     * Returns a [Track] instance representing the track of the newly adjusted audio file,
     * or null if the adjustment didn't have to be done.
     *
     * If a file with the same name is already present, the actual adjustment is not done
     */
    abstract fun adjust(outputFile: File, inputTrack: Track): Track?
}