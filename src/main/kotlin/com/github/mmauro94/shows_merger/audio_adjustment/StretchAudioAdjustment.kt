package com.github.mmauro94.shows_merger.audio_adjustment

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.Track
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder

/**
 * Adjustment to change by a certain stretch factor an audio track
 */
class StretchAudioAdjustment(
    adjustment: StretchFactor
) : AbstractAudioAdjustment<StretchFactor>(adjustment) {

    override val outputConcat = listOf("stretch${adjustment.speedMultiplier.toPlainString()}")

    override fun prepare(inputTrack: Track) {
        targetDuration = inputTrack.durationOrFileDuration?.let { adjustment.resultingDurationForStretchFactor(it) }
    }

    override fun shouldAdjust(): Boolean {
        return !adjustment.isEmpty()
    }

    override fun FFmpegBuilder.fillBuilder(inputTrack: Track) {
    }

    override fun FFmpegOutputBuilder.fillOutputBuilder(inputTrack: Track) {
        addExtraArgs("-map", "0:${inputTrack.id}")
        setAudioFilter("atempo=" + adjustment.speedMultiplier.toPlainString())
    }
}