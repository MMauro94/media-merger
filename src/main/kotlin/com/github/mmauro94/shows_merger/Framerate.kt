package com.github.mmauro94.shows_merger

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

/**
 * Class that represents a framerate (e.g. 25.000 fps)
 * @param framerate the framerate that has always exactly 4 decimal digits
 */
data class Framerate(val framerate: BigDecimal) {
    init {
        require(framerate.scale() == 3)
    }

    override fun toString() = "${framerate.stripTrailingZeros().toPlainString()} fps"
}

/**
 * Detects the framerate of an [InputFile] using its video track, if present.
 *
 * If unable to detect, returns `null`.
 */
fun InputFile.detectFramerate(): Framerate? {
    val frameDuration =
        tracks.singleOrNull { it.isVideoTrack() }?.mkvTrack?.properties?.defaultDuration?.toNanos()?.toBigDecimal()
    return if (frameDuration != null) {
        Framerate(
            Duration.ofSeconds(1).toNanos().toBigDecimal().setScale(3).divide(
                frameDuration,
                RoundingMode.HALF_UP
            )
        )
    } else null
}