package com.github.mmauro94.media_merger.video_part

import com.github.mmauro94.media_merger.InputFile
import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.StretchFactor
import com.github.mmauro94.media_merger.Track
import com.github.mmauro94.media_merger.config.FFMpegBlackdetectConfig
import com.github.mmauro94.media_merger.util.*
import com.github.mmauro94.media_merger.video_part.VideoPart.Type.BLACK_SEGMENT
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
 * It requires and ensures that consecutive parts have different type and that the first part starts at 0:00.
 */
class VideoPartIterator(
    private val cache: MutableList<VideoPart>,
    private val iterator: Iterator<VideoPart>,
    private val transform: VideoPartIterator.(VideoPart) -> List<VideoPart> = { listOf(it) }
) : ListIterator<VideoPart> {

    var nextIndex = 0
        private set

    fun cache(): List<VideoPart> = cache

    private fun addToCache(videoPart: VideoPart) {
        if (cache.isEmpty()) {
            check(videoPart.time.start == Duration.ZERO)
        } else {
            check(cache.last().type != videoPart.type)
            check(videoPart.time.isConsecutiveOf(cache.last().time))
        }
        cache.add(videoPart)
    }

    override fun hasNext(): Boolean {
        return if (nextIndex in cache.indices) true
        else iterator.hasNext()
    }

    override fun hasPrevious(): Boolean {
        return nextIndex > 0
    }

    override fun next(): VideoPart {
        val ret = value {
            val next = iterator.next()
            val transformed = transform(this, next)
            check(transformed.isNotEmpty())
            transformed.forEach(::addToCache)
            transformed.first()
        }
        nextIndex++
        return ret
    }

    private inline fun value(otherwise: () -> VideoPart) =
        if (nextIndex in cache.indices) cache[nextIndex] else otherwise()

    override fun nextIndex(): Int {
        return nextIndex
    }

    override fun previous(): VideoPart {
        nextIndex--
        return value { throw NoSuchElementException() }
    }

    override fun previousIndex(): Int {
        return nextIndex - 1
    }

    fun peek(): VideoPart {
        return next().also { previous() }
    }

    fun reset() {
        nextIndex = 0
    }

    /**
     * Multiplies all the [VideoPart]s to the provided [stretchFactor].
     *
     * @see VideoPart.times
     */
    operator fun times(stretchFactor: StretchFactor) = VideoPartIterator(mutableListOf(), this) { listOf(it * stretchFactor) }

    operator fun plus(offset: Duration): VideoPartIterator {
        check(!offset.isNegative)
        return if (offset.isZero) copy()
        else VideoPartIterator(mutableListOf(), this) {
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
        }
    }

    fun copy(): VideoPartIterator = VideoPartIterator(cache, iterator, transform)

    fun skipIfBlackFragment() {
        if (hasNext() && peek().type == BLACK_SEGMENT) {
            next()
        }
    }

    fun goTo(nextIndex: Int) {
        require(nextIndex in 0..cache.size)
        this.nextIndex = nextIndex
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

    fun readOnly(): List<VideoPart> = iterator.cache()

    /**
     * Multiplies all the [VideoPart]s to the provided [stretchFactor].
     *
     * @see VideoPart.times
     */
    operator fun times(stretchFactor: StretchFactor) = VideoParts(iterator * stretchFactor)

    operator fun plus(offset: Duration) = VideoParts(iterator + offset)

}

fun Iterable<VideoPart>.sumDurations() = map { it.time.duration }.sum()

/**
 * Multiplies all the [VideoPart]s to the provided [stretchFactor].
 *
 * @see VideoPart.times
 */
operator fun Iterable<VideoPart>.times(stretchFactor: StretchFactor) = map { it * stretchFactor }


class VideoPartsProvider(
    private val inputFile: InputFile,
    private val config: FFMpegBlackdetectConfig,
    private val offset: Duration
) {

    private fun videoParts(chunkSize: Duration?): VideoParts {
        return VideoParts(inputFile.detectVideoParts(config, chunkSize)) + offset
    }


    fun lazy(chunkSize: Duration = Duration.ofSeconds(30)): VideoParts {
        return videoParts(chunkSize)
    }

    fun all(): VideoParts {
        return videoParts(null)
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
    config: FFMpegBlackdetectConfig,
    chunkSize: Duration?
): VideoPartIterator {
    if (chunkSize != null) {
        require(chunkSize > Duration.ZERO)
    }
    return VideoPartIterator(mutableListOf(), iterator {
        val videoTrack = tracks.singleOrNull { it.isVideoTrack() }
        if (videoTrack != null) {
            var chunk = if (chunkSize == null) null else DurationSpan(Duration.ZERO, chunkSize)
            var blacks = videoTrack.detectBlackSegments(config, chunk)

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
                    blacks = videoTrack.detectBlackSegments(config, chunk)
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
fun Track.detectBlackSegments(
    config: FFMpegBlackdetectConfig,
    range: DurationSpan? = null
): List<DurationSpan>? {
    //If we must detect a range, we seek a bit earlier in order to prevent tiny errors in timings
    val errorMargin: Duration = Duration.ofSeconds(1)
    val seek = if (range != null && range.start > Duration.ZERO) {
        max(Duration.ZERO, range.start - errorMargin)
    } else Duration.ZERO

    val rangeStr = if (range == null) "all" else range.start.toTimeString('.') + "_" + range.end.toTimeString('.')
    val filename =
        file.nameWithoutExtension + "@blacksegments@range_$rangeStr@" + config.toFilenameString()
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
                    append("d=" + config.minDuration.toTotalSeconds())
                    config.pictureBlackThreshold?.let {
                        append(":pic_th=" + it.toPlainString())
                    }
                    config.pixelBlackThreshold?.let {
                        append(":pix_th=" + it.toPlainString())
                    }
                    append('"')
                }
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
                start = it.groups[1]!!.value.toBigDecimal().toSecondsDuration(),
                end = it.groups[2]!!.value.toBigDecimal().toSecondsDuration()
            ) + seek

            //If the black segments starts before the range, adjust accordingly
            if (range != null && ds.end <= range.start) null //In this case the entire segments is before the error margin, thus ignore
            else if (range != null && ds.start < range.start) ds.copy(start = range.start) //In this case we just truncate the black fragment to start at the start of the range
            else ds //Otherwise return normally
        }
}