package com.github.mmauro94.media_merger.adjustment.subtitle

import com.github.mmauro94.media_merger.LinearDrift
import com.github.mmauro94.media_merger.Track
import com.github.mmauro94.media_merger.adjustment.Adjustment
import com.github.mmauro94.media_merger.subtitles.Subtitle
import java.io.File

class LinearDriftSubtitleAdjuster(
    track: Track,
    adjustment: Adjustment<LinearDrift>,
    outputFile: File
) : SubtitleAdjuster<LinearDrift>(track, adjustment, outputFile) {

    override fun applyTransformations(subtitle: Subtitle<*>): Subtitle<*> {
        return subtitle * adjustment.data
    }

}
