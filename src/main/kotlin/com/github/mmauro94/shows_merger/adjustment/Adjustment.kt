package com.github.mmauro94.shows_merger.adjustment

import com.github.mmauro94.shows_merger.Track
import com.github.mmauro94.shows_merger.adjustment.audio.AudioAdjuster
import java.io.File

/**
 * Represents an operation (adjustment) to apply to a track
 * @param T the type of the class defining the parameters for the operation
 */
abstract class AbstractAdjustment<T>(val data : T) {

    /**
     * Whether the adjustment should be done.
     * This should returns false for adjustment that make no sense (e.g. move track by 0 seconds)
     */
    protected abstract fun isValid(): Boolean

    /**
     * List of parameters to concat to the output audio file.
     * Should contain the type of operation and its params in a simplified form.
     */
    protected abstract val outputConcat: List<String>

    protected fun outputFile(inputTrack: Track) : File {
        return File(
            inputTrack.file.parentFile,
            inputTrack.file.nameWithoutExtension + "@adjusted@track" + inputTrack.id + "__" + outputConcat.joinToString(separator = "_") + "." + inputTrack.audioExtension
        )
    }

    abstract fun audioAdjuster(inputTrack: Track): AudioAdjuster<T>
}

