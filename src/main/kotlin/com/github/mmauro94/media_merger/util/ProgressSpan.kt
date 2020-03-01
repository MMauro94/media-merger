package com.github.mmauro94.media_merger.util

class ProgressSpan(val start: Progress, val end: Progress) {
    init {
        if (start.ratio != null && end.ratio != null) {
            require(start.ratio <= end.ratio)
        }
        require((start.ratio == null) == (end.ratio == null))
    }

    fun interpolate(interpolation: Float): Progress {
        require(interpolation in 0f..1f)
        return if (start.ratio != null && end.ratio != null) {
            val discreteProgress =
                if (start.discreteProgress != null && end.discreteProgress != null && start.discreteProgress.second == end.discreteProgress.second) {
                    val startIndex = start.discreteProgress.first
                    val endIndex = end.discreteProgress.first
                    (startIndex + (endIndex - startIndex) * interpolation).toInt() to start.discreteProgress.second
                } else null

            Progress(
                ratio = start.ratio + (end.ratio - start.ratio) * interpolation,
                discreteProgress = discreteProgress
            )
        } else start
    }
}
