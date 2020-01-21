package com.github.mmauro94.shows_merger.adjustment

import com.github.mmauro94.shows_merger.adjustment.audio.CutsAudioAdjuster
import com.github.mmauro94.shows_merger.adjustment.subtitle.CutsSubtitleAdjuster
import com.github.mmauro94.shows_merger.cuts.Cuts

/**
 * Adjustment to cut the track according to the provided cuts
 */
class CutsAdjustment(data: Cuts) : Adjustment<Cuts>(data) {

    override val outputConcat = listOf("cuts" + data.hashCode())

    override fun isValid(): Boolean {
        return !data.isEmptyOffset()
    }

    override val audioAdjusterFactory = ::CutsAudioAdjuster
    override val subtitleAdjusterFactory = ::CutsSubtitleAdjuster
}