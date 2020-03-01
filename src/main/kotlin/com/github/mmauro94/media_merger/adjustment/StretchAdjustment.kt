package com.github.mmauro94.media_merger.adjustment

import com.github.mmauro94.media_merger.StretchFactor
import com.github.mmauro94.media_merger.adjustment.audio.StretchAudioAdjuster
import com.github.mmauro94.media_merger.adjustment.subtitle.StretchSubtitleAdjuster

/**
 * Adjustment to change by a certain stretch factor an audio track
 * @see StretchFactor
 */
class StretchAdjustment(
    stretchFactor: StretchFactor
) : Adjustment<StretchFactor>(stretchFactor) {

    override val outputConcat = listOf("stretch${stretchFactor.speedMultiplier.toPlainString()}")

    override fun isValid(): Boolean {
        return !data.isEmpty()
    }

    override fun toString() = data.toString()

    override val audioAdjusterFactory = ::StretchAudioAdjuster
    override val subtitleAdjusterFactory = ::StretchSubtitleAdjuster

}