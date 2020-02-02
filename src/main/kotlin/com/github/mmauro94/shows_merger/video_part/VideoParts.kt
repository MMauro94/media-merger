package com.github.mmauro94.shows_merger.video_part

import com.github.mmauro94.shows_merger.InputFile
import com.github.mmauro94.shows_merger.Main
import com.github.mmauro94.shows_merger.StretchFactor
import com.github.mmauro94.shows_merger.util.DurationSpan
import com.github.mmauro94.shows_merger.util.asSecondsDuration
import com.github.mmauro94.shows_merger.util.toTimeString
import com.github.mmauro94.shows_merger.util.toTotalSeconds
import com.github.mmauro94.shows_merger.video_part.VideoPart.Type.BLACK_SEGMENT
import com.github.mmauro94.shows_merger.video_part.VideoPart.Type.SCENE
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.apache.commons.lang3.ObjectUtils.max
import java.io.File
import java.io.PrintStream
import java.time.Duration
import java.util.concurrent.TimeUnit


/**
 * Class that is a [Sequence] of [VideoPart]s.
 *
 * It requires and ensures that consecutive parts have different type and that the first part starts at 0:00.
 */
class LazyVideoParts(
    inputFile: InputFile,
    minDuration: Duration,
    chunkSize: Duration?
) : Sequence<VideoPart> {

    init {
        if (chunkSize != null) {
            require(chunkSize > Duration.ZERO)
        }
    }

    private val iterator = inputFile.detectVideoParts(minDuration, chunkSize)

    private val cache: MutableList<VideoPart> = mutableListOf()

    private fun addToCache(videoPart: VideoPart) {
        if (cache.isEmpty()) {
            check(videoPart.time.start == Duration.ZERO)
        } else {
            check(cache.last().type != videoPart.type)
            check(videoPart.time.isConsecutiveOf(cache.last().time))
        }
        cache.add(videoPart)
    }

    override fun iterator(): Iterator<VideoPart> {
        return iterator {
            yieldAll(cache)

            iterator.forEach { vp ->
                addToCache(vp)
                yield(vp)
            }
        }
    }

    override fun toString() = joinToString(separator = "\n")

}

/**
 * Returns only the scenes
 */
val Sequence<VideoPart>.scenes
    get() = filter { it.type == SCENE }

/**
 * Returns only the black segments
 */
val Sequence<VideoPart>.blackSegments
    get() = filter { it.type == BLACK_SEGMENT }

/**
 * Multiplies all the [VideoPart]s to the provided [stretchFactor].
 *
 * @see VideoPart.times
 */
operator fun Sequence<VideoPart>.times(stretchFactor: StretchFactor) = map { it * stretchFactor }




/**
 * Returns only the scenes
 */
val Iterable<VideoPart>.scenes
    get() = filter { it.type == SCENE }

/**
 * Returns only the black segments
 */
val Iterable<VideoPart>.blackSegments
    get() = filter { it.type == BLACK_SEGMENT }

/**
 * Multiplies all the [VideoPart]s to the provided [stretchFactor].
 *
 * @see VideoPart.times
 */
operator fun Iterable<VideoPart>.times(stretchFactor: StretchFactor) = map { it * stretchFactor }


class VideoParts(
    private val inputFile: InputFile,
    private val minDuration: Duration
) {

    fun lazy(chunkSize: Duration = Duration.ofSeconds(30)): Sequence<VideoPart> {
        return LazyVideoParts(inputFile, minDuration, chunkSize)
    }

    fun all(): List<VideoPart> {
        return (LazyVideoParts(inputFile, minDuration, null).toList())
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
    minDuration: Duration,
    chunkSize: Duration?
): Iterator<VideoPart> {
    return iterator {
        var chunk = if (chunkSize == null) null else DurationSpan(Duration.ZERO, chunkSize)
        var blacks = detectBlackSegments(minDuration, chunk)
        var lastBlack: DurationSpan? = null

        while (blacks != null) {
            for (black in blacks) {
                when {
                    lastBlack == null -> {
                        //This is the first black we encounter
                        if (!black.start.isZero) {
                            //Video starts with a scene
                            yield(Scene(Duration.ZERO, black.end))
                        }
                        lastBlack = black
                    }
                    black.isConsecutiveOf(lastBlack) -> {
                        //The new black is directly after the last one, we need to merge them
                        lastBlack = DurationSpan(lastBlack.start, black.end)
                    }
                    else -> {
                        //In this case we need to yield both the previous black
                        yield(BlackSegment(lastBlack))
                        //as well as the scene between the two
                        yield(Scene(lastBlack.end, black.start))
                        lastBlack = black
                    }
                }
            }
            if (chunk != null) {
                chunk = chunk.consecutiveOfSameLength()
                blacks = detectBlackSegments(minDuration, chunk)
            } else {
                blacks = null
            }
        }
        //End of file reached
        //We return the last black segment, if it exists
        if (lastBlack != null) {
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
}

/**
 * Detects the black segments from this [InputFile].
 *
 * It also saves the output of the command to a file with the same prefix as the input file. If this file is already
 * present, it parses it rather than performing the command again.
 *
 * Returns a list of [DurationSpan] representing the durations segments at which the black segments were found.
 * Returns `null` if the given [range] is outside the range of the input file.
 *
 * @param minDuration the min duration of the black segments. All black segments with a smaller duration will be ignored
 * @param range the range of the input file where to search the black segments in. If a black segment is between the start/end of the range, it will be returned split.
 */
fun InputFile.detectBlackSegments(
    minDuration: Duration,
    range: DurationSpan? = null
): List<DurationSpan>? {
    //If we must detect a range, we seek a bit earlier in order to prevent tiny errors in timings
    val errorMargin: Duration = Duration.ofSeconds(1)
    val seek = if (range != null && range.start > Duration.ZERO) {
        max(Duration.ZERO, range.start - errorMargin)
    } else Duration.ZERO

    val rangeStr = if (range == null) "all" else range.start.toTimeString('.') + "_" + range.end.toTimeString('.')
    val filename =
        file.nameWithoutExtension + "@blacksegments@range_$rangeStr@minDuration_" + minDuration.toTimeString('.')
    val blackFramesFile = File(
        file.parentFile,
        "$filename.txt"
    )

    if (!blackFramesFile.exists()) {
        val builder = FFmpegBuilder()
            .setVerbosity(FFmpegBuilder.Verbosity.INFO)
            .apply {
                if (range != null) {
                    if (seek > Duration.ZERO) {
                        addExtraArgs("-ss", seek.toTotalSeconds())
                    }
                    addExtraArgs("-t", range.duration.toTotalSeconds())
                }
            }
            .setInput(file.absolutePath).apply {
                Main.config?.ffmpegHardwareAcceleration?.let {
                    addExtraArgs("-hwaccel", it)
                }
            }
            .addStdoutOutput()
            .addExtraArgs(
                "-vf",
                "\"blackdetect=d=" + minDuration.toTotalSeconds() + "\""
            )
            .addExtraArgs("-an")

            .setFormat("null")
            .done()

        println(builder.build().joinToString(" "))
        FFmpegExecutor(FFmpeg(), FFprobe()).apply {
            println("Detecting blackframes...")
            val realOut = System.out
            val ps = PrintStream(blackFramesFile)
            try {
                System.setOut(ps)
                createJob(builder) { prg ->
                    val lod = range?.duration ?: duration

                    val percentage = if (lod == null) null else prg.out_time_ns / lod.toNanos().toDouble()
                    if (percentage != null) {
                        realOut.println(
                            String.format(
                                "[%.0f%%] %s, speed:%.2fx",
                                percentage * 100.0,
                                FFmpegUtils.toTimecode(
                                    prg.out_time_ns + (range?.start?.toNanos() ?: 0),
                                    TimeUnit.NANOSECONDS
                                ),
                                prg.speed
                            )
                        )
                    } else {
                        realOut.println(
                            String.format(
                                "[N/A] %s, speed:%.2fx",
                                FFmpegUtils.toTimecode(prg.out_time_ns, TimeUnit.NANOSECONDS),
                                prg.speed
                            )
                        )
                    }
                }.run()
            } finally {
                ps.close()
                System.setOut(realOut)
            }
        }
    }

    val regex =
        "\\[blackdetect @ [0-9a-f]+] black_start:(\\d+(?:\\.\\d+)?) black_end:(\\d+(?:\\.\\d+)?).+".toRegex()
    val lines = blackFramesFile.readLines()
    return if (lines.any { it.contains("Output file is empty", ignoreCase = true) }) {
        null
    } else lines
        .mapNotNull { regex.matchEntire(it) }
        .mapNotNull {
            val ds = DurationSpan(
                start = it.groups[1]!!.value.toBigDecimal().asSecondsDuration(),
                end = it.groups[2]!!.value.toBigDecimal().asSecondsDuration()
            ) + seek

            //If the black segments starts before the range, adjust accordingly
            if (range != null && ds.end <= range.start) null //In this case the entire segments is before the error margin, thus ignore
            else if (range != null && ds.start < range.start) ds.copy(start = range.start) //In this case we just truncate the black fragment to start at the start of the range
            else ds //Otherwise return normally
        }
}