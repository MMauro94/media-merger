package com.github.mmauro94.media_merger

enum class MergeMode(val description : String) {
    /**
     * No adjustments will be done, the tracks will be merged as is
     */
    NO_ADJUSTMENTS("No adjustments"),

    /**
     * The tracks will be stretched to match the target framerate
     */
    ADJUST_STRETCH("Adjust stretch only"),

    /**
     * The tracks will be stretched to match the target framerate,
     * and an offset will be calculated using the first black frames of the videos
     */
    ADJUST_STRETCH_AND_OFFSET("Adjust stretch and offset using black frames"),

    /**
     * The tracks will be stretched to match the target framerate,
     * and all the black frames will be analyzed and matched, cutting the tracks appropriately.
     */
    ADJUST_STRETCH_AND_CUT("Adjust stretch and cut using black frames")
}