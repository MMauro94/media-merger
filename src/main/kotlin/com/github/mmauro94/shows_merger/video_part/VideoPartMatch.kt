package com.github.mmauro94.shows_merger.video_part

import com.github.mmauro94.shows_merger.sum
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

fun VideoPart.acceptableDurationDiff(duration: Duration): Boolean {
    return (this.duration - duration).abs() < Duration.ofMillis(150)
}

fun VideoPart.acceptableDurationDiff(other: VideoPart): Boolean {
    return acceptableDurationDiff(other.duration)
}

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

private fun VideoParts.matchFirstScene(targets: VideoParts): VideoPart? {
    val firstScene = targets.scenes.firstOrNull() ?: return null
    return scenes.take(2).firstOrNull { firstScene.acceptableDurationDiff(it) }
}

private fun VideoParts.matchWithTargetApprox(targets: VideoParts): List<VideoPartMatch>? {
    val matchedFirstScene = matchFirstScene(targets) ?: return null
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

        //Black segments are always a match because they can be of completely different lengths
        //Scenes are always a match if they are close enough in duration
        if (type == BLACK_SEGMENT || nextTargetPart.acceptableDurationDiff(nextInputPart)) {
            matches += VideoPartMatch(nextInputPart, nextTargetPart)
        } else if (nextInputPart.duration < nextTargetPart.duration) {
            //Here it probably means that in the target file there is no black segment, while in the input there is
            //So I try to accumulate the scenes until a correct duration is found, or I go too far
            val accumulated = mutableListOf(nextInputPart)
            while (remainingInputParts.hasNext()) {
                val next = remainingInputParts.next()
                //Discard black segments
                if (next.type == SCENE) {
                    accumulated += next

                    val accumulatedDuration = accumulated.map { it.duration }.sum()
                    if (nextTargetPart.acceptableDurationDiff(accumulatedDuration)) {
                        //Match found! Need to split the target in multiple pieces, one for each input part
                        var start = nextTargetPart.start
                        for (i in accumulated.indices) {
                            val end =
                                if (i == accumulated.lastIndex)
                                    nextTargetPart.end
                                else start + accumulated[i].duration
                            matches += VideoPartMatch(
                                accumulated[i],
                                VideoPart(
                                    start = start,
                                    end = end,
                                    type = SCENE
                                )
                            )
                            start += accumulated[i].duration
                        }

                        //After adding the matches matching should continue as normal
                        continue@bigWhile
                    } else if (accumulatedDuration > nextTargetPart.duration) {
                        //I overshot with the duration, so no match could be found
                        //I have no other option than to return null
                        return null
                    }
                }
            }
        } else if (nextInputPart.duration > nextTargetPart.duration) {
            //There is probably a black frame in the target video that is not present in the input video
            //In this case, I cannot possibly know where to cut the input video, so I return null
            return null
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