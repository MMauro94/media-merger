package com.github.mmauro94.media_merger.adjustment

import com.github.mmauro94.media_merger.LinearDrift
import com.github.mmauro94.media_merger.adjustment.audio.LinearDriftAudioAdjuster
import com.github.mmauro94.media_merger.adjustment.subtitle.LinearDriftSubtitleAdjuster

/**
 * Adjustment to change by a certain linear drift an audio track
 * @see LinearDrift
 */
class LinearDriftAdjustment(
    linearDrift: LinearDrift
) : Adjustment<LinearDrift>(linearDrift) {

    override val outputConcat = listOf("lineardrift${linearDrift.speedMultiplier.toPlainString()}")

    override fun isValid(): Boolean {
        return !data.isNone()
    }

    override fun toString() = data.toString()

    override val audioAdjusterFactory = ::LinearDriftAudioAdjuster
    override val subtitleAdjusterFactory = ::LinearDriftSubtitleAdjuster

}