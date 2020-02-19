package com.github.mmauro94.media_merger.adjustment.subtitle

import com.github.mmauro94.media_merger.Track
import com.github.mmauro94.media_merger.adjustment.Adjustment
import com.github.mmauro94.media_merger.cuts.Cuts
import com.github.mmauro94.media_merger.subtitles.Subtitle
import java.io.File

class CutsSubtitleAdjuster(
    track: Track,
    adjustment: Adjustment<Cuts>,
    outputFile: File
) : SubtitleAdjuster<Cuts>(track, adjustment, outputFile) {

    override fun applyTransformations(subtitle: Subtitle<*>): Subtitle<*> {
        return subtitle.withCuts(adjustment.data)
    }

}