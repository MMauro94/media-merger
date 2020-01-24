package com.github.mmauro94.shows_merger.adjustment

import com.github.mmauro94.shows_merger.Track
import com.github.mmauro94.shows_merger.adjustment.audio.AudioAdjuster
import com.github.mmauro94.shows_merger.adjustment.subtitle.SubtitleAdjuster
import java.io.File

/**
 * Represents an operation (adjustment) to apply to a track
 * @param T the the class defining the parameters for the operation
 * @param data an instance of [T] that defines the parameters of the adjustment
 */
abstract class Adjustment<T>(val data : T) {

    /**
     * Whether the adjustment should be done.
     * This should returns false for adjustment that make no sense (e.g. move track by 0 seconds)
     */
    abstract fun isValid(): Boolean

    /**
     * List of parameters to concat to the output file.
     * Should contain the type of operation and its params in a simplified form.
     */
    protected abstract val outputConcat: List<String>

    /**
     * Calculates the output file
     */
    private fun outputFile(inputTrack: Track) : File {
        return File(
            inputTrack.file.parentFile,
            inputTrack.file.nameWithoutExtension + "@adjusted@track" + inputTrack.id + "__" + outputConcat.joinToString(separator = "_") + "." + inputTrack.extension
        )
    }

    /**
     * A function that, given a [Track], an [Adjustment] and an output [File], creates an [AudioAdjuster]
     */
    protected abstract val audioAdjusterFactory : (Track, Adjustment<T>, File) -> AudioAdjuster<T>

    /**
     * The [AudioAdjuster] for the given [inputTrack]
     */
    fun audioAdjuster(inputTrack: Track): AudioAdjuster<T> {
        return audioAdjusterFactory(inputTrack, this, outputFile(inputTrack))
    }

    /**
     * A function that, given a [Track], an [Adjustment] and an output [File], creates an [SubtitleAdjuster]
     */
    protected abstract val subtitleAdjusterFactory : (Track, Adjustment<T>, File) -> SubtitleAdjuster<T>

    /**
     * The [SubtitleAdjuster] for the given [inputTrack]
     */
    fun subtitleAdjuster(inputTrack: Track): SubtitleAdjuster<T> {
        return subtitleAdjusterFactory(inputTrack, this, outputFile(inputTrack))
    }
}

