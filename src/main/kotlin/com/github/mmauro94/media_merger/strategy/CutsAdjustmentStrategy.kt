package com.github.mmauro94.media_merger.strategy

import com.github.mmauro94.media_merger.InputFile
import com.github.mmauro94.media_merger.LinearDrift
import com.github.mmauro94.media_merger.cuts.Cuts
import com.github.mmauro94.media_merger.cuts.computeCuts
import com.github.mmauro94.media_merger.util.CliDescriptor
import com.github.mmauro94.media_merger.util.Reporter
import com.github.mmauro94.media_merger.util.askDouble
import com.github.mmauro94.media_merger.util.toTimeString
import com.github.mmauro94.media_merger.video_part.*
import java.time.Duration

sealed class CutsAdjustmentStrategy(val detectProgressSplit: Float) {

    abstract fun detect(reporter: Reporter, linearDrift: LinearDrift, inputFile: InputFile, targetFile: InputFile): Cuts?
    abstract override fun toString(): String

    protected fun detect(
        reporter: Reporter,
        lazy: Boolean,
        linearDrift: LinearDrift,
        inputFile: InputFile,
        targetFile: InputFile,
        block: (inputVideoParts: VideoParts, targetVideoParts: VideoParts) -> Cuts?
    ): Cuts? {
        val inputVideoParts = (inputFile.videoParts ?: return null.also { reporter.log.debug("No black segments in input file") })
            .get(lazy, reporter.split(0, 2, "Detecting ${inputFile.file.name} first black segment..."))
            .times(linearDrift)

        val targetVideoParts = (targetFile.videoParts ?: return null.also { reporter.log.debug("No black segments in target file") })
            .get(lazy, reporter.split(1, 2, "Detecting ${targetFile.file.name} first black segment..."))

        return block(inputVideoParts, targetVideoParts).also {
            reporter.log.debug()
            reporter.log.debug("TARGET VIDEO PARTS (not complete) ($targetFile):")
            reporter.log.debug(targetVideoParts.readOnly().joinToString(separator = "\n"))
            reporter.log.debug()
            reporter.log.debug("INPUT VIDEO PARTS (not complete), ALREADY STRETCHED BY $linearDrift ($inputFile):")
            reporter.log.debug(inputVideoParts.readOnly().joinToString(separator = "\n"))
        }
    }

    /**
     * No cuts will be done
     */
    @CliDescriptor("No cuts")
    @Suppress("unused")
    object None : CutsAdjustmentStrategy(0f) {
        override fun toString() = "None"

        override fun detect(reporter: Reporter, linearDrift: LinearDrift, inputFile: InputFile, targetFile: InputFile) = Cuts.EMPTY
    }

    /**
     * The tracks will have an offset calculated using the first scene black fragments of the videos
     */
    @CliDescriptor("Adjust offset using first black segment")
    @Suppress("unused")
    object FirstBlackSegmentOffset : CutsAdjustmentStrategy(.4f) {
        override fun toString() = "First black segment offset"

        override fun detect(reporter: Reporter, linearDrift: LinearDrift, inputFile: InputFile, targetFile: InputFile): Cuts? {
            return detect(reporter, true, linearDrift, inputFile, targetFile) { inputVideoParts, targetVideoParts ->
                val firstInputPart = inputVideoParts.first()
                val firstTargetPart = targetVideoParts.first()

                val inputFirstSceneOffset = if (firstInputPart.type == VideoPart.Type.SCENE) Duration.ZERO!! else firstInputPart.time.duration
                val targetFirstSceneOffset = if (firstTargetPart.type == VideoPart.Type.SCENE) Duration.ZERO!! else firstTargetPart.time.duration
                val offset = targetFirstSceneOffset - inputFirstSceneOffset

                reporter.log.debug("Input file first non-black frame offset: ${inputFirstSceneOffset.toTimeString()}")
                reporter.log.debug("Target file first non-black frame offset: ${targetFirstSceneOffset.toTimeString()}")
                reporter.log.debug("Resulting offset: ${offset.toTimeString()}")

                Cuts.ofOffset(offset)
            }
        }
    }

    /**
     * The tracks will have an offset calculated using the matched first scene of the videos, detected using black frames
     */
    @CliDescriptor("Adjust offset using first matching scene")
    @Suppress("unused")
    object FirstSceneOffset : CutsAdjustmentStrategy(.6f) {
        override fun toString() = "First scene offset"
        override fun detect(reporter: Reporter, linearDrift: LinearDrift, inputFile: InputFile, targetFile: InputFile): Cuts? {
            return detect(reporter, true, linearDrift, inputFile, targetFile) { inputVideoParts, targetVideoParts ->
                val offset = inputVideoParts.matchFirstSceneOffset(targetVideoParts)
                if (offset == null) {
                    reporter.log.debug("Unable to detect matching first scene")
                    null
                } else {
                    Cuts.ofOffset(offset)
                }
            }
        }
    }

    /**
     * The scenes will be detected using black frames, cutting the tracks appropriately.
     */
    @CliDescriptor("Cut by matching scenes")
    @Suppress("unused")
    data class CutScenes(val minimumAccuracy: Double) : CutsAdjustmentStrategy(.8f) {

        override fun toString() = "Cut scenes with $minimumAccuracy% accuracy"

        override fun detect(reporter: Reporter, linearDrift: LinearDrift, inputFile: InputFile, targetFile: InputFile): Cuts? {
            return detect(reporter, false, linearDrift, inputFile, targetFile) { inputVideoParts, targetVideoParts ->
                try {
                    val (matches, accuracy) = inputVideoParts.matchWithTarget(targetVideoParts)
                    reporter.log.debug("Detected matches:")
                    reporter.log.prepend("   ").apply {
                        for (match in matches) {
                            debug("INPUT=" + match.input.toString().padEnd(50) + "| TARGET=" + match.target.toString())
                        }
                    }
                    reporter.log.debug("Accuracy: ${accuracy.accuracy}")

                    if (accuracy.accuracy < minimumAccuracy) {
                        reporter.log.debug("Accuracy too low! (threshold $minimumAccuracy")
                        null
                    } else matches.computeCuts()
                } catch (e: VideoPartsMatchException) {
                    reporter.log.debug("Unable to match scenes: ${e.message}")
                    null
                }
            }
        }

        companion object {
            @Suppress("unused")
            fun ask(): CutScenes {
                return CutScenes(
                    askDouble(
                        question = "Select minimum accuracy",
                        isValid = { this in 0.0..100.0 },
                        default = 95.0
                    )
                )
            }
        }
    }
}

