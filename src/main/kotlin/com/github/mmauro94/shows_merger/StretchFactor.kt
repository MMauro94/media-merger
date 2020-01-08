package com.github.mmauro94.shows_merger

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import kotlin.math.roundToLong

private val FPS_25 = BigDecimal("25")
private val FPS_23_9 = BigDecimal("23.976215")

private val KNOWN_STRETCH_FACTORS = arrayOf(
    StretchFactor(FPS_25.divide(FPS_23_9, 3, RoundingMode.HALF_UP), "25 fps to 23.976 fps"),
    StretchFactor(FPS_23_9.divide(FPS_25, 3, RoundingMode.HALF_UP), "23.976 fps to 25 fps")
)

class StretchFactor(val factor: BigDecimal, val name: String? = null) {

    val ratio = BigDecimal.ONE.divide(factor, 3, RoundingMode.HALF_UP)!!

    override fun hashCode() = factor.hashCode()
    override fun equals(other: Any?) = other is StretchFactor && factor == other.factor

    override fun toString(): String {
        return name ?: "Stretch factor $factor"
    }

    @JvmName("resultingDurationForStretchFactorNotNull")
    fun resultingDurationForStretchFactor(duration: Duration): Duration {
        return Duration.ofNanos((duration.toNanos() * factor.toDouble()).roundToLong())
    }

    fun resultingDurationForStretchFactor(duration: Duration?): Duration? {
        return if (duration == null) null else resultingDurationForStretchFactor(duration)
    }

}

fun detectStretchFactorFromDuration(duration: Duration?, targetDuration: Duration?): StretchFactor? {
    if (duration != null && targetDuration != null) {
        KNOWN_STRETCH_FACTORS.forEach { sf ->
            if (MergeOptions.isDurationValid(sf.resultingDurationForStretchFactor(duration), targetDuration)) {
                return sf
            }
        }
    }
    return null
}

fun detectStretchFactorFromFramerate(framerate: Framerate?, targetFramerate: Framerate?): StretchFactor? {
    return if (framerate != null && targetFramerate != null) {
        StretchFactor(
            factor = framerate.framerate.divide(targetFramerate.framerate, 6, RoundingMode.HALF_UP),
            name = "$framerate to $targetFramerate"
        )
    } else null
}

fun detectOrAskStretchFactor(inputFile: InputFile, targetFile: InputFile): Pair<StretchFactor, Boolean>? {
    val fromFramerate = detectStretchFactorFromFramerate(inputFile.framerate, targetFile.framerate)
    if(fromFramerate != null) return fromFramerate to false

    val fromDuration = detectStretchFactorFromDuration(inputFile.duration, targetFile.duration)
    if(fromDuration != null) return fromDuration to false

    val fromUser = askStretchFactor(inputFile, targetFile)
    return if(fromUser != null) fromUser to true
    else null
}

fun askStretchFactor(inputFile: InputFile, targetFile: InputFile): StretchFactor? {
    val originalDuration = inputFile.duration
    val targetDuration = targetFile.duration

    val possibleOutcomes = KNOWN_STRETCH_FACTORS.joinToString(", ") { sf ->
        if (originalDuration == null) "Unknown" else sf.resultingDurationForStretchFactor(originalDuration).humanStr()
    }

    println("$inputFile cannot be adjusted because of unknown duration ratio (duration=${originalDuration.humanStr()}, target=${targetDuration.humanStr()}, possibleOutcomes=$possibleOutcomes)")
    val adjustAnyway = askYesNo("Adjust anyway with custom parameters?", false)
    return if (adjustAnyway) {
        println("0) No adjustment (${originalDuration.humanStr()})")
        KNOWN_STRETCH_FACTORS.forEachIndexed { i, sf ->
            val resultingDuration = sf.resultingDurationForStretchFactor(originalDuration)
            println("${i + 1}) ${sf.name} (resulting duration: ${resultingDuration.humanStr()})")
        }
        val stretchSelection = askInt(
            "Select wanted resulting duration: ",
            0,
            KNOWN_STRETCH_FACTORS.size
        )
        if (stretchSelection == 0) StretchFactor(BigDecimal.ONE)
        else KNOWN_STRETCH_FACTORS[stretchSelection - 1]
    } else null
}