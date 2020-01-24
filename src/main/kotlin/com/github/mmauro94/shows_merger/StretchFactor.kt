package com.github.mmauro94.shows_merger

import com.github.mmauro94.shows_merger.Framerate.Companion.FPS_23_976
import com.github.mmauro94.shows_merger.Framerate.Companion.FPS_25
import com.github.mmauro94.shows_merger.adjustment.StretchAdjustment
import com.github.mmauro94.shows_merger.util.humanStr
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration


private val KNOWN_STRETCH_FACTORS = arrayOf(
    FPS_25.calculateStretchTo(FPS_23_976),
    FPS_23_976.calculateStretchTo(FPS_25)
)

/**
 * Represents a stretching parameter, to speedup/slowdown a track.
 * @param name the name of this stretch factor. Optional.
 */
class StretchFactor private constructor(
    val durationMultiplier: BigDecimal,
    val speedMultiplier: BigDecimal,
    val name: String?
) {

    /**
     * Returns true iif this stretch factor is exactly equal to 1
     */
    fun isEmpty() = speedMultiplier.compareTo(BigDecimal.ONE) == 0

    override fun hashCode() = speedMultiplier.hashCode()
    override fun equals(other: Any?) = other is StretchFactor && speedMultiplier == other.speedMultiplier

    override fun toString(): String {
        val str =
            "speedMultiplier=" + speedMultiplier.toPlainString() + ", durationMultiplier=" + durationMultiplier.toPlainString()
        return when {
            isEmpty() -> "No stretch (1.0)"
            name != null -> "$name ($str)"
            else -> "Stretch factor: $str"
        }
    }

    /**
     * Returns the duration [Duration] of track after applying this stretch factor to a track of the provided [duration].
     */
    fun resultingDurationForStretchFactor(duration: Duration): Duration {
        return Duration.ofNanos(duration.toNanos().toBigDecimal().multiply(durationMultiplier).toBigInteger().longValueExact())
    }

    /**
     * Creates a new [StretchAdjustment] from this instance
     */
    fun audioAdjustment() = StretchAdjustment(this)

    companion object {

        val EMPTY = StretchFactor(BigDecimal.ONE, BigDecimal.ONE, null)

        //region FACTORY METHODS
        /**
         * Creates a new stretch factor providing a [durationMultiplier], that must have exactly 3 decimal digits.
         * The [durationMultiplier] must be:
         * * &gt; 1 for slowdowns (i.e. the total duration increases)
         * * &lt; 1 for speedups (i.e. the total duration decreases)
         */
        fun ofDurationMultiplier(durationMultiplier: BigDecimal, name: String?): StretchFactor {
            require(durationMultiplier > BigDecimal.ZERO)
            require(durationMultiplier.scale() == 3)
            return StretchFactor(
                durationMultiplier = durationMultiplier,
                speedMultiplier = BigDecimal.ONE.divide(durationMultiplier, 3, RoundingMode.HALF_UP),
                name = name
            )
        }

        /**
         * Creates a new stretch factor providing a [speedMultiplier].
         * The [speedMultiplier] must be:
         * * &lt; 1 for slowdowns (i.e. the speed decreases)
         * * &gt; 1 for speedups (i.e. the speed increases)
         */
        fun ofSpeedMultiplier(speedMultiplier: BigDecimal, name: String?): StretchFactor {
            require(speedMultiplier > BigDecimal.ZERO)
            require(speedMultiplier.scale() == 3)
            return StretchFactor(
                durationMultiplier = BigDecimal.ONE.divide(speedMultiplier, 3, RoundingMode.HALF_UP),
                speedMultiplier = speedMultiplier,
                name = name
            )
        }
        //endregion

        //region DETECTION METHODS
        /**
         * Detects and returns the [StretchFactor] between two [Framerate]s, or `null` if unable to detect.
         */
        fun detectFromFramerate(framerate: Framerate?, targetFramerate: Framerate?): StretchFactor? {
            return if (framerate != null && targetFramerate != null) {
                framerate.calculateStretchTo(targetFramerate)
            } else null
        }

        /**
         * Detects and returns the [StretchFactor] between two [Duration]s using some known stretch factors or `null` if unable to detect.
         * The parameter [maxDurationError] defines the maximum error that the two durations can have.
         */
        fun detectFromDuration(duration: Duration?, targetDuration: Duration?, maxDurationError : Duration= Duration.ofSeconds(2)): StretchFactor? {
            if (duration != null && targetDuration != null) {
                KNOWN_STRETCH_FACTORS.forEach { sf ->
                    val resultingDuration = sf.resultingDurationForStretchFactor(duration)
                    if (resultingDuration > targetDuration.minus(maxDurationError) && resultingDuration < targetDuration.plus(
                            maxDurationError
                        )
                    ) {
                        return sf
                    }
                }
            }
            return null
        }

        /**
         * Detects (or asks) and returns the [StretchFactor] between two [InputFile]s, or `null` if unable to detect.
         *
         * Prioritizes the detection using framerates and duration. If both fails, resorts to asking the user.
         *
         * Returns a [Pair] of [StretchFactor] and a [Boolean] that is true if the factor was asked to the user.
         * Returns `null` if the user doesn't want to provide stretch manually.
         */
        fun detectOrAsk(inputFile: InputFile, targetFile: InputFile): Pair<StretchFactor, Boolean>? {
            val fromFramerate = detectFromFramerate(inputFile.framerate, targetFile.framerate)
            if (fromFramerate != null) return fromFramerate to false

            val fromDuration = detectFromDuration(inputFile.duration, targetFile.duration)
            if (fromDuration != null) return fromDuration to false

            val fromUser = askStretchFactor(inputFile, targetFile)
            return if (fromUser != null) fromUser to true
            else null
        }
        //endregion

        /**
         * Asks the user for a stretch factor to adjust the files.
         * Returns the [StretchFactor] or `null` if the user decided not to provide one.
         */
        fun askStretchFactor(inputFile: InputFile, targetFile: InputFile): StretchFactor? {
            val originalDuration = inputFile.duration
            val targetDuration = targetFile.duration

            val possibleOutcomes = KNOWN_STRETCH_FACTORS.joinToString(", ") { sf ->
                (if (originalDuration == null) null else sf.resultingDurationForStretchFactor(originalDuration)).humanStr()
            }

            println("$inputFile cannot be adjusted because of unknown stretch factor (duration=${originalDuration.humanStr()}, target=${targetDuration.humanStr()}, possibleOutcomes=$possibleOutcomes)")
            val provideManually = askYesNo("Provide adjustment manually?", false)
            return if (provideManually) {
                println("0) No adjustment (${originalDuration.humanStr()})")
                KNOWN_STRETCH_FACTORS.forEachIndexed { i, sf ->
                    val resultingDuration =
                        if (originalDuration == null) null else sf.resultingDurationForStretchFactor(originalDuration)
                    println("${i + 1}) ${sf.name} (resulting duration: ${resultingDuration.humanStr()})")
                }
                val stretchSelection = askInt(
                    "Select wanted resulting duration: ",
                    0,
                    KNOWN_STRETCH_FACTORS.size
                )
                if (stretchSelection == 0) EMPTY
                else KNOWN_STRETCH_FACTORS[stretchSelection - 1]
            } else null
        }
    }
}

/**
 * Multiplies this duration by the given [stretchFactor], enlarging it if the stretch factor is a slowdown, and
 * shrinking it if it's a speedup.
 */
operator fun Duration.times(stretchFactor: StretchFactor): Duration {
    return stretchFactor.resultingDurationForStretchFactor(this)
}