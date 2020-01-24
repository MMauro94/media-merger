package com.github.mmauro94.shows_merger.video_part

import com.github.mmauro94.shows_merger.util.humanStr
import com.github.mmauro94.shows_merger.util.sum
import com.github.mmauro94.shows_merger.util.DurationSpan
import com.github.mmauro94.shows_merger.video_part.VideoPart.Type.BLACK_SEGMENT
import com.github.mmauro94.shows_merger.video_part.VideoPart.Type.SCENE
import java.time.Duration

/**
 * A match between two video parts of two different files.
 *
 * Ensures that they are of the same type.
 */
data class VideoPartMatch(val input: VideoPart, val target: VideoPart) {
    init {
        require(input.type == target.type)
    }

    val type = input.type
}

/**
 * Returns whether the different in duration of this video part with the given [duration] is within margin.
 */
fun VideoPart.acceptableDurationDiff(duration: Duration): Boolean {
    return (this.time.duration - duration).abs() < Duration.ofMillis(250)
}

/**
 * Returns whether the different in duration of this video part with the given [videoPart] is within margin.
 */
fun VideoPart.acceptableDurationDiff(videoPart: VideoPart): Boolean {
    return acceptableDurationDiff(videoPart.time.duration)
}

/**
 * Exception thrown when a video parts match cannot be found
 */
class VideoPartsMatchException(message: String, val input: VideoParts, val targets: VideoParts) : Exception(message)

/**
 * Matches [this] video parts with [targets] iff they have exactly the same number of scenes and most scenes are of acceptable duration.
 */
private fun VideoParts.matchWithTargetExact(targets: VideoParts): List<VideoPartMatch>? {
    return if (this.parts.isNotEmpty() && this.parts.size == targets.parts.size && this.parts.first().type == this.parts.first().type) {
        val zip = this.scenes.zip(targets.scenes)
        val numberOfAcceptableErrors = zip.count { (input, target) ->
            input.acceptableDurationDiff(target)
        }
        if (numberOfAcceptableErrors / zip.size.toFloat() > 0.8) {
            this.parts.zip(targets.parts).map { (input, target) ->
                VideoPartMatch(input, target)
            }
        } else null
    } else null
}

/**
 * Returns the scene that should be paired with the first scene of [targets], or `null` if it can't be found.
 */
private fun VideoParts.matchFirstScene(targets: VideoParts): VideoPart? {
    val firstScene = targets.scenes.firstOrNull() ?: return null
    return scenes.take(2).firstOrNull { firstScene.acceptableDurationDiff(it) }
}

/**
 * Returns the offset between the matched first scene and the first [targets] scene. If no match is found returns `null`.
 * @see matchFirstScene
 */
fun VideoParts.matchFirstSceneOffset(targets: VideoParts): Duration? {
    val matchedFirstScene = matchFirstScene(targets) ?: return null
    val firstTargetScene = targets.scenes.first()
    return matchedFirstScene.time.start - firstTargetScene.time.start
}

/**
 * Matches [this] video parts with [targets] using a more lenient and smart algorithm.
 */
private fun VideoParts.matchWithTargetApprox(targets: VideoParts): List<VideoPartMatch> {
    val matchedFirstScene =
        matchFirstScene(targets) ?: throw VideoPartsMatchException("Unable to match first scene", this, targets)
    val matches = mutableListOf<VideoPartMatch>()

    //Adding first black match if at the start of the scene
    if (targets.parts.first().type == BLACK_SEGMENT && parts.first().type == BLACK_SEGMENT && matchedFirstScene == scenes.first()) {
        matches.add(VideoPartMatch(parts.first(), targets.parts.first()))
    }

    //Adding first scene match
    matches.add(VideoPartMatch(matchedFirstScene, targets.scenes.first()))

    //Now we calculate the remaining video parts that we have yet to match, and we get the iterators
    val remainingInputParts = parts.subList(
        fromIndex = parts.indexOf(matchedFirstScene) + 1,
        toIndex = parts.size
    ).iterator()
    val remainingTargetParts = targets.parts.subList(
        fromIndex = targets.parts.indexOf(targets.scenes.first()) + 1,
        toIndex = targets.parts.size
    ).iterator()

    //For a match to exist, both parts must exist
    bigWhile@ while (remainingInputParts.hasNext() && remainingTargetParts.hasNext()) {
        val nextInputPart = remainingInputParts.next()
        val nextTargetPart = remainingTargetParts.next()

        //Check that they are of the same type. Shouldn't be necessary, but better safe then sorry
        require(nextInputPart.type == nextTargetPart.type)
        val type = nextInputPart.type

        if (type == BLACK_SEGMENT || nextTargetPart.acceptableDurationDiff(nextInputPart)) {
            //Black segments are always a match because they can be of completely different lengths
            //Scenes are always a match if they are close enough in duration
            matches += VideoPartMatch(nextInputPart, nextTargetPart)
        } else if (nextInputPart == this.scenes.last()) {
            //Special case: the input part is the last scene of the file
            //In this case there might be a mismatch of durations because the last part may have been cut out from the input file
            //We allow it anyway
            matches += VideoPartMatch(nextInputPart, nextTargetPart)
        } else if (nextInputPart.time.duration < nextTargetPart.time.duration) {
            //Here it probably means that in the target file there is no black segment, while in the input there is
            //So I try to accumulate the scenes until a correct duration is found, or I go too far
            val accumulated = mutableListOf(nextInputPart)
            while (remainingInputParts.hasNext()) {
                val next = remainingInputParts.next()
                //Discard black segments
                if (next.type == SCENE) {
                    accumulated += next

                    val accumulatedDuration = accumulated.map { it.time.duration }.sum()
                    if (nextTargetPart.acceptableDurationDiff(accumulatedDuration)) {
                        //Match found! Need to split the target in multiple pieces, one for each input part
                        var start = nextTargetPart.time.start
                        for (i in accumulated.indices) {
                            val end =
                                if (i == accumulated.lastIndex)
                                    nextTargetPart.time.end
                                else start + accumulated[i].time.duration
                            matches += VideoPartMatch(
                                accumulated[i],
                                VideoPart(
                                    time = DurationSpan(start, end),
                                    type = SCENE
                                )
                            )
                            start += accumulated[i].time.duration
                        }

                        //After adding the matches matching should continue as normal
                        continue@bigWhile
                    } else if (accumulatedDuration > nextTargetPart.time.duration) {
                        //I overshot with the duration, so no match could be found
                        //I have no other option than to throw
                        throw VideoPartsMatchException(
                            "Unable to match target scene ending @ ${nextTargetPart.time.end.humanStr()}: next scenes aren't of the correct length",
                            this,
                            targets
                        )
                    }
                }
            }
        } else if (nextInputPart.time.duration > nextTargetPart.time.duration) {
            //There is probably a black frame in the target video that is not present in the input video
            //In this case, I cannot possibly know where to cut the input video, so I throw
            throw VideoPartsMatchException(
                "Black segment in target video @ ${nextTargetPart.time.end.humanStr()} not present in input video",
                this,
                targets
            )
        }
    }
    return matches
}

/**
 * Tries to match two [VideoParts] of two different files.
 *
 * Returns `null` if no match can be found.
 */
fun VideoParts.matchWithTarget(targets: VideoParts): List<VideoPartMatch>? {
    return matchWithTargetExact(targets) ?: matchWithTargetApprox(targets)
}