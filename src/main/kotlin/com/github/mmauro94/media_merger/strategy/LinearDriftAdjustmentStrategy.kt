package com.github.mmauro94.media_merger.strategy

import com.github.mmauro94.media_merger.Framerate
import com.github.mmauro94.media_merger.InputFile
import com.github.mmauro94.media_merger.KNOWN_LINEAR_DRIFTS
import com.github.mmauro94.media_merger.LinearDrift
import com.github.mmauro94.media_merger.util.CliDescriptor
import com.github.mmauro94.media_merger.util.askLong
import com.github.mmauro94.media_merger.util.log.Logger
import com.github.mmauro94.media_merger.util.toTimeString
import java.math.RoundingMode
import java.time.Duration

/**
 * Different strategies to detect the linear drift to be applied to a given file
 */
sealed class LinearDriftAdjustmentStrategy {

    abstract fun detect(log: Logger, inputFile: InputFile, targetFile: InputFile): LinearDrift?

    /**
     * The stretch is never adjusted. Use it if you are sure all the files don't have any linear drift.
     */
    @CliDescriptor("None")
    @Suppress("unused")
    object None : LinearDriftAdjustmentStrategy() {
        override fun detect(log: Logger, inputFile: InputFile, targetFile: InputFile) = LinearDrift.NONE
        override fun toString() = "None"
    }

    /**
     * Calculates the linear drift using the file framerates and using the one closer one of the [KNOWN_LINEAR_DRIFTS].
     */
    @CliDescriptor("By framerate (known only)")
    @Suppress("unused")
    object ByFramerate : LinearDriftAdjustmentStrategy() {
        override fun detect(log: Logger, inputFile: InputFile, targetFile: InputFile): LinearDrift? {
            return detectFromFramerate(log, inputFile.framerate, targetFile.framerate)?.toKnownOnly()
        }

        override fun toString() = "By framerate (known only)"
    }

    /**
     * Calculates the linear drift using the file framerates.
     */
    @CliDescriptor("By framerate (precise)")
    @Suppress("unused")
    object ByPreciseFramerate : LinearDriftAdjustmentStrategy() {
        override fun detect(log: Logger, inputFile: InputFile, targetFile: InputFile): LinearDrift? {
            return detectFromFramerate(log, inputFile.framerate, targetFile.framerate)
        }

        override fun toString() = "By framerate (precise)"
    }


    /**
     * Calculates the linear drift using the file durations.
     * The parameter [maxDurationError] defines the maximum error that the two durations can have.
     */
    @CliDescriptor("By duration (known only)")
    @Suppress("unused")
    class ByDuration(val maxDurationError: Duration) : LinearDriftAdjustmentStrategy() {
        override fun detect(log: Logger, inputFile: InputFile, targetFile: InputFile): LinearDrift? {
            return detectFromDuration(log, inputFile.duration, targetFile.duration, maxDurationError)?.toKnownOnly()
        }

        override fun toString() = "By duration (known only, max duration error ${maxDurationError.toTimeString()})"

        companion object {
            @Suppress("unused")
            fun ask(): ByDuration {
                return ByDuration(
                    Duration.ofSeconds(
                        askLong(
                            question = "Select max duration error (in seconds)",
                            isValid = { this >= 0 },
                            default = 2
                        )
                    )
                )
            }
        }
    }

    /**
     * Calculates the linear drift using the file durations.
     */
    @CliDescriptor("By duration (precise)")
    object ByPreciseDuration : LinearDriftAdjustmentStrategy() {
        override fun detect(log: Logger, inputFile: InputFile, targetFile: InputFile): LinearDrift? {
            return detectFromDuration(log, inputFile.duration, targetFile.duration, null)
        }

        override fun toString() = "By duration (precise)"
    }


    companion object {

        private fun LinearDrift.toKnownOnly(): LinearDrift? {
            return KNOWN_LINEAR_DRIFTS.minByOrNull { (it.speedMultiplier - this.speedMultiplier).abs() }!!
        }

        /**
         * Detects and returns the [LinearDrift] between two [Framerate]s, or `null` if unable to detect.
         */
        private fun detectFromFramerate(
            log: Logger,
            framerate: Framerate?,
            targetFramerate: Framerate?
        ): LinearDrift? {
            return when {
                framerate == null -> null.also {
                    log.debug("Input framerate unknown, cannot calculate ratio based on framerate")
                }
                targetFramerate == null -> null.also {
                    log.debug("Target framerate unknown, cannot calculate ratio based on framerate")
                }
                else -> framerate.calculateLinearDriftTo(targetFramerate).also {
                    log.debug("Input framerate: $framerate")
                    log.debug("Target framerate: $targetFramerate")
                    log.debug("Calculated precise linear drift: $it")
                }
            }
        }

        /**
         * Detects and returns the [LinearDrift] between two [Duration]s.
         * When the parameter [maxDurationError] is null the precise linear drift calculated with the duration is returned.
         * When it is not null all known linear drifts are tried, the one where the error exeeds it are discarded and the the one with the min error is returned.
         */
        private fun detectFromDuration(
            log: Logger,
            duration: Duration?,
            targetDuration: Duration?,
            maxDurationError: Duration?
        ): LinearDrift? {
            return when {
                duration == null -> null.also {
                    log.debug("Input duration unknown, cannot calculate ratio based on duration")
                }
                targetDuration == null -> null.also {
                    log.debug("Target duration unknown, cannot calculate ratio based on duration")
                }
                maxDurationError == null -> {
                    LinearDrift.ofSpeedMultiplier(
                        speedMultiplier = duration.toNanos().toBigDecimal().divide(targetDuration.toNanos().toBigDecimal(), 3, RoundingMode.HALF_UP),
                        name = "${duration.toTimeString()} to ${targetDuration.toTimeString()}"
                    )
                }
                else -> {
                    log.debug("Max duration error is ${maxDurationError.toTimeString()}")
                    KNOWN_LINEAR_DRIFTS
                        .mapNotNull { sf ->
                            val resultingDuration = sf.resultingDurationForLinearDrift(duration)
                            val error = (resultingDuration - targetDuration).abs()
                            val ok = error <= maxDurationError
                            log.debug("Resulting duration with linear drift $sf would be ${resultingDuration.toTimeString()}" + if (!ok) " (exceeds max error by ${error.toTimeString()})" else "")
                            if (ok) sf to resultingDuration
                            else null
                        }
                        .minByOrNull { it.second }
                        ?.first
                }
            }
        }
    }
}