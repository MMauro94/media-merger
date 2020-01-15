package com.github.mmauro94.shows_merger

import java.math.BigDecimal
import java.time.Duration

/**
 * @param inputFile the file to adjust
 * @param stretchFactor the stretch factor of the audio file
 * @param cuts the cuts to apply AFTER the audio has been stretched
 */
data class Adjustment(
    val inputFile: InputFile,
    val stretchFactor: StretchFactor,
    val cuts: Cuts
) {
    fun isEmpty() = stretchFactor.factor.compareTo(BigDecimal.ONE) == 0 && cuts.isEmpty()

    companion object {
        fun empty(inputFile: InputFile) = Adjustment(
            inputFile,
            StretchFactor(BigDecimal.ONE),
            Cuts(emptyList())
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

        val cuts = when (mergeMode) {
            MergeMode.ADJUST_STRETCH_AND_OFFSET -> {
                val inputFirstBlackSegment =
                    inputFile.videoPartsLimited?.blackSegments()?.firstOrNull()?.times(stretchFactor)
                val targetFirstBlackSegment = if (inputFirstBlackSegment != null) {
                    targetFile.videoPartsLimited?.blackSegments()?.take(2)?.find {
                        (inputFirstBlackSegment.duration - it.duration).abs() < Duration.ofMillis(100)
                    }
                } else null

                val offset = if (inputFirstBlackSegment != null && targetFirstBlackSegment != null) {
                    targetFirstBlackSegment.end - inputFirstBlackSegment.end
                } else {
                    needsCheck = true
                    Duration.ZERO
                }
                Cuts.ofOffset(offset)
            }
            MergeMode.ADJUST_STRETCH_AND_CUT -> {
                val inputBlackSegments = inputFile.videoParts?.times(stretchFactor)
                val targetBlackSegments = targetFile.videoParts

                if (inputBlackSegments != null && targetBlackSegments != null) {
                    inputBlackSegments.matchWithTarget(targetBlackSegments)?.computeCuts()
                } else null
            }
            else -> null
        }

        Adjustment(
            inputFile,
            stretchFactor,
            cuts ?: Cuts.empty()
        )
    }
    return adj to needsCheck
}