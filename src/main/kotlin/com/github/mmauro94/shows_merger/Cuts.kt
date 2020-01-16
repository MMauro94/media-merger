package com.github.mmauro94.shows_merger

import com.github.mmauro94.shows_merger.VideoPart.Type.BLACK_SEGMENT
import com.github.mmauro94.shows_merger.VideoPart.Type.SCENE
import java.time.Duration

/**
 * Sealed class that represents a part of audio that can either be [Silence] or a [Cut].
 */
sealed class SilenceOrCut {
    /**
     * The duration
     */
    abstract val duration: Duration
}

class Cut(val startCut: Duration, val endCut: Duration, val targetStart: Duration) : SilenceOrCut() {

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
    val targetEnd: Duration = targetStart + duration

    fun targetIntersects(cut: Cut): Boolean {
        return if (targetStart < cut.targetStart) targetEnd > cut.targetStart
        else targetStart < cut.targetEnd
    }
}

class Silence(override val duration: Duration) : SilenceOrCut() {
    init {
        duration.requireMillisPrecision()
    }
}

data class Cuts(val cuts: List<Cut>) {

    init {
        cuts.forEach { c1 ->
            cuts.forEach { c2 ->
                if (c1 != c2) {
                    //require(!c1.targetIntersects(c2))
                    //TODO fix
                }
            }
        }
    }

    fun isEmpty() = optOffset() == Duration.ZERO

    fun getSilenceOrCuts(): List<SilenceOrCut> {
        var last: Cut? = null
        val ret = mutableListOf<SilenceOrCut>()
        for (cut in cuts) {
            val targetEnd = last?.targetEnd ?: Duration.ZERO
            if (cut.targetStart > targetEnd) {
                ret.add(Silence(cut.targetStart - targetEnd))
            }
            ret.add(cut)
            last = cut
        }
        return ret
    }

    fun optOffset(): Duration? {
        return when (cuts.size) {
            0 -> Duration.ZERO
            1 -> {
                val cut = cuts.single()
                return cut.targetStart - cut.startCut
            }
            else -> null
        }
    }

    companion object {
        fun empty() = Cuts(emptyList())

        fun ofOffset(offset: Duration): Cuts {
            return Cuts(listOf(
                if (offset.isNegative) {
                    Cut(
                        startCut = offset.abs(),
                        endCut = Duration.ofDays(999),
                        targetStart = Duration.ZERO
                    )
                } else {
                    Cut(
                        startCut = Duration.ZERO,
                        endCut = Duration.ofDays(999),
                        targetStart = offset
                    )
                }
            ))
        }
    }
}

fun List<VideoPartMatch>.computeCuts(): Cuts {
    return Cuts(flatMap {
        when (it.type) {
            BLACK_SEGMENT -> {
                if (it.input.duration < it.target.duration) {
                    listOf(
                        Cut(it.input.start, it.input.middle, it.target.start),
                        Cut(it.input.middle, it.input.end, it.target.end - it.input.halfDuration)
                    )
                } else {
                    val halfDuration = it.target.duration.dividedBy(2L).makeMillisPrecision()
                    listOf(
                        Cut(it.input.start, it.input.start + halfDuration, it.target.start),
                        Cut(it.input.end - halfDuration, it.input.end, it.target.start + halfDuration)
                    )
                }
            }
            SCENE -> listOf(Cut(it.input.start, it.input.end, it.target.start))
        }
    })
}