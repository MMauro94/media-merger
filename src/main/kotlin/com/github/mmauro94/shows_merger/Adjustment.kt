package com.github.mmauro94.shows_merger

import java.math.BigDecimal
import java.time.Duration

data class Adjustment(
    val inputFile: InputFile,
    val stretchFactor: StretchFactor,
    val offset: Duration
)

/**
 * @return Adjustment, needsCheck
 */
fun selectAdjustment(inputFile: InputFile, targetFile: InputFile): Pair<Adjustment?, Boolean>? {
    val (stretchFactor, stretchFromUser) = detectOrAskStretchFactor(inputFile, targetFile) ?: return null
    var needsCheck = stretchFromUser

    println("Selected stretch factor: $stretchFactor")

    val inputFirstBlackSegment = inputFile.blackSegments?.firstOrNull() * stretchFactor
    val targetFirstBlackSegment = if (inputFirstBlackSegment != null) {
         targetFile.blackSegments?.take(2)?.find {
            (inputFirstBlackSegment.duration - it.duration).abs() < Duration.ofMillis(100)
        }
    } else null

    val offset = if (inputFirstBlackSegment != null && targetFirstBlackSegment != null) {
        targetFirstBlackSegment.end - inputFirstBlackSegment.end
    } else {
        needsCheck = true
        Duration.ZERO
    }

    println("Selected offset: ${offset.humanStr()}")

    val adj = if(stretchFactor.factor == BigDecimal.ONE && offset.isZero) null
    else Adjustment(
        inputFile,
        stretchFactor,
        offset
    )
    return adj to needsCheck

    //val moveDuration = Duration.ofMillis(askInt("Select wanted offset (in ms): ").toLong())
}