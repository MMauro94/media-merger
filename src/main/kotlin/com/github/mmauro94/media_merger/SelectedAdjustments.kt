package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.cuts.Cuts
import com.github.mmauro94.media_merger.cuts.computeCuts
import com.github.mmauro94.media_merger.strategy.AdjustmentStrategies
import com.github.mmauro94.media_merger.strategy.CutsAdjustmentStrategy.*
import com.github.mmauro94.media_merger.video_part.VideoPartsMatchException
import com.github.mmauro94.media_merger.video_part.matchFirstSceneOffset
import com.github.mmauro94.media_merger.video_part.matchWithTarget

/**
 * @param inputFile the file to adjust
 * @param stretchFactor the stretch factor of the audio file
 * @param cuts the cuts to apply AFTER the audio has been stretched
 */
data class SelectedAdjustments(
    val inputFile: InputFile,
    val stretchFactor: StretchFactor,
    val cuts: Cuts
) {
    fun isEmpty() = stretchFactor.isEmpty() && cuts.isEmptyOffset()

    companion object {
        fun empty(inputFile: InputFile) = SelectedAdjustments(
            inputFile,
            StretchFactor.NONE,
            Cuts(emptyList())
        )
    }
}

fun selectAdjustments(
    adjustmentStrategies: AdjustmentStrategies,
    inputFile: InputFile,
    targetFile: InputFile
): SelectedAdjustments {
    val stretchFactor = StretchFactor.detect(adjustmentStrategies.stretch, inputFile, targetFile)
        ?: throw OperationCreationException("Unable to detect stretch factor")

    val cuts = when (adjustmentStrategies.cuts) {
        NONE -> Cuts.EMPTY
        FIRST_SCENE_OFFSET -> {
            val inputVideoParts = inputFile.videoParts?.lazy()?.times(stretchFactor)
                ?: throw OperationCreationException("No black segments in file $inputFile")
            val targetVideoParts = targetFile.videoParts?.lazy()
                ?: throw OperationCreationException("No black segments in file $targetFile")

            val offset = inputVideoParts.matchFirstSceneOffset(targetVideoParts)
                ?: throw OperationCreationException("Unable to detect matching first scene", StringBuilder().apply {
                    appendln("TARGET VIDEO PARTS (not complete) ($targetFile):")
                    appendln(targetVideoParts.readOnly().joinToString(separator = "\n"))
                    appendln()
                    appendln("INPUT VIDEO PARTS (not complete), ALREADY STRETCHED BY $stretchFactor ($inputFile):")
                    appendln(inputVideoParts.readOnly().joinToString(separator = "\n"))
                }.toString())
            Cuts.ofOffset(offset)
        }
        SCENES -> {
            val all = inputFile.videoParts?.all()
            val inputVideoParts = all?.times(stretchFactor)
                ?: throw OperationCreationException("No black segments in file $inputFile")
            val targetVideoParts = targetFile.videoParts?.all()
                ?: throw OperationCreationException("No black segments in file $targetFile")

            try {
                val (matches, accuracy) = inputVideoParts.matchWithTarget(targetVideoParts)
                if (accuracy.accuracy < 95) {
                    throw VideoPartsMatchException(
                        "Accuracy too low (${accuracy.accuracy}%)",
                        inputVideoParts.toList(),
                        targetVideoParts.toList()
                    )
                } else matches.computeCuts()
            } catch (e: VideoPartsMatchException) {
                throw OperationCreationException("Unable to match scenes: ${e.message}", StringBuilder().apply {
                    appendln("TARGET VIDEO PARTS ($targetFile):")
                    appendln(e.targets.joinToString(separator = "\n"))
                    appendln()
                    appendln("INPUT VIDEO PARTS, ALREADY STRETCHED BY $stretchFactor ($inputFile):")
                    appendln(e.input.joinToString(separator = "\n"))
                }.toString(), e)
            }
        }
    }

    return SelectedAdjustments(inputFile, stretchFactor, cuts)
}