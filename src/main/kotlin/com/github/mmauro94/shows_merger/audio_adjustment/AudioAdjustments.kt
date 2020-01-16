package com.github.mmauro94.shows_merger.audio_adjustment

import com.github.mmauro94.shows_merger.InputFile
import com.github.mmauro94.shows_merger.Track

data class AudioAdjustments(
    val inputTrack: Track,
    val adjustments: List<AbstractAudioAdjustment<*>>
) {
    fun adjustAll(): InputFile? {
        var input: Track = this.inputTrack
        for ((i, adj) in adjustments.withIndex()) {
            val newInput = adj.adjust(input, "${i + 1}/${adjustments.size}")
            if (newInput != null) {
                input = newInput
            }
        }
        return if(input == inputTrack) null else input.inputFile
    }
}
