package com.github.mmauro94.shows_merger.adjustment.audio

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.Track
import com.github.mmauro94.shows_merger.adjustment.Adjustment
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import java.io.File
import java.time.Duration

class StretchAudioAdjuster(
    track: Track,
    adjustment: Adjustment<StretchFactor>,
    outputFile: File
) : AudioAdjuster<StretchFactor>(track, adjustment, outputFile) {

    override val targetDuration: Duration? =
        track.durationOrFileDuration?.let { data.resultingDurationForStretchFactor(it) }

    override fun FFmpegBuilder.fillBuilder() {
    }

    override fun FFmpegOutputBuilder.fillOutputBuilder() {
        addExtraArgs("-map", "0:${track.id}")
        setAudioFilter("atempo=" + data.speedMultiplier.toPlainString())
    }
}
