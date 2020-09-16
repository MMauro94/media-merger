package com.github.mmauro94.media_merger.strategy

import com.github.mmauro94.media_merger.InputFile
import com.github.mmauro94.media_merger.LinearDrift
import com.github.mmauro94.media_merger.config.FFMpegBlackdetectConfig
import com.github.mmauro94.media_merger.cuts.Cuts
import com.github.mmauro94.media_merger.cuts.computeCuts
import com.github.mmauro94.media_merger.ui.CutsSelectionFrame
import com.github.mmauro94.media_merger.util.CliDescriptor
import com.github.mmauro94.media_merger.util.Reporter
import com.github.mmauro94.media_merger.util.cli.type.DoubleCliType
import com.github.mmauro94.media_merger.util.toTimeString
import com.github.mmauro94.media_merger.video_part.*
import java.time.Duration
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.Lock

sealed class CutsAdjustmentStrategy(val detectProgressSplit: Float) {

    abstract fun detect(reporter: Reporter, linearDrift: LinearDrift, inputFile: InputFile, targetFile: InputFile): Cuts?
    abstract override fun toString(): String

    protected fun detect(
        blackdetectConfig: FFMpegBlackdetectConfig,
        reporter: Reporter,
        lazy: Boolean,
        linearDrift: LinearDrift,
        inputFile: InputFile,
        targetFile: InputFile,
        block: (inputVideoParts: VideoParts, targetVideoParts: VideoParts) -> Cuts?
    ): Cuts? {
        class NoBlackSegments() : Exception()

        val getInputParts: (Duration) -> VideoParts = {
            (inputFile.videoParts(blackdetectConfig.thresholds, it)
                ?: throw NoBlackSegments().also { reporter.log.debug("No black segments in input file") })
                .get(lazy, reporter.split(0, 2, "Detecting ${inputFile.file.name} black segments..."))
                .times(linearDrift)
        }
        val getTargetParts: (Duration) -> VideoParts = {
            (targetFile.videoParts(blackdetectConfig.thresholds, it)
                ?: throw NoBlackSegments().also { reporter.log.debug("No black segments in target file") })
                .get(lazy, reporter.split(1, 2, "Detecting ${targetFile.file.name} black segments..."))
        }

        try {
            if (blackdetectConfig.minDuration == null) {
                val semaphore = Semaphore(0)
                var cuts : Cuts? = null
                CutsSelectionFrame(
                    inputVideoPartsProvider = getInputParts,
                    targetVideoPartsProvider = getTargetParts,
                    onSelected = {
                        cuts = it
                        semaphore.release()
                    }
                ).apply {
                    isVisible = true
                }
                semaphore.acquire()
                return cuts
            } else {
                val inputVideoParts = getInputParts(blackdetectConfig.minDuration)
                val targetVideoParts = getInputParts(blackdetectConfig.minDuration)

                return block(inputVideoParts, targetVideoParts).also {
                    reporter.log.debug()
                    reporter.log.debug("TARGET VIDEO PARTS " + (if (lazy) "(Not complete)" else "") + " ($targetFile):")
                    reporter.log.debug(targetVideoParts.readOnly().joinToString(separator = "\n"))
                    reporter.log.debug()
                    reporter.log.debug("INPUT VIDEO PARTS " + (if (lazy) "(Not complete)" else "") + ", ALREADY STRETCHED BY $linearDrift ($inputFile):")
                    reporter.log.debug(inputVideoParts.readOnly().joinToString(separator = "\n"))
                }
            }
        } catch (nbs: NoBlackSegments) {
            return null
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
    data class FirstBlackSegmentOffset(val blackdetectConfig: FFMpegBlackdetectConfig) : CutsAdjustmentStrategy(.4f) {
        override fun toString() = "First black segment offset"

        override fun detect(reporter: Reporter, linearDrift: LinearDrift, inputFile: InputFile, targetFile: InputFile): Cuts? {
            return detect(blackdetectConfig, reporter, true, linearDrift, inputFile, targetFile) { inputVideoParts, targetVideoParts ->
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

        companion object {
            @Suppress("unused")
            fun ask() = FirstBlackSegmentOffset(FFMpegBlackdetectConfig.ask())
        }
    }

    /**
     * The tracks will have an offset calculated using the matched first scene of the videos, detected using black frames
     */
    @CliDescriptor("Adjust offset using first matching scene")
    @Suppress("unused")
    data class FirstSceneOffset(val blackdetectConfig: FFMpegBlackdetectConfig) : CutsAdjustmentStrategy(.6f) {
        override fun toString() = "First scene offset"
        override fun detect(reporter: Reporter, linearDrift: LinearDrift, inputFile: InputFile, targetFile: InputFile): Cuts? {
            return detect(blackdetectConfig, reporter, true, linearDrift, inputFile, targetFile) { inputVideoParts, targetVideoParts ->
                val offset = inputVideoParts.matchFirstSceneOffset(targetVideoParts)
                if (offset == null) {
                    reporter.log.debug("Unable to detect matching first scene")
                    null
                } else {
                    Cuts.ofOffset(offset)
                }
            }
        }

        companion object {
            @Suppress("unused")
            fun ask() = FirstSceneOffset(FFMpegBlackdetectConfig.ask())
        }
    }

    /**
     * The scenes will be detected using black frames, cutting the tracks appropriately.
     */
    @CliDescriptor("Cut by matching scenes")
    @Suppress("unused")
    data class CutScenes(val blackdetectConfig: FFMpegBlackdetectConfig, val minimumAccuracy: Double) : CutsAdjustmentStrategy(.8f) {

        override fun toString() = "Cut scenes with $minimumAccuracy% accuracy"

        override fun detect(reporter: Reporter, linearDrift: LinearDrift, inputFile: InputFile, targetFile: InputFile): Cuts? {
            return detect(blackdetectConfig, reporter, false, linearDrift, inputFile, targetFile) { inputVideoParts, targetVideoParts ->
                try {
                    val (matches, accuracy) = inputVideoParts.matchWithTarget(targetVideoParts)
                    reporter.log.debug("Detected matches:")
                    reporter.log.prepend("   ").apply {
                        val maxInput = matches.map { it.input.toString().length }.maxOrNull()
                        for (match in matches) {
                            debug("INPUT=" + match.input.toString().padEnd(maxInput!! + 1) + "| TARGET=" + match.target.toString())
                        }
                    }
                    reporter.log.debug("Accuracy: ${accuracy.accuracy}")

                    if (accuracy.accuracy < minimumAccuracy) {
                        reporter.log.debug("Accuracy too low! (threshold $minimumAccuracy)")
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
                    blackdetectConfig = FFMpegBlackdetectConfig.ask(),
                    minimumAccuracy = DoubleCliType.ask(
                        question = "Select minimum accuracy",
                        isValid = { this in 0.0..100.0 },
                        default = 95.0
                    )
                )
            }
        }
    }
}

