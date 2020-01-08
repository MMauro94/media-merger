package com.github.mmauro94.shows_merger

import java.awt.Frame
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

data class Framerate(val framerate: BigDecimal) {
    init {
        require(framerate.scale() == 3)
    }

    override fun toString() = "${framerate.stripTrailingZeros().toPlainString()} fps"
}

fun InputFile.detectFramerate() : Framerate? {
     val frameDuration =
        tracks.singleOrNull { it.isVideoTrack() }?.mkvTrack?.properties?.defaultDuration?.toNanos()?.toBigDecimal()
    return if (frameDuration != null) {
        Framerate(Duration.ofSeconds(1).toNanos().toBigDecimal().setScale(3).divide(frameDuration, RoundingMode.HALF_UP))
    } else null
}