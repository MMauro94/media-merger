package com.github.mmauro94.media_merger.video_part

import com.github.mmauro94.media_merger.InputFile
import com.github.mmauro94.media_merger.LinearDrift
import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.Track
import com.github.mmauro94.media_merger.config.FFMpegBlackdetectThresholds
import com.github.mmauro94.media_merger.util.*
import com.github.mmauro94.media_merger.util.iter.CachedIterator
import com.github.mmauro94.media_merger.util.iter.takeUntil
import com.github.mmauro94.media_merger.util.iter.transform
import com.github.mmauro94.media_merger.util.json.KLAXON
import com.github.mmauro94.media_merger.util.json.toPrettyJsonString
import com.github.mmauro94.media_merger.util.progress.Progress
import com.github.mmauro94.media_merger.video_part.VideoPart.Type.BLACK_SEGMENT
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.io.PrintStream
import java.time.Duration
import kotlin.math.min

/**
 * It requires and ensures that consecutive parts have different type and that the first part starts at 0:00.
 */
class VideoPartIterator(iterator: Iterator<VideoPart>) : CachedIterator<VideoPart>(iterator) {
    override fun addToCache(item: VideoPart): VideoPart {
        if (cache.isNotEmpty()) {
            check(cache.last().type != item.type)
            check(item.time.isConsecutiveOf(cache.last().time))
        }
        return super.addToCache(item)
    }

    /**
     * Multiplies all the [VideoPart]s to the provided [linearDrift].
     *
     * @see VideoPart.times
     */
    operator fun times(linearDrift: LinearDrift) = VideoPartIterator(copy().transform { listOf(it * linearDrift) })

    operator fun plus(offset: Duration): VideoPartIterator {
        check(!offset.isNegative)
        return if (offset.isZero) copy()
        else VideoPartIterator(copy().transform {
            if (nextIndex == 0) {
                if (it.type == BLACK_SEGMENT) {
                    listOf(
                        it.copy(
                            time = DurationSpan(
                                it.time.start,
                                it.time.end + offset
                            )
                        )
                    )
                } else {
                    listOf(
                        BlackSegment(Duration.ZERO, offset),
                        it + offset
                    )
                }
            } else listOf(it + offset)
        })
    }

    override fun copy() = VideoPartIterator(super.copy())

    fun takeUntil(videoPart: VideoPart)  = VideoPartIterator(takeUntil { it != videoPart })

    fun skipIfBlackFragment() {
        skipIf { it.type == BLACK_SEGMENT }
    }
}

/**
 * Class that is a [Sequence] of [VideoPart]s.
 */
class VideoParts(
    private val iterator: VideoPartIterator
) : Sequence<VideoPart> {

    override fun iterator(): VideoPartIterator {
        return iterator.copy()
    }

    fun readOnly(): List<VideoPart> = iterator.cache

    /**
     * Multiplies all the [VideoPart]s to the provided [linearDrift].
     *
     * @see VideoPart.times
     */
    operator fun times(linearDrift: LinearDrift) = VideoParts(iterator * linearDrift)

    operator fun plus(offset: Duration) = VideoParts(iterator + offset)

}

fun Iterable<VideoPart>.sumDurations() = map { it.time.duration }.sum()

fun Iterable<VideoPart>.duration() = maxOf { it.time.end }


/**
 * Multiplies all the [VideoPart]s to the provided [linearDrift].
 *
 * @see VideoPart.times
 */
operator fun Iterable<VideoPart>.times(linearDrift: LinearDrift) = map { it * linearDrift }


class VideoPartsProvider(
    private val inputFile: InputFile,
    private val thresholds: FFMpegBlackdetectThresholds,
    private val minDuration: Duration,
    private val offset: Duration
) {

    private fun videoParts(reporter: Reporter, chunkSize: Duration?): VideoParts {
        return VideoParts(inputFile.detectVideoParts(thresholds, minDuration, chunkSize, reporter)) + offset
    }


    fun lazy(reporter: Reporter, chunkSize: Duration = Duration.ofSeconds(30)): VideoParts {
        return videoParts(reporter, chunkSize)
    }

    fun all(reporter: Reporter): VideoParts {
        return videoParts(reporter, null)
    }

    fun get(lazy: Boolean, reporter: Reporter): VideoParts {
        return if (lazy) lazy(reporter)
        else all(reporter)
    }
}


/**
 * Detects the black fragments on this [InputFile] using [detectBlackSegments], and returns an iterator of [VideoPart].
 *
 * It does so by lazily calling [detectBlackSegments] multiple times, each time detecting a chunk of size [chunkSize].
 * If [chunkSize] is null, all black segments are detected in a single pass.
 *
 * This iterator automatically takes care of merging black segments that happen to be between chunks.
 */
private fun InputFile.detectVideoParts(
    thresholds: FFMpegBlackdetectThresholds,
    minDuration: Duration,
    chunkSize: Duration?,
    reporter: Reporter
): VideoPartIterator {
    if (chunkSize != null) {
        require(chunkSize > Duration.ZERO)
    }

    val videoTrack = tracks.singleOrNull { it.isVideoTrack() }

    fun createReporter(chunk: DurationSpan?): Reporter {
        val message = "Detecting black frames segments in chunk $chunk..."
        return when {
            duration == null -> reporter.split(Progress.INDETERMINATE, message)
            chunk == null -> reporter
            else -> reporter.split(
                min(1f, chunk.start.toSeconds() / duration.toSeconds().toFloat()),
                min(1f, chunk.end.toSeconds() / duration.toSeconds().toFloat()),
                message
            )
        }
    }

    return VideoPartIterator(iterator {
        if (videoTrack != null) {
            var chunk = if (chunkSize == null) null else DurationSpan(Duration.ZERO, chunkSize)

            var blacks = videoTrack.detectBlackSegments(thresholds, minDuration, chunk, createReporter(chunk))

            var lastBlack: DurationSpan? = null
            var lastYielded = true

            while (blacks != null) {
                for (black in blacks) {
                    lastBlack = when {
                        lastBlack == null -> {
                            //This is the first black we encounter
                            if (black.start <= videoTrack.startTime) {
                                //Black starts right at or before the start of the track
                                //Then we register a black that starts at the start of the file
                                DurationSpan(Duration.ZERO, black.end)
                            } else {
                                //Otherwise it means that the track starts with a scene
                                if (videoTrack.startTime > Duration.ZERO) {
                                    //We add a first black part if the track doesn't start at zero
                                    yield(BlackSegment(Duration.ZERO, videoTrack.startTime))
                                }
                                //And we add a scene starting at the start of the track
                                yield(Scene(videoTrack.startTime, black.start))
                                black
                            }
                        }
                        !lastYielded && black.isConsecutiveOf(lastBlack) -> {
                            //The new black is directly after the last one, we need to merge them
                            DurationSpan(lastBlack.start, black.end)
                        }
                        else -> {
                            if (!lastYielded) {
                                //In this case we need to yield both the previous black
                                yield(BlackSegment(lastBlack))
                            }
                            //as well as the scene between the two
                            yield(Scene(lastBlack.end, black.start))
                            black
                        }
                    }
                    lastYielded = false
                }
                if (chunk != null) {
                    if (!lastYielded && lastBlack!!.end < chunk.end) {
                        //In this case I know that this black segment can be safely yielded, so I do so
                        yield(BlackSegment(lastBlack))
                        lastYielded = true
                    }
                    chunk = chunk.consecutiveOfSameLength()
                    blacks = videoTrack.detectBlackSegments(thresholds, minDuration, chunk, createReporter(chunk))
                } else {
                    blacks = null
                }
            }
            //End of file reached
            //We return the last black segment, if it exists
            if (lastBlack != null && !lastYielded) {
                yield(BlackSegment(lastBlack))
            }

            if (duration != null) {
                //If we know the duration of the file we return the last scene (if it exists)
                //When lastBlack is null it means there is only a single scene in the file, starting at zero

                val lastSceneStart = (lastBlack?.end ?: Duration.ZERO)
                if (lastSceneStart < duration) {
                    yield(Scene(lastSceneStart, duration))
                }
            }
        }
    })
}


/**
 * Calls [Track.runBlackSegmentDetection] or returned the cached value if present
 */
fun Track.detectBlackSegments(
    thresholds: FFMpegBlackdetectThresholds,
    minDuration: Duration,
    range: DurationSpan? = null,
    reporter: Reporter
): List<DurationSpan>? {
    val blacksegmentsFile = File(file.parentFile, file.name + ".blacksegments.json")
    val cache = if (blacksegmentsFile.exists()) {
        blacksegmentsFile.reader().use { r ->
            CachedBlackSegments.fromJsonArray(KLAXON.parseJsonArray(r))
        }
    } else CachedBlackSegments()

    return cache[thresholds].takeOrComputeForRange(range) { start, end ->
        runBlackSegmentDetection(thresholds, start, end, reporter)
    }?.filter { it.duration >= minDuration }.also {
        blacksegmentsFile.writeText(KLAXON.toPrettyJsonString(cache.simplify().toJsonArray()))
    }
}

/**
 * Detects the black segments from this [InputFile].
 *
 * Returns a list of [DurationSpan] representing the durations segments at which the black segments were found.
 * Returns `null` if the given [range] is outside the range of the input file.
 *
 * [start] and [end] are the range of the input file where to search the black segments in. If a black segment is between the start/end of the range, it will be returned split.
 */
private fun Track.runBlackSegmentDetection(
    thresholds: FFMpegBlackdetectThresholds,
    start: Duration = Duration.ZERO,
    end: Duration? = null,
    reporter: Reporter
): List<DurationSpan>? {
    require(!start.isNegative)
    val duration = when {
        end != null -> {
            require(start < end)
            end - start
        }
        durationOrFileDuration != null -> durationOrFileDuration - start
        else -> null
    }

    //If we must detect a range, we seek a bit earlier in order to prevent tiny errors in timings
    val errorMargin: Duration = Duration.ofSeconds(1)
    val seek = maxOf(Duration.ZERO, start - errorMargin)

    val builder = FFmpegBuilder()
        .setVerbosity(FFmpegBuilder.Verbosity.INFO)
        .apply {
            if (seek > Duration.ZERO) {
                addExtraArgs("-ss", seek.toTotalSeconds())
            }
            if (end != null) {
                addExtraArgs("-t", (end - start).toTotalSeconds())
            }
        }
        .setInput(file.absolutePath).apply {
            Main.config.ffmpeg.hardwareAcceleration?.let {
                addExtraArgs("-hwaccel", it)
            }
        }
        .addStdoutOutput()
        .addExtraArgs(
            "-map",
            "0:" + ffprobeStream.index,
            "-vf",
            buildString {
                append('"')
                append("blackdetect=")
                append("d=0")
                append(":pic_th=" + thresholds.pictureBlackThreshold.toPlainString())
                append(":pix_th=" + thresholds.pixelBlackThreshold.toPlainString())
                append('"')
            }
        )
        .addExtraArgs("-an")

        .setFormat("null")
        .done()

    val tmpFile = newTmpFile()
    try {
        FFmpegExecutor(FFmpeg(), FFprobe()).apply {
            val realOut = System.out
            val ps = PrintStream(tmpFile)
            try {
                System.setOut(ps)
                createJob(builder) { prg ->
                    reporter.progress.ffmpeg(prg, duration, start)
                }.run()
            } finally {
                ps.close()
                System.setOut(realOut)
            }
        }

        val regex = "\\[blackdetect @ [0-9a-f]+] black_start:(\\d+(?:\\.\\d+)?) black_end:(\\d+(?:\\.\\d+)?).+".toRegex()
        val lines = tmpFile.readLines()
        return if (lines.any { it.contains("Output file is empty", ignoreCase = true) }) {
            null
        } else lines
            .mapNotNull { regex.matchEntire(it) }
            .map {
                DurationSpan(
                    start = it.groups[1]!!.value.toBigDecimal().toSecondsDuration(),
                    end = it.groups[2]!!.value.toBigDecimal().toSecondsDuration()
                ) + seek
            }.restrictTo(start, end)
    } finally {
        tmpFile.delete()
    }
}