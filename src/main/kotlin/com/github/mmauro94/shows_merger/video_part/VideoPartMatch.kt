package com.github.mmauro94.shows_merger.video_part

import com.github.mmauro94.shows_merger.util.DurationSpan
import com.github.mmauro94.shows_merger.video_part.VideoPart.Type.BLACK_SEGMENT
import com.github.mmauro94.shows_merger.video_part.VideoPart.Type.SCENE
import java.lang.Double.max
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

data class Accuracy(val accuracy: Double, val offset : Duration?) {
    init {
        require(accuracy > 0 && accuracy <= 100)
    }

    companion object {
        val PERFECT = Accuracy(100.0, null)
    }
}

fun Collection<Accuracy>.avg() : Accuracy = Accuracy(sumByDouble { it.accuracy } / size, null)

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
class VideoPartsMatchException(message: String, val input: List<VideoPart>, val targets: List<VideoPart>) :
    Exception(message)


/**
 * Returns the offset between the matched first scene and the first [targets] scene. If no match is found returns `null`.
 */
fun VideoParts.matchFirstSceneOffset(targets: VideoParts): Duration? {
    val inputsIterator = iterator()
    val targetsIterator = targets.iterator()

    //We don't care about first black segments
    inputsIterator.skipIfBlackFragment()
    targetsIterator.skipIfBlackFragment()

    val candidates = mutableListOf<Accuracy>()

    var max = 3
    while(max > 0 && targetsIterator.hasNext()) {
        val target = targetsIterator.next()
        if(target.type == SCENE) {
            val match = matchNext(inputsIterator, target)
            candidates += match.second
            max--
        }
    }
    return candidates
        .sortedByDescending { it.accuracy }
        .mapNotNull { it.offset }
        .firstOrNull()
}

private fun matchNext(
    inputsIterator: VideoPartIterator,
    targetPart: VideoPart
): Pair<List<VideoPartMatch>, Accuracy> {
    val nextInputPart = inputsIterator.next()

    //Check that they are of the same type
    check(nextInputPart.type == targetPart.type)
    val type = nextInputPart.type

    if (type == BLACK_SEGMENT) {
        //Black segments are always a match because they can be of completely different lengths
        return listOf(VideoPartMatch(nextInputPart, targetPart)) to Accuracy.PERFECT
    } else {
        val matches = mutableListOf<VideoPartMatch>()

        //If we have a scene I accumulate the scenes until the duration becomes too big, then I match the closest one
        val accumulated = mutableListOf(nextInputPart)
        while (inputsIterator.hasNext() && accumulated.sum() < targetPart.time.duration) {
            val next = inputsIterator.next()
            //Discard black segments
            if (next.type == SCENE) {
                accumulated += next
            }
        }
        val diff = (targetPart.time.duration - accumulated.sum()).abs()
        val diffWithoutLast = (targetPart.time.duration - accumulated.dropLast(1).sum()).abs()
        if (diff > diffWithoutLast) {
            //The last input part doesn't belong: remove it from the accumulated and go back with the iterator
            accumulated.removeAt(accumulated.lastIndex)
            inputsIterator.previous()
        }

        var start = targetPart.time.start
        for (i in accumulated.indices) {
            val duration = accumulated[i].time.duration

            val end = if (i == accumulated.lastIndex) targetPart.time.end
            else start + duration

            matches += VideoPartMatch(
                accumulated[i],
                VideoPart(
                    time = DurationSpan(start, end),
                    type = SCENE
                )
            )
            start += duration
        }

        //Loses 1% accuracy every 500ms
        return matches to Accuracy(max(0.0, 100.0 - (minOf(diff, diffWithoutLast).toNanos() / 1_000_000_000_000.0) * 2), targetPart.time.start - accumulated.first().time.start)
    }
}

/**
 * Matches [this] video parts with [targets] using a more lenient and smart algorithm.
 */
fun VideoParts.matchWithTarget(targets: VideoParts): Pair<List<VideoPartMatch>, Accuracy> {
    val inputParts = iterator()
    val targetParts = targets.iterator()

    //TODO first scene detection

    //Skip the first black segment if the other file doesn't have it
    if (inputParts.hasNext() && targetParts.hasNext()) {
        val firstInput = inputParts.peek()
        val firstTarget = targetParts.peek()
        if (firstInput.type !== firstTarget.type) {
            inputParts.skipIfBlackFragment()
            targetParts.skipIfBlackFragment()
        }
    }

    val matches = mutableListOf<VideoPartMatch>()
    val accuracies = mutableListOf<Accuracy>()
    //For a match to exist, both parts must exist
    bigWhile@ while (inputParts.hasNext() && targetParts.hasNext()) {
        val match = matchNext(inputParts, targetParts.next())
        matches += match.first
        accuracies += match.second
    }
    return matches to accuracies.avg()
}