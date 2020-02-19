package com.github.mmauro94.media_merger.strategy

import com.github.mmauro94.media_merger.KNOWN_STRETCH_FACTORS

/**
 * Different strategies to detect the stretch adjustment to be applied to a given file
 */
enum class StretchAdjustmentStrategy(val description : String) {
    /**
     * The stretch is never adjusted. Use it if you are sure all the files don't have any linear drift.
     */
    NONE("None"),

    /**
     * The stretch is the one closer to 1 (none) or one of the [KNOWN_STRETCH_FACTORS].
     * File framerate is used first; if unavailable duration is used.
     */
    KNOWN_ONLY("Known only"),

    /**
     * The stretch is calculated precisely using the framerate, if available.
     * If framerate is unavailable, duration is used, but like with the [KNOWN_ONLY] option.
     */
    PRECISE("Precise");

}