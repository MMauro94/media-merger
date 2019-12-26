package com.github.mmauro94.shows_merger

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import kotlin.math.roundToLong


private val FPS_25 = BigDecimal("25")
private val FPS_23_9 = BigDecimal("23.976215")

private val KNOWN_STRETCH_FACTORS = arrayOf(
    "25 fps to 23.976215 fps" to FPS_25.divide(FPS_23_9, 6, RoundingMode.HALF_UP),
    "23.976215 fps to 25 fps" to FPS_23_9.divide(FPS_25, 6, RoundingMode.HALF_UP)
)

data class Adjustment(
    val inputFile: InputFile,
    val stretchFactor: BigDecimal,
    val offset: Duration,
    val targetDuration: Duration
)

private fun resultingDurationForStretchFactor(duration: Duration, stretchFactor: BigDecimal): Duration {
    return Duration.ofNanos((duration.toNanos() * stretchFactor.toDouble()).roundToLong())
}


private fun selectStretchFactor(duration: Duration, targetDuration: Duration): BigDecimal? {
    KNOWN_STRETCH_FACTORS.forEach { (_, sf) ->
        if (MergeOptions.isDurationValid(resultingDurationForStretchFactor(duration, sf), targetDuration)) {
            return sf
        }
    }
    return null
}

/**
 * @return Adjustment, userSelected
 */
fun selectAdjustment(inputFile: InputFile, targetDuration: Duration): Pair<Adjustment, Boolean>? {
    val originalDuration = inputFile.duration
    if (originalDuration == null) {
        println()
        println("$inputFile cannot be adjusted because of unknown duration")
        return null
    }

    var stretchFactor = selectStretchFactor(originalDuration, targetDuration)
    return if (stretchFactor == null) {
        val possibleOutcomes = KNOWN_STRETCH_FACTORS.joinToString(", ") { (_, sf) ->
            resultingDurationForStretchFactor(originalDuration, sf).humanStr()
        }
        println()
        println("Track $inputFile cannot be adjusted because of unknown duration ratio (duration=${originalDuration.humanStr()}, target=${targetDuration.humanStr()}, possibleOutcomes=$possibleOutcomes)")
        val adjustAnyway = askYesNo("Adjust anyway with custom parameters?", false)
        if (adjustAnyway) {
            println("0) No adjustment (${originalDuration.humanStr()})")
            KNOWN_STRETCH_FACTORS.forEachIndexed { i, (name, sf) ->
                val resultingDuration = resultingDurationForStretchFactor(originalDuration, sf)
                println("${i + 1}) $name (resulting duration: ${resultingDuration.humanStr()})")
            }
            val stretchSelection = askInt(
                "Select wanted resulting duration: ",
                0,
                KNOWN_STRETCH_FACTORS.size
            )
            stretchFactor = if (stretchSelection == 0) BigDecimal.ONE
            else KNOWN_STRETCH_FACTORS[stretchSelection - 1].second

            val moveDuration = Duration.ofMillis(askInt("Select wanted offset (in ms): ").toLong())

            Adjustment(inputFile, stretchFactor, moveDuration, targetDuration) to true
        } else null
    } else Adjustment(inputFile, stretchFactor, Duration.ZERO, targetDuration) to false
}