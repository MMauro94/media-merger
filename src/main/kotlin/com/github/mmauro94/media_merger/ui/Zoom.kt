package com.github.mmauro94.media_merger.ui

import java.time.Duration
import kotlin.math.roundToInt

data class Zoom(val value: Double) {
    val msWidth = (1000 / 60000.0) * value

    fun calcX(duration: Duration): Int {
        return (msWidth * duration.toMillis()).roundToInt()
    }
    companion object{
        val DEFAULT = Zoom(1.0)
    }
}