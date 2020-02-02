package com.github.mmauro94.shows_merger.util

import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.TimeUnit


fun Duration.toTimeString(separator: Char = ':'): String {
    return if (this.isZero) {
        "0"
    } else {
        mutableListOf<String>().apply {
            fun add(i: Int, pad: Int = 2) {
                add(i.toString().padStart(pad, '0'))
            }
            add(toHoursPart())
            add(toMinutesPart())
            add(toSecondsPart())
            add(toNanosPart(), 9)
        }.joinToString(separator = separator.toString())
    }
}

fun Duration?.toTimeStringOrUnknown(separator: Char = ':'): String = this?.toTimeString(separator) ?: "Unknown"

fun String.parseTimeString(separator: Char = ':'): Duration? {
    val sep = Regex.escape(separator.toString())
    val regex = "(?:([0-9]+)$sep)?(?:([0-9]+)$sep)?([0-9]+)$sep([0-9]+)".toRegex()
    val units = listOf(TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS, TimeUnit.NANOSECONDS)
    return regex.matchEntire(this)?.let { m ->
        val totalNanos =
            m.groupValues.drop(1).zip(units.drop(units.size - (m.groupValues.size - 1))).map { (match, unit) ->
                unit.toNanos(match.toLong())
            }.sum()
        Duration.ofNanos(totalNanos)
    }
}


/**
 * Converts [this] double as a number of seconds and coverts it to a [Duration]
 */
fun Double.asSecondsDuration() =
    if (this == 0.0) null else Duration.ofSeconds(toLong(), ((this % 1) * 1000000000).toLong())!!

/**
 * Converts [this] BigDecimal as a number of seconds and coverts it to a [Duration]
 */
fun BigDecimal.asSecondsDuration() = Duration.ofNanos(this.setScale(9).unscaledValue().longValueExact())!!

/**
 * Converts this duration to a [BigDecimal] in seconds and returns its plain string representation.
 */
fun Duration.toTotalSeconds(): String = BigDecimal.valueOf(toNanos(), 9).toPlainString()

/**
 * Sums this iterable of [Duration]s.
 */
fun Iterable<Duration>.sum(): Duration = fold(Duration.ZERO) { acc, it -> acc + it }
