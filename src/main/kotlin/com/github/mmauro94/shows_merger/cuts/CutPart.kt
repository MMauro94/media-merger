package com.github.mmauro94.shows_merger.cuts

import com.github.mmauro94.shows_merger.requireMillisPrecision
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
class Cut(val startCut: Duration, val endCut: Duration, val targetStart: Duration) : CutPart() {

    init {
        startCut.requireMillisPrecision()
        endCut.requireMillisPrecision()
        targetStart.requireMillisPrecision()

        require(!startCut.isNegative)
        require(startCut < endCut)
        require(!targetStart.isNegative)

        require(endCut < Duration.ofHours(1))
    }

    override val duration: Duration = endCut - startCut
    /**
     * Where this piece of cut should be end in the target file
     */
    val targetEnd: Duration = targetStart + duration

    /**
     * Returns true if this and [cut] intersect at the target file.
     */
    fun targetIntersects(cut: Cut): Boolean {
        return if (targetStart < cut.targetStart) targetEnd > cut.targetStart
        else targetStart < cut.targetEnd
    }
}

/**
 * [CutPart] inheritor that represents a certain amount of silence.
 */
class Silence(override val duration: Duration) : CutPart() {
    init {
        duration.requireMillisPrecision()
    }
}

