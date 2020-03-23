package com.github.mmauro94.media_merger.strategy

import com.github.mmauro94.media_merger.util.CliDescriptor
import com.github.mmauro94.media_merger.util.askDouble
import jdk.jfr.Percentage

sealed class CutsAdjustmentStrategy(val detectProgressSplit : Float) {

    abstract override fun toString() : String

    /**
     * No cuts will be done
     */
    @CliDescriptor("No cuts")
    object None : CutsAdjustmentStrategy(0f) {
        override fun toString() = "None"
    }

    /**
     * The tracks will have an offset calculated using the first scene black fragments of the videos
     */
    @CliDescriptor("Adjust offset using first black segment")
    object FirstBlackSegmentOffset : CutsAdjustmentStrategy(.4f) {
        override fun toString() = "First black segment offset"
    }

    /**
     * The tracks will have an offset calculated using the matched first scene of the videos, detected using black frames
     */
    @CliDescriptor("Adjust offset using first matching scene")
    object FirstSceneOffset : CutsAdjustmentStrategy(.6f){
        override fun toString() = "First scene offset"
    }

    /**
     * The scenes will be detected using black frames, cutting the tracks appropriately.
     */
    @CliDescriptor("Cut by matching scenes")
    data class CutScenes(val minimumAccuracy: Double) : CutsAdjustmentStrategy(.8f) {

        override fun toString() = "Cut scenes with $minimumAccuracy% accuracy"

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

