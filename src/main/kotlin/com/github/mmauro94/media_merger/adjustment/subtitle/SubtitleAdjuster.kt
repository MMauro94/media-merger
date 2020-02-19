package com.github.mmauro94.media_merger.adjustment.subtitle

import com.github.mmauro94.media_merger.Track
import com.github.mmauro94.media_merger.adjustment.Adjustment
import com.github.mmauro94.media_merger.adjustment.TrackAdjuster
import com.github.mmauro94.media_merger.subtitles.Subtitle
import java.io.File

/**
 * Base instance of [TrackAdjuster] for all adjustments to be made on a subtitle track.
 */
abstract class SubtitleAdjuster<T>(
    track: Track,
    adjustment: Adjustment<T>,
    outputFile: File
) : TrackAdjuster<T>(track, adjustment, outputFile) {

    init {
        require(track.isSubtitlesTrack())
    }

    abstract fun applyTransformations(subtitle: Subtitle<*>): Subtitle<*>

    override fun doAdjust(): Boolean {
        val file = track.fileOrExtracted()
        val subtitle = Subtitle.parse(file)
        return if(subtitle != null) {
            applyTransformations(subtitle).save(outputFile)
            true
        } else false
    }
}