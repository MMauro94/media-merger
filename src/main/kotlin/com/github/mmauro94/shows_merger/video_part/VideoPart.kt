package com.github.mmauro94.shows_merger.video_part

import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.makeMillisPrecision
import com.github.mmauro94.shows_merger.requireMillisPrecision
import com.github.mmauro94.shows_merger.times
import java.time.Duration

/**
 * Represents a part of video that can be either a black segment or a scene.
 * @param start the start of the part
 * @param end the end of the part
 * @param type the type of the part
 */
data class VideoPart(val start: Duration, val end: Duration, val type: Type) {

    /**
     * Type of [VideoPart]
     */
    enum class Type {
        BLACK_SEGMENT, SCENE;
    }

    init {
        start.requireMillisPrecision()
        end.requireMillisPrecision()

        require(start < end)
    }

    /**
     * The duration of this [VideoPart]
     */
    val duration: Duration = end - start

    /**
     * Commodity property that is half of [duration]
     */
    val halfDuration: Duration = duration.dividedBy(2L).makeMillisPrecision()

    /**
     * Commodity property that is the middle of the part
     */
    val middle: Duration = start + halfDuration
}

/**
 * Multiplies this [VideoPart] by the given [stretchFactor]
 *
 * @see Duration.times
 */
operator fun VideoPart.times(stretchFactor: StretchFactor): VideoPart {
    return this.copy(
        start = (start * stretchFactor).makeMillisPrecision(),
        end = (end * stretchFactor).makeMillisPrecision()
    )
}