package com.github.mmauro94.media_merger.util.progress

data class Progress(
    val ratio: Float?,
    val discreteProgress: Pair<Int, Int>? = null
) {

    init {
        require(ratio == null || ratio in 0.0..1.0)
        if (discreteProgress != null) {
            require(discreteProgress.first >= 0)
            require(discreteProgress.second >= 0)
            require(discreteProgress.first <= discreteProgress.second)
            //TODO better check
        }
    }

    companion object {
        val INDETERMINATE = Progress(null, null)

        fun of(index: Int, max: Int): Progress {
            return Progress(
                ratio = if (max == 0) 1f else index / max.toFloat(),
                discreteProgress = index to max
            )
        }
    }
}

data class ProgressWithMessage(val progress: Progress, val message: String?)