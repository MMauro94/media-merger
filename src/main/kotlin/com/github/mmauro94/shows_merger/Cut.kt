package com.github.mmauro94.shows_merger

import java.time.Duration

sealed class SilenceOrCut {
    abstract val duration : Duration

    class Cut(val startCut: Duration, val endCut: Duration, val targetStart: Duration) : SilenceOrCut() {

        init {
            require(!startCut.isNegative)
            require(startCut < endCut)
            require(!targetStart.isNegative)
        }

        override val duration: Duration = endCut - startCut
        val targetEnd: Duration = targetStart + duration

        fun targetIntersects(cut: Cut): Boolean {
            return if (targetStart < cut.targetStart) targetEnd > cut.targetStart
            else targetStart < cut.targetEnd
        }
    }

    class Silence(override val duration: Duration) : SilenceOrCut()
}

data class Cuts(val cuts: List<SilenceOrCut.Cut>) {

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

    fun getSilenceOrCuts() : List<SilenceOrCut> {
        var last : SilenceOrCut.Cut? = null
        val ret = mutableListOf<SilenceOrCut>()
        for(cut in cuts) {
            val targetEnd = last?.targetEnd ?: Duration.ZERO
            if(cut.targetStart > targetEnd) {
                ret.add(SilenceOrCut.Silence(cut.targetStart - targetEnd))
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
            return Cuts(
                listOf(
                    if (offset.isNegative) {
                        SilenceOrCut.Cut(
                            startCut = offset.abs(),
                            endCut = Duration.ofDays(999),
                            targetStart = Duration.ZERO
                        )
                    } else {
                        SilenceOrCut.Cut(
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