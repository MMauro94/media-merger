package com.github.mmauro94.shows_merger

import com.github.mmauro94.shows_merger.cuts.Cuts
import com.github.mmauro94.shows_merger.cuts.computeCuts
import com.github.mmauro94.shows_merger.video_part.matchWithTarget
import com.github.mmauro94.shows_merger.video_part.times
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
    fun isEmpty() = stretchFactor.isEmpty() && cuts.isEmptyOffset()

    companion object {
        fun empty(inputFile: InputFile) = Adjustment(
            inputFile,
            StretchFactor.EMPTY,
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
        val (stretchFactor, stretchFromUser) = StretchFactor.detectOrAsk(inputFile, targetFile) ?: return null
        needsCheck = needsCheck || stretchFromUser

        val cuts = when (mergeMode) {
            MergeMode.ADJUST_STRETCH_AND_OFFSET -> {
                //TODO better algorithm
                val inputFirstBlackSegment =
                    inputFile.videoPartsLimited?.blackSegments?.firstOrNull()?.times(stretchFactor)
                val targetFirstBlackSegment = if (inputFirstBlackSegment != null) {
                    targetFile.videoPartsLimited?.blackSegments?.take(2)?.find {
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

                val cuts = if (inputBlackSegments != null && targetBlackSegments != null) {
                    targetBlackSegments.println()
                    println()
                    inputBlackSegments.println()
                    inputBlackSegments.matchWithTarget(targetBlackSegments)?.computeCuts()
                } else null
                if (cuts != null) cuts else {
                    System.err.println("Unable to automatically detect cuts for file $inputFile")
                    return null
                }
            }
            else -> null
        }

        Adjustment(
            inputFile,
            stretchFactor,
            cuts ?: Cuts.EMPTY
        )
    }
    return adj to needsCheck
}