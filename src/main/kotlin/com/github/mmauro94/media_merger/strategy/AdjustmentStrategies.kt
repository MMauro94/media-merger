package com.github.mmauro94.media_merger.strategy

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.util.selectEnum
import com.github.mmauro94.media_merger.util.selectSealedClass

data class AdjustmentStrategies(
    val stretch: StretchAdjustmentStrategy,
    val cuts: CutsAdjustmentStrategy
) {
    companion object {
        fun ask(): AdjustmentStrategies {
            return AdjustmentStrategies(
                stretch = selectEnum(
                    question = "Select stretch adjustment strategy:",
                    defaultValue = Main.config.defaultStretchAdjustmentStrategy,
                    long = true,
                    nameProvider = { it.description }
                ),
                cuts = selectSealedClass(
                    question = "Select cuts adjustment strategy:",
                    long = true
                )
            )
        }
    }
}