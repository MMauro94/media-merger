package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.cuts.Cuts
import com.github.mmauro94.media_merger.strategy.AdjustmentStrategies
import com.github.mmauro94.media_merger.util.Reporter

/**
 * @param inputFile the file to adjust
 * @param linearDrift the linear drift of the audio file
 * @param cuts the cuts to apply AFTER the audio has been stretched
 */
data class SelectedAdjustments(
    val inputFile: InputFile,
    val linearDrift: LinearDrift,
    val cuts: Cuts
) {
    fun isEmpty() = linearDrift.isNone() && cuts.isEmptyOffset()

    companion object {
        fun empty(inputFile: InputFile) = SelectedAdjustments(
            inputFile,
            LinearDrift.NONE,
            Cuts(emptyList())
        )
    }
}

fun selectAdjustments(
    adjustmentStrategies: AdjustmentStrategies,
    inputFile: InputFile,
    targetFile: InputFile,
    reporter: Reporter
): SelectedAdjustments {
    reporter.log.debug("--- ADJUSTMENT DETECTION ---")
    reporter.log.debug("Input file: ${inputFile.file.absolutePath}")
    reporter.log.debug("Target file: ${targetFile.file.absolutePath}")
    reporter.log.debug()

    reporter.progress.ratio(0f, "Detecting linear drift...")
    val linearDrift = adjustmentStrategies.linearDrift.detect(reporter.log, inputFile, targetFile) ?: throw AdjustmentDetectionImpossible()
    reporter.log.debug("Detected linear drift: $linearDrift")

    reporter.log.debug()
    val cutsReporter = reporter.split(.1f, 1f, "Detecting cuts...")
    val cuts = adjustmentStrategies.cuts.detect(cutsReporter, linearDrift, inputFile, targetFile) ?: throw AdjustmentDetectionImpossible()

    reporter.log.debug()
    reporter.progress.finished("Adjustments detected for file ${inputFile.file.name}")
    return SelectedAdjustments(inputFile, linearDrift, cuts)
}