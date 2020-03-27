package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.Framerate.Companion.FPS_23_976
import com.github.mmauro94.media_merger.Framerate.Companion.FPS_25
import com.github.mmauro94.media_merger.adjustment.LinearDriftAdjustment
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration


val KNOWN_LINEAR_DRIFTS = arrayOf(
    LinearDrift.NONE,
    FPS_25.calculateLinearDriftTo(FPS_23_976),
    FPS_23_976.calculateLinearDriftTo(FPS_25)
)

/**
 * Represents a stretching parameter, to speedup/slowdown a track, aka linear drift.
 * @param name the name of this linear drift. Optional.
 */
class LinearDrift private constructor(
    val durationMultiplier: BigDecimal,
    val speedMultiplier: BigDecimal,
    val name: String?
) {

    /**
     * Returns true iif this linear drift is exactly equal to 1
     */
    fun isNone() = speedMultiplier.compareTo(BigDecimal.ONE) == 0

    override fun hashCode() = speedMultiplier.hashCode()
    override fun equals(other: Any?) = other is LinearDrift && speedMultiplier == other.speedMultiplier

    override fun toString(): String {
        val str =
            "speedMultiplier=" + speedMultiplier.toPlainString() + ", durationMultiplier=" + durationMultiplier.toPlainString()
        return when {
            isNone() -> "No linear drift (1.0)"
            name != null -> "$name ($str)"
            else -> "Linear drift: $str"
        }
    }

    /**
     * Returns the duration [Duration] of track after applying this linear drift to a track of the provided [duration].
     */
    fun resultingDurationForLinearDrift(duration: Duration): Duration {
        return Duration.ofNanos(duration.toNanos().toBigDecimal().multiply(durationMultiplier).toBigInteger().longValueExact())
    }

    /**
     * Creates a new [LinearDriftAdjustment] from this instance
     */
    fun audioAdjustment() = LinearDriftAdjustment(this)

    companion object {

        /**
         * A null linear drift, equal to 1.0
         */
        val NONE = LinearDrift(
            BigDecimal.ONE,
            BigDecimal.ONE,
            null
        )

        //region FACTORY METHODS
        /**
         * Creates a new linear drift providing a [durationMultiplier], that must have exactly 3 decimal digits.
         * The [durationMultiplier] must be:
         * * &gt; 1 for slowdowns (i.e. the total duration increases)
         * * &lt; 1 for speedups (i.e. the total duration decreases)
         */
        fun ofDurationMultiplier(durationMultiplier: BigDecimal, name: String?): LinearDrift {
            require(durationMultiplier > BigDecimal.ZERO)
            require(durationMultiplier.scale() == 3)
            return LinearDrift(
                durationMultiplier = durationMultiplier,
                speedMultiplier = BigDecimal.ONE.divide(durationMultiplier, 3, RoundingMode.HALF_UP),
                name = name
            )
        }

        /**
         * Creates a new linear drift providing a [speedMultiplier].
         * The [speedMultiplier] must be:
         * * &lt; 1 for slowdowns (i.e. the speed decreases)
         * * &gt; 1 for speedups (i.e. the speed increases)
         */
        fun ofSpeedMultiplier(speedMultiplier: BigDecimal, name: String?): LinearDrift {
            require(speedMultiplier > BigDecimal.ZERO)
            require(speedMultiplier.scale() == 3)
            return LinearDrift(
                durationMultiplier = BigDecimal.ONE.divide(speedMultiplier, 3, RoundingMode.HALF_UP),
                speedMultiplier = speedMultiplier,
                name = name
            )
        }
        //endregion
    }
}

/**
 * Multiplies this duration by the given [linearDrift], enlarging it if the linear drift is a slowdown, and
 * shrinking it if it's a speedup.
 */
operator fun Duration.times(linearDrift: LinearDrift): Duration {
    return linearDrift.resultingDurationForLinearDrift(this)
}