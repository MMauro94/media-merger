package com.github.mmauro94.shows_merger.cuts

import com.github.mmauro94.shows_merger.requireMillisPrecision
import com.github.mmauro94.shows_merger.util.DurationSpan
import java.time.Duration

/**
 * Sealed class that represents a part of a cut. All [CutPart]s are concatenated together. *
 */
sealed class CutPart {
    /**
     * The duration of this part
     */
    abstract val duration: Duration
}

/**
 * [CutPart] inheritor that "cuts" data from the source.
 * @param startCut where to start cutting from the source file
 * @param endCut where to stop cutting from the source file
 * @param targetStart where this piece of cut should be start in the target file
 */
class Cut(val time: DurationSpan, val targetStart: Duration) : CutPart() {

    val targetTime = DurationSpan(targetStart, targetStart + time.duration)

    override val duration: Duration = time.start
}

/**
 * [CutPart] inheritor that represents a certain amount of emptiness.
 */
class Empty(override val duration: Duration) : CutPart() {
    init {
        duration.requireMillisPrecision()
    }
}

