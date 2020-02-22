package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.cuts.Cuts
import com.github.mmauro94.media_merger.cuts.computeCuts
import com.github.mmauro94.media_merger.strategy.AdjustmentStrategies
import com.github.mmauro94.media_merger.strategy.CutsAdjustmentStrategy.*
import com.github.mmauro94.media_merger.video_part.VideoPart.Type.SCENE
import com.github.mmauro94.media_merger.video_part.VideoPartsMatchException
import com.github.mmauro94.media_merger.video_part.matchFirstSceneOffset
import com.github.mmauro94.media_merger.video_part.matchWithTarget
import java.time.Duration

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
        is None -> Cuts.EMPTY
        is FirstBlackSegmentOffset -> {
            val inputVideoParts = inputFile.videoParts?.lazy()?.times(stretchFactor)
                ?: throw OperationCreationException("No black segments in file $inputFile")
            val targetVideoParts = targetFile.videoParts?.lazy()
                ?: throw OperationCreationException("No black segments in file $targetFile")

            val firstInputPart = inputVideoParts.first()
            val firstTargetPart = targetVideoParts.first()

            val inputFirstSceneOffset = if (firstInputPart.type == SCENE) Duration.ZERO!! else firstInputPart.time.duration
            val targetFirstSceneOffset = if (firstTargetPart.type == SCENE) Duration.ZERO!! else firstTargetPart.time.duration

            Cuts.ofOffset(targetFirstSceneOffset - inputFirstSceneOffset)
        }
        is FirstSceneOffset -> {
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
        is CutScenes -> {
            val all = inputFile.videoParts?.all()
            val inputVideoParts = all?.times(stretchFactor)
                ?: throw OperationCreationException("No black segments in file $inputFile")
            val targetVideoParts = targetFile.videoParts?.all()
                ?: throw OperationCreationException("No black segments in file $targetFile")

            try {
                val (matches, accuracy) = inputVideoParts.matchWithTarget(targetVideoParts)
                if (accuracy.accuracy < adjustmentStrategies.cuts.minimumAccuracy) {
                    throw VideoPartsMatchException(
                        "Accuracy too low (${accuracy.accuracy}%)",
                        inputVideoParts.toList(),
                        targetVideoParts.toList(),
                        StringBuilder().apply {
                            appendln("DETECTED MATCHES:")
                            matches.joinTo(
                                buffer = this,
                                separator = "\n",
                                transform = {
                                    "INPUT=" + it.input.toString().padEnd(50) + "| TARGET=" + it.target.toString()
                                }
                            )
                        }.toString()
                    )
                } else matches.computeCuts()
            } catch (e: VideoPartsMatchException) {
                throw OperationCreationException("Unable to match scenes: ${e.message}", e.text(inputFile, targetFile, stretchFactor), e)
            }
        }
    }

    return SelectedAdjustments(inputFile, stretchFactor, cuts)
}