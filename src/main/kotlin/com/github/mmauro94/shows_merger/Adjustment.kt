package com.github.mmauro94.shows_merger

import com.github.mmauro94.shows_merger.cuts.Cuts
import com.github.mmauro94.shows_merger.cuts.computeCuts
import com.github.mmauro94.shows_merger.video_part.*
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
fun selectAdjustment(mergeMode: MergeMode, inputFile: InputFile, targetFile: InputFile): Pair<Adjustment, Boolean> {
    var needsCheck = false
    val adj = if (mergeMode == MergeMode.NO_ADJUSTMENTS) Adjustment.empty(inputFile)
    else {
        val (stretchFactor, stretchFromUser) = StretchFactor.detectOrAsk(inputFile, targetFile)
            ?: throw OperationCreationException("Unable to detect stretch factor")
        needsCheck = needsCheck || stretchFromUser

        val cuts = when (mergeMode) {
            MergeMode.ADJUST_STRETCH_AND_OFFSET -> {
                val inputVideoParts = inputFile.videoPartsLimited?.times(stretchFactor)
                    ?: throw OperationCreationException("No black segments in file $inputFile")
                val targetVideoParts = targetFile.videoPartsLimited
                    ?: throw OperationCreationException("No black segments in file $targetFile")

                val offset = inputVideoParts.matchFirstSceneOffset(targetVideoParts)
                    ?: throw OperationCreationException("Unable to detect matching first scene", StringBuilder().apply {
                        appendln("TARGET VIDEO PARTS (not complete) ($targetFile):")
                        appendln(targetVideoParts)
                        appendln()
                        appendln("INPUT VIDEO PARTS (not complete), ALREADY STRETCHED BY $stretchFactor ($inputFile):")
                        appendln(inputVideoParts)
                    }.toString())
                Cuts.ofOffset(offset)
            }
            MergeMode.ADJUST_STRETCH_AND_CUT -> {
                val inputVideoParts = inputFile.videoParts?.times(stretchFactor)
                    ?: throw OperationCreationException("No black segments in file $inputFile")
                val targetVideoParts = targetFile.videoParts
                    ?: throw OperationCreationException("No black segments in file $targetFile")

                try {
                    inputVideoParts.matchWithTarget(targetVideoParts)?.computeCuts()
                } catch (e: VideoPartsMatchException) {
                    throw OperationCreationException("Unable to match scenes: ${e.message}", StringBuilder().apply {
                        appendln("TARGET VIDEO PARTS ($targetFile):")
                        appendln(e.targets.toString())
                        appendln()
                        appendln("INPUT VIDEO PARTS, ALREADY STRETCHED BY $stretchFactor ($inputFile):")
                        appendln(e.input.toString())
                    }.toString(), e)
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