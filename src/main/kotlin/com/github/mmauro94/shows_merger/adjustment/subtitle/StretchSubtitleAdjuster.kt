package com.github.mmauro94.shows_merger.adjustment.subtitle

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.Track
import com.github.mmauro94.shows_merger.adjustment.Adjustment
import com.github.mmauro94.shows_merger.adjustment.audio.AudioAdjuster
import com.github.mmauro94.shows_merger.subtitles.Subtitle
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import java.io.File
import java.time.Duration

class StretchSubtitleAdjuster(
    track: Track,
    adjustment: Adjustment<StretchFactor>,
    outputFile: File
) : SubtitleAdjuster<StretchFactor>(track, adjustment, outputFile) {

    override fun applyTransformations(subtitle: Subtitle<*>): Subtitle<*> {
        return subtitle * adjustment.data
    }

}
