package com.github.mmauro94.shows_merger.util

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.makeMillisPrecision
import com.github.mmauro94.shows_merger.requireMillisPrecision
import com.github.mmauro94.shows_merger.times
import java.time.Duration


data class DurationSpan(val start: Duration, val end: Duration) {

    init {
        start.requireMillisPrecision()
        end.requireMillisPrecision()

        require(!start.isNegative)
        require(start < end)
    }

    val duration: Duration = end - start

    /**
     * Commodity property that is half of [duration]
     */
    val halfDuration: Duration = duration.dividedBy(2L).makeMillisPrecision()

    /**
     * Commodity property that is the middle of span
     */
    val middle: Duration = start + halfDuration

    fun firstHalf() = DurationSpan(
        start = start,
        end = middle
    )

    fun secondHalf() = DurationSpan(
        start = middle,
        end = end
    )


    fun intersects(other: DurationSpan): Boolean {
        return if (start < other.start) end > other.start
        else start < other.end
    }

    fun restrictIn(other: DurationSpan): DurationSpan? {
        val newStart = maxOf(start, other.start)
        val newEnd = minOf(end, other.end)
        return if(newStart >= newEnd) {
            null
        } else DurationSpan(
            start = newStart,
            end = newEnd
        )
    }

    fun moveBy(offset: Duration): DurationSpan {
        return DurationSpan(
            start = start + offset,
            end = end + offset
        )
    }

    operator fun times(stretchFactor: StretchFactor): DurationSpan {
        return DurationSpan(
            start = start * stretchFactor,
            end = end * stretchFactor
        )
    }
}