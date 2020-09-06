package com.github.mmauro94.media_merger.util

import com.beust.klaxon.Json
import com.github.mmauro94.media_merger.LinearDrift
import com.github.mmauro94.media_merger.times
import java.time.Duration

/**
 * A time span (e.g. from 00:01.123 to 00:15.456)
 * @param start the start time, must be > 0
 * @param end the end time, must be greater than [start]
 */
data class DurationSpan(val start: Duration, val end: Duration) {

    init {
        require(!start.isNegative)
        require(end > start)
    }

    /**
     * The duration of this span
     */
    @Json(ignored = true)
    val duration: Duration = end - start

    /**
     * Commodity property that is half of [duration]
     */
    @Json(ignored = true)
    val halfDuration: Duration = duration.dividedBy(2L)

    /**
     * Commodity property that is the middle of span
     */
    @Json(ignored = true)
    val middle: Duration = start + halfDuration

    /**
     * Returns a new [DurationSpan] of the first half of this span.
     */
    fun firstHalf() = DurationSpan(
        start = start,
        end = middle
    )

    /**
     * Returns a new [DurationSpan] of the second half of this span.
     */
    fun secondHalf() = DurationSpan(
        start = middle,
        end = end
    )

    /**
     * Returns if [other] duration span is directly after this span
     */
    fun isConsecutiveOf(other: DurationSpan): Boolean {
        return start == other.end
    }

    /**
     * Returns the consecutive [DurationSpan] with the given [length]
     */
    fun consecutiveOfLength(length: Duration): DurationSpan {
        require(!length.isNegative && !length.isZero)
        return DurationSpan(end, end + length)
    }

    fun consecutiveOfSameLength(): DurationSpan {
        return consecutiveOfLength(duration)
    }

    /**
     * Returns whether this span and [other] intersect.
     */
    fun intersects(other: DurationSpan): Boolean {
        return intersection(other) != null
    }

    /**
     * Returns the intersection between this span and [other].
     * Returns `null` if no intersection exists.
     */
    fun intersection(other: DurationSpan): DurationSpan? {
        val start = maxOf(start, other.start)
        val end = minOf(start, other.start)
        return if (start < end) DurationSpan(start, end)
        else null
    }

    fun isContainedIn(other : DurationSpan): Boolean {
        return start >= other.start && end <= other.end
    }

    /**
     * Returns a new [DurationSpan] restricted in the [other] span.
     * If the resulting span would be empty, returns `null`
     */
    fun restrictIn(other: DurationSpan): DurationSpan? {
        val newStart = maxOf(start, other.start)
        val newEnd = minOf(end, other.end)
        return if (newStart >= newEnd) {
            null
        } else DurationSpan(
            start = newStart,
            end = newEnd
        )
    }

    /**
     * Moves this span by the given [offset].
     */
    operator fun plus(offset: Duration): DurationSpan {
        return DurationSpan(
            start = start + offset,
            end = end + offset
        )
    }

    /**
     * Multiplies this span by the given [linearDrift].
     */
    operator fun times(linearDrift: LinearDrift): DurationSpan {
        return DurationSpan(
            start = start * linearDrift,
            end = end * linearDrift
        )
    }

    override fun toString(): String {
        return "${start.toTimeString()} --> ${end.toTimeString()}"
    }
}

fun List<DurationSpan>.restrictTo(start: Duration, end: Duration?): List<DurationSpan> {
    require(!start.isNegative)
    if (end != null) {
        require(start < end)
    }
    return mapNotNull {
        when {
            it.end <= start -> null //In this case the entire span is before the range, thus ignore
            it.start < start -> it.copy(start = start) //In this case we just truncate the span to start at the start of the range

            end != null && it.start > end -> null //In this case the entire span is after the range, thus ignore
            end != null && it.end > end -> it.copy(end = end) //In this case we just truncate the span to end at the end of the range
            else -> it //Otherwise return normally
        }
    }
}