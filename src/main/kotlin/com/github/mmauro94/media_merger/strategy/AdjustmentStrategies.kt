package com.github.mmauro94.media_merger.strategy

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.util.selectEnum
import com.github.mmauro94.media_merger.util.selectSealedClass

data class AdjustmentStrategies(
    val linearDrift: LinearDriftAdjustmentStrategy,
    val cuts: CutsAdjustmentStrategy
) {

    val detectProgressWeight = cuts.detectProgressSplit

    companion object {
        fun ask(): AdjustmentStrategies {
            return AdjustmentStrategies(
                linearDrift = selectSealedClass(
                    question = "Select linear drift strategy:",
                    long = true
                ),
                cuts = selectSealedClass(
                    question = "Select cuts adjustment strategy:",
                    long = true
                )
            )
        }
    }
}