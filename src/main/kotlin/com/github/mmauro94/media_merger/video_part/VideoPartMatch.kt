package com.github.mmauro94.media_merger.video_part

import com.github.mmauro94.media_merger.util.DurationSpan
import com.github.mmauro94.media_merger.video_part.VideoPart.Type.BLACK_SEGMENT
import com.github.mmauro94.media_merger.video_part.VideoPart.Type.SCENE
import java.time.Duration
import kotlin.math.max

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

data class Accuracy(val accuracy: Double, val offset: Duration?) {
    init {
        require(accuracy in 0.0..100.0)
    }

    companion object {
        val PERFECT = Accuracy(100.0, null)
    }
}

fun Collection<Accuracy>.avg(): Accuracy = Accuracy(sumByDouble { it.accuracy } / size, null)

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
class VideoPartsMatchException(
    message: String
) : Exception(message)

fun VideoParts.matchFirstSceneOffset(targets: VideoParts): Duration? {
    return matchFirstScene(iterator(), targets.iterator())?.second?.offset
}

private fun matchFirstScene(
    inputsIterator: VideoPartIterator,
    targetsIterator: VideoPartIterator
): Pair<List<VideoPartMatch>, Accuracy>? {
    inputsIterator.reset()
    inputsIterator.skipIfBlackFragment()

    val candidates = mutableListOf<Pair<Pair<List<VideoPartMatch>, Accuracy>, Pair<Int, Int>>>()

    var targets = 0
    while (targets < 3 && targetsIterator.hasNext()) {
        val target = targetsIterator.next()
        if (target.type == SCENE) {
            repeat(3) { i ->
                inputsIterator.reset()
                repeat(i) {
                    if (inputsIterator.hasNext()) inputsIterator.next()
                    if (inputsIterator.hasNext()) inputsIterator.next()
                }
                inputsIterator.skipIfBlackFragment()
                if (inputsIterator.hasNext()) {
                    val match = matchNext(inputsIterator, target) to (inputsIterator.nextIndex to targetsIterator.nextIndex)
                    candidates += match
                }
            }
            targets++
        }
    }

    inputsIterator.reset()
    targetsIterator.reset()

    val (detected, indexes) = candidates.maxByOrNull { it.first.second.accuracy } ?: return null
    val (inputNextIndex, targetNextIndex) = indexes
    inputsIterator.goTo(inputNextIndex)
    targetsIterator.goTo(targetNextIndex)

    return detected
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
        while (inputsIterator.hasNext() && accumulated.sumDurations() < targetPart.time.duration) {
            val next = inputsIterator.next()
            //Discard black segments
            if (next.type == SCENE) {
                accumulated += next
            }
        }
        val diff = (targetPart.time.duration - accumulated.sumDurations()).abs()
        val offset = if (accumulated.size > 1) {
            val diffWithoutLast = (targetPart.time.duration - accumulated.dropLast(1).sumDurations()).abs()
            if (diff > diffWithoutLast) {
                //The last input part doesn't belong: remove it from the accumulated and go back with the iterator
                accumulated.removeAt(accumulated.lastIndex)
                //Need to go back two times because we also need to rewind the black segment
                inputsIterator.previous()
                inputsIterator.previous()
                diffWithoutLast
            } else diff
        } else diff

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
        return matches to Accuracy(
            max(0.0, 100.0 - (offset.toNanos() / 1_000_000_000.0) * 2),
            targetPart.time.start - accumulated.first().time.start
        )
    }
}

/**
 * Matches [this] video parts with [targets]
 */
fun VideoParts.matchWithTarget(targets: VideoParts): Pair<List<VideoPartMatch>, Accuracy> {
    val inputParts = iterator()
    val targetParts = targets.iterator()

    val matches = mutableListOf<Pair<List<VideoPartMatch>, Accuracy>>()
    val firstSceneMatch = matchFirstScene(inputParts, targetParts)
        ?: throw VideoPartsMatchException("Unable to detect first scene!")

    val initialInputParts = inputParts.copy().takeUntil(firstSceneMatch.first.first().input)
    val initialTargetParts = targetParts.copy().takeUntil(firstSceneMatch.first.first().target)
    try {
        matches += VideoParts(initialInputParts).matchWithTarget(VideoParts(initialTargetParts))
    } catch (ignored: VideoPartsMatchException) {
    }

    matches += firstSceneMatch


    while (inputParts.hasNext() && targetParts.hasNext()) {
        val match = matchNext(inputParts, targetParts.next())
        matches += match
    }
    return matches.flatMap { it.first } to matches.map { it.second }.avg()
}