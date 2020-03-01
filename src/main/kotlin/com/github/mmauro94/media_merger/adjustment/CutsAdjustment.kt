package com.github.mmauro94.media_merger.adjustment

import com.github.mmauro94.media_merger.adjustment.audio.CutsAudioAdjuster
import com.github.mmauro94.media_merger.adjustment.subtitle.CutsSubtitleAdjuster
import com.github.mmauro94.media_merger.cuts.Cuts

/**
 * Adjustment to cut the track according to the provided cuts
 * @see Cuts
 */
class CutsAdjustment(cuts: Cuts) : Adjustment<Cuts>(cuts) {

    override val outputConcat = listOf("cuts" + cuts.hashCode()) //TODO better string representation

    override fun isValid(): Boolean {
        return !data.isEmptyOffset()
    }

    override fun toString() = "Cuts"

    override val audioAdjusterFactory = ::CutsAudioAdjuster
    override val subtitleAdjusterFactory = ::CutsSubtitleAdjuster
}