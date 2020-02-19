package com.github.mmauro94.media_merger.strategy

enum class CutsAdjustmentStrategy(val description : String) {
    /**
     * No cuts will be done
     */
    NONE("No cuts"),

    /**
     * The tracks will be offset by a value calculated using the first black frames of the videos
     */
    OFFSET("Adjust offset using black frames"),

    /**
     * The scenes will be detected using black frames, cutting the tracks appropriately.
     */
    SCENES("Cut scenes using black frames")
}