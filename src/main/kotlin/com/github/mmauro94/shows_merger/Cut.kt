package com.github.mmauro94.shows_merger

import java.time.Duration

data class Cut(val startCut: Duration, val endCut: Duration, val targetStart: Duration) {

    init {
        require(!startCut.isNegative)
        require(startCut < endCut)
        require(!targetStart.isNegative)
    }

    val duration: Duration = endCut - startCut
    val targetEnd: Duration = targetStart + duration

    fun targetIntersects(cut: Cut): Boolean {
        return if (targetStart < cut.targetStart) targetEnd > cut.targetStart
        else targetStart < cut.targetEnd
    }

}

data class Cuts(val cuts: List<Cut>) {

    init {
        cuts.forEach { c1 ->
            cuts.forEach { c2 ->
                if (c1 != c2) {
                    require(!c1.targetIntersects(c2))
                }
            }
        }
    }

    fun isEmpty() = optOffset() == Duration.ZERO

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