package com.github.mmauro94.shows_merger.cuts

import com.github.mmauro94.shows_merger.util.DurationSpan
import com.github.mmauro94.shows_merger.video_part.VideoPart
import com.github.mmauro94.shows_merger.video_part.VideoPartMatch
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
                    require(!c1.targetTime.intersects(c2.targetTime))
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
                return cut.offset
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
     * Returns a list of [CutPart] by adding between this [cuts] some [Empty]s where appropriate
     */
    fun getCutParts(): List<CutPart> {
        var last: Cut? = null
        val ret = mutableListOf<CutPart>()
        for (cut in cuts) {
            val targetEnd = last?.targetTime?.end ?: Duration.ZERO
            if (cut.targetTime.start > targetEnd) {
                ret.add(Empty(cut.targetTime.start - targetEnd))
            }
            ret.add(cut)
            last = cut
        }
        return ret
    }

    companion object {
        /**
         * A [Cuts] instance representing no action
         */
        val EMPTY = Cuts(emptyList())

        /**
         * Creates a new [Cuts] instance that represents a simple [offset]
         */
        fun ofOffset(offset: Duration): Cuts {
            return Cuts(
                listOf(
                    if (offset.isNegative) {
                        Cut(
                            time = DurationSpan(
                                offset.abs(),
                                Duration.ofDays(999)
                            ),
                            targetStart = Duration.ZERO
                        )
                    } else {
                        Cut(
                            time = DurationSpan(
                                Duration.ZERO,
                                Duration.ofDays(999)
                            ),
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
                if (it.input.time.duration < it.target.time.duration) {
                    //If the input black segment is too short I put the first half at the start
                    //of the target and second half at the end. the middle will be empty
                    listOf(
                        Cut(
                            time = it.input.time.firstHalf(),
                            targetStart = it.target.time.start
                        ),
                        Cut(
                            time = it.input.time.secondHalf(),
                            targetStart = it.target.time.end - it.input.time.halfDuration
                        )
                    )
                } else {
                    //If the input black segment is too long I simply cut the middle of it
                    listOf(
                        Cut(
                            time = DurationSpan(
                                it.input.time.start,
                                it.input.time.start + it.target.time.halfDuration
                            ),
                            targetStart = it.target.time.start
                        ),
                        Cut(
                            time = DurationSpan(
                                it.input.time.end - it.target.time.halfDuration,
                                it.input.time.end
                            ),
                            targetStart = it.target.time.middle
                        )
                    )
                }
            }
            VideoPart.Type.SCENE -> listOf(
                Cut(
                    time = DurationSpan(
                        start = it.input.time.start,
                        end = minOf(
                            it.input.time.end,
                            it.input.time.start + it.target.time.duration
                        )
                    ),
                    targetStart = it.target.time.start
                )
            )
        }
    })
}