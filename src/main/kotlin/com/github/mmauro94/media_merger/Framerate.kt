package com.github.mmauro94.media_merger

import org.apache.commons.lang3.math.Fraction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration


/**
 * Class that represents a framerate (e.g. 25.000 fps)
 * @param framerate the framerate that has always exactly 3 decimal digits
 */
data class Framerate(val framerate: BigDecimal) {
    init {
        require(framerate.scale() == 3)
    }

    override fun toString() = "${framerate.stripTrailingZeros().toPlainString()} fps"

    /**
     * Calculates the [LinearDrift] that must be applied to this framerate to comply with [target] timings.
     */
    fun calculateLinearDriftTo(target: Framerate): LinearDrift {
        return LinearDrift.ofDurationMultiplier(
            framerate.divide(target.framerate, 3, RoundingMode.HALF_UP),
            "$this to $target"
        )
    }

    /**
     * Calculates the [LinearDrift] that must be applied to [input] framerate to comply with this timings.
     */
    fun calculateLinearDriftFrom(input: Framerate): LinearDrift {
        return input.calculateLinearDriftTo(this)
    }

    companion object {
        val FPS_25 = Framerate(BigDecimal("25.000"))
        val FPS_23_976 = Framerate(BigDecimal("23.976"))
    }
}

/**
 * Detects the framerate of an [InputFile] using its video track, if present.
 *
 * If unable to detect, returns `null`.
 */
fun InputFile.detectFramerate(): Framerate? {
    val videoTrack = tracks.singleOrNull { it.isVideoTrack() } ?: return null
    val frameDuration = videoTrack.mkvTrack.properties?.defaultDuration?.toNanos()?.toBigDecimal()
    return if (frameDuration != null) {
        Framerate(
            Duration.ofSeconds(1).toNanos().toBigDecimal().setScale(3).divide(
                frameDuration,
                RoundingMode.HALF_UP
            )
        )
    } else {
        val avg: Fraction? = videoTrack.ffprobeStream.avg_frame_rate
        if (avg != null) {
            Framerate(
                avg.numerator.toBigDecimal().divide(avg.denominator.toBigDecimal(), 3, RoundingMode.HALF_UP)
            )
        } else null
    }
}