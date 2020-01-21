package com.github.mmauro94.shows_merger.video_part

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.util.DurationSpan

/**
 * Represents a part of video that can be either a black segment or a scene.
 * @param start the start of the part
 * @param end the end of the part
 * @param type the type of the part
 */
data class VideoPart(val time: DurationSpan, val type: Type) {

    /**
     * Type of [VideoPart]
     */
    enum class Type {
        BLACK_SEGMENT, SCENE;
    }


    override fun toString(): String {
        val type = when (type) {
            Type.BLACK_SEGMENT -> "BlackSegment"
            Type.SCENE -> "Scene"
        }.padEnd(15)

        val start = time.start.toString().padEnd(15)
        val end = time.end.toString().padEnd(15)
        val duration = time.duration.toString().padEnd(15)
        return type + "start=$start end=$end duration=$duration"
    }
}

/**
 * Multiplies this [VideoPart] by the given [stretchFactor]
 */
operator fun VideoPart.times(stretchFactor: StretchFactor): VideoPart {
    return this.copy(time = time * stretchFactor)
}