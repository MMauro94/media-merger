package com.github.mmauro94.shows_merger.adjustment

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.adjustment.audio.StretchAudioAdjuster
import com.github.mmauro94.shows_merger.adjustment.subtitle.StretchSubtitleAdjuster

/**
 * Adjustment to change by a certain stretch factor an audio track
 */
class StretchAdjustment(
    adjustment: StretchFactor
) : Adjustment<StretchFactor>(adjustment) {

    override val outputConcat = listOf("stretch${adjustment.speedMultiplier.toPlainString()}")

    override fun isValid(): Boolean {
        return !data.isEmpty()
    }

    override val audioAdjusterFactory = ::StretchAudioAdjuster
    override val subtitleAdjusterFactory = ::StretchSubtitleAdjuster

}