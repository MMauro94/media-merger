package com.github.mmauro94.shows_merger

import java.math.BigDecimal
import java.time.Duration

data class Adjustment(
    val inputFile: InputFile,
    val stretchFactor: StretchFactor,
    val offset: Duration
) {
    fun isEmpty() = stretchFactor.factor.compareTo(BigDecimal.ONE) == 0 && offset.isZero

    companion object {
        fun empty(inputFile: InputFile) = Adjustment(
            inputFile,
            StretchFactor(BigDecimal.ONE),
            Duration.ZERO
        )
    }
}

/**
 * @return Adjustment, needsCheck
 */
fun selectAdjustment(mergeMode: MergeMode, inputFile: InputFile, targetFile: InputFile): Pair<Adjustment, Boolean>? {
    var needsCheck = false
    val adj = if (mergeMode == MergeMode.NO_ADJUSTMENTS) Adjustment.empty(inputFile)
    else {
        val (stretchFactor, stretchFromUser) = detectOrAskStretchFactor(inputFile, targetFile) ?: return null
        needsCheck = needsCheck || stretchFromUser

        val offset = if (mergeMode >= MergeMode.ADJUST_STRETCH_AND_OFFSET) {
            val inputFirstBlackSegment = inputFile.blackSegments?.firstOrNull() * stretchFactor
            val targetFirstBlackSegment = if (inputFirstBlackSegment != null) {
                targetFile.blackSegments?.take(2)?.find {
                    (inputFirstBlackSegment.duration - it.duration).abs() < Duration.ofMillis(100)
                }
            } else null

            if (inputFirstBlackSegment != null && targetFirstBlackSegment != null) {
                targetFirstBlackSegment.end - inputFirstBlackSegment.end
            } else {
                needsCheck = true
                Duration.ZERO
            }
        } else Duration.ZERO

        Adjustment(
            inputFile,
            stretchFactor,
            offset
        )
    }
    return adj to needsCheck

    //val moveDuration = Duration.ofMillis(askInt("Select wanted offset (in ms): ").toLong())
}