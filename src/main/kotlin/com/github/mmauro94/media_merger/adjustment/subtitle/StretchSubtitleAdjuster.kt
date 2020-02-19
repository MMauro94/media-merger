package com.github.mmauro94.media_merger.adjustment.subtitle

import com.github.mmauro94.media_merger.StretchFactor
import com.github.mmauro94.media_merger.Track
import com.github.mmauro94.media_merger.adjustment.Adjustment
import com.github.mmauro94.media_merger.subtitles.Subtitle
import java.io.File

class StretchSubtitleAdjuster(
    track: Track,
    adjustment: Adjustment<StretchFactor>,
    outputFile: File
) : SubtitleAdjuster<StretchFactor>(track, adjustment, outputFile) {

    override fun applyTransformations(subtitle: Subtitle<*>): Subtitle<*> {
        return subtitle * adjustment.data
    }

}
