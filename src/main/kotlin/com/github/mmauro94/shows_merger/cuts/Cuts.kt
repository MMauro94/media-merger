package com.github.mmauro94.shows_merger.cuts

import com.github.mmauro94.shows_merger.video_part.VideoPart
import com.github.mmauro94.shows_merger.video_part.VideoPartMatch
import com.github.mmauro94.shows_merger.makeMillisPrecision
import java.time.Duration

/**
 * Class that holds a list of [Cut]s
 * @param cuts the [Cut]s. They should overlap at the target.
 */
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

    /**
     * If this [Cuts] represents a simple offset, then that offset is returned as a [Duration].
     * Otherwise this function returns `null`.
     */
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

    /**
     * Whether this [Cuts] represents a simple offset
     * @see optOffset
     */
    fun isOffset() = optOffset() != null

    /**
     * Returns true if this [Cuts] represents an empty offset
     */
    fun isEmptyOffset() = optOffset() == Duration.ZERO

    /**
     * Returns a list of [CutPart] by adding between this [cuts] some [Silence]s where appropriate
     */
    fun getCutParts(): List<CutPart> {
        var last: Cut? = null
        val ret = mutableListOf<CutPart>()
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


    companion object {
        val EMPTY = Cuts(emptyList())

        /**
         * Creates a new [Cuts] instance that represents a simple [offset]
         */
        fun ofOffset(offset: Duration): Cuts {
            return Cuts(
                listOf(
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
                )
            )
        }
    }
}

fun List<VideoPartMatch>.computeCuts(): Cuts {
    return Cuts(flatMap {
        when (it.type) {
            VideoPart.Type.BLACK_SEGMENT -> {
                if (it.input.duration < it.target.duration) {
                    listOf(
                        Cut(
                            it.input.start,
                            it.input.middle,
                            it.target.start
                        ),
                        Cut(
                            it.input.middle,
                            it.input.end,
                            it.target.end - it.input.halfDuration
                        )
                    )
                } else {
                    val halfDuration = it.target.duration.dividedBy(2L).makeMillisPrecision()
                    listOf(
                        Cut(
                            it.input.start,
                            it.input.start + halfDuration,
                            it.target.start
                        ),
                        Cut(
                            it.input.end - halfDuration,
                            it.input.end,
                            it.target.start + halfDuration
                        )
                    )
                }
            }
            VideoPart.Type.SCENE -> listOf(
                Cut(
                    it.input.start,
                    it.input.end,
                    it.target.start
                )
            )
        }
    })
}