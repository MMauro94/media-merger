package com.github.mmauro94.media_merger.util

import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.TimeUnit


fun Duration.toTimeString(): String {
    val abs = abs()
    val firstPart = (if (isNegative) "-" else "") + mutableListOf<String>().apply {
        fun add(v: Int) {
            add(v.toString().padStart(2, '0'))
        }
        if (abs.toHoursPart() > 0) {
            add(abs.toHoursPart())
        }
        add(abs.toMinutesPart())
        add(abs.toSecondsPart())
    }.joinToString(":")

    val secondPart = if (abs.nano > 0) {
        mutableListOf<String>().apply {
            var added = false
            fun add(v: Int) {
                if (added || v > 0) {
                    add(v.toString().padStart(3, '0'))
                    added = true
                }
            }

            add(abs.nano % 1000)
            add((abs.nano / 1000) % 1000)
            add(abs.toMillisPart())
            reverse()
        }.joinToString(separator = "", prefix = ".")
    } else ""

    return firstPart + secondPart
}

fun Duration?.toTimeStringOrUnknown(): String = this?.toTimeString() ?: "Unknown"

fun String.parseTimeStringOrNull(): Duration? {
    val regex = "(-?)(?:(?:([0-9]{1,2}):)?([0-9]{1,2}):)?([0-9]{1,2})(?:\\.([0-9]{1,9}))?".toRegex()
    val units = listOf(TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS)
    return regex.matchEntire(this)?.let { m ->
        val neg = m.groupValues[1] == "-"
        val totalNanos =
            m.groupValues.drop(2).zip(units).map { (match, unit) ->
                unit.toNanos(match.toLongOrNull() ?: 0)
            }.sum() + m.groupValues.last().padEnd(9, '0').toLong()
        Duration.ofNanos(totalNanos * if (neg) -1 else 1)
    }
}


/**
 * Converts [this] double as a number of seconds and coverts it to a [Duration]
 */
fun Double.toSecondsDuration(onZero: Duration? = null): Duration? {
    check(this >= 0)
    return if (this == 0.0) onZero else Duration.ofSeconds(toLong(), ((this % 1) * 1000000000).toLong())!!
}

/**
 * Converts [this] BigDecimal as a number of seconds and coverts it to a [Duration]
 */
fun BigDecimal.toSecondsDuration() = Duration.ofNanos(this.setScale(9).unscaledValue().longValueExact())!!

/**
 * Converts this duration to a [BigDecimal] in seconds and returns its plain string representation.
 */
fun Duration.toTotalSeconds(): String = BigDecimal.valueOf(toNanos(), 9).toPlainString()


/**
 * Sums this iterable of [Duration]s.
 */
fun Iterable<Duration>.sum(): Duration = fold(Duration.ZERO) { acc, it -> acc + it }
