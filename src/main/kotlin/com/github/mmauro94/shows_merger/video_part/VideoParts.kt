package com.github.mmauro94.shows_merger.video_part

import com.github.mmauro94.shows_merger.*
import com.github.mmauro94.shows_merger.util.DurationSpan
import com.github.mmauro94.shows_merger.util.asSecondsDuration
import com.github.mmauro94.shows_merger.util.toTotalSeconds
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.io.PrintStream
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Class that contains a list of [VideoPart]s.
 *
 * It requires and ensures that consecutive parts have different type and that the first part starts at 0:00.
 */
data class VideoParts(val parts: List<VideoPart>) {

    init {
        require(parts.first().time.start == Duration.ZERO)
        parts.zipWithNext().forEach { (a, b) ->
            require(a.type != b.type)
        }
    }

    /**
     * Returns only the scenes
     */
    val scenes = parts.filter { it.type == VideoPart.Type.SCENE }

    /**
     * Returns only the black segments
     */
    val blackSegments = parts.filter { it.type == VideoPart.Type.BLACK_SEGMENT }

    override fun toString() = parts.joinToString(separator = "\n")
}

/**
 * Multiplies all the [VideoPart]s to the provided [stretchFactor].
 *
 * @see VideoPart.times
 */
operator fun VideoParts.times(stretchFactor: StretchFactor) = VideoParts(parts.map { it * stretchFactor })

/**
 * Detects the [VideoParts] from this [InputFile].
 *
 * It also saves the output of the command to a file with the same prefix as the input file. If this file is already
 * present, it parses it rather than performing the command again.
 *
 * Returns `null` if no black segments were found.
 *
 * @param minDuration the min duration of the black segments. All black segments with a smaller duration will be ignored
 * @param secondsLimits if provided, limits the parsing to the first seconds. Useful if we are interesed only in the first black frames
 */
fun InputFile.detectVideoParts(minDuration: Duration, secondsLimits: Long? = null): VideoParts? {
    val prefix = file.nameWithoutExtension + "_blackframes_minDuration${minDuration.toMillis()}"
    val fileRegex = (Regex.escape(prefix) + "(?:_([0-9]+))?\\.txt").toRegex()
    val max = file.parentFile.listFiles()
        ?.mapNotNull {
            val match = fileRegex.matchEntire(it.name)
            if (match != null && it.isFile) {
                it to (match.groups[1]?.value?.toLong() ?: Long.MAX_VALUE)
            } else null
        }
        ?.maxBy { it.second }
    val blackFramesFile = if (max != null && max.second >= (secondsLimits ?: Long.MAX_VALUE)) {
        max.first
    } else {
        File(
            file.parentFile,
            prefix + (if (secondsLimits != null) "_$secondsLimits" else "") + ".txt"
        )
    }

    if (!blackFramesFile.exists()) {
        val builder = FFmpegBuilder()
            .setVerbosity(FFmpegBuilder.Verbosity.INFO)
            .setInput(file.absolutePath).apply {
                Main.config?.ffmpegHardwareAcceleration?.let {
                    addExtraArgs("-hwaccel", it)
                }
            }
            .addStdoutOutput()
            .apply {
                if (secondsLimits != null) {
                    addExtraArgs("-t", secondsLimits.toString())
                }
            }
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
                    val lod = if (secondsLimits != null) Duration.ofSeconds(secondsLimits) else duration

                    val percentage = if (lod == null) null else prg.out_time_ns / lod.toNanos().toDouble()
                    if (percentage != null) {
                        realOut.println(
                            String.format(
                                "[%.0f%%] %s, speed:%.2fx",
                                percentage * 100.0,
                                FFmpegUtils.toTimecode(prg.out_time_ns, TimeUnit.NANOSECONDS),
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
    val blackSegments = blackFramesFile.readLines()
        .mapNotNull { regex.matchEntire(it) }
        .map {
            VideoPart(
                time = DurationSpan(
                    start = it.groups[1]!!.value.toBigDecimal().asSecondsDuration(),
                    end = it.groups[2]!!.value.toBigDecimal().asSecondsDuration()
                ),
                type = VideoPart.Type.BLACK_SEGMENT
            )
        }
        .filter {
            if (secondsLimits != null) it.time.end < Duration.ofSeconds(secondsLimits) else true
        }
        .toMutableList()
    return if (blackSegments.isEmpty()) null
    else {
        val scenes = blackSegments.zipWithNext { a, b ->
            VideoPart(
                DurationSpan(
                    start = a.time.end,
                    end = b.time.start
                ),
                VideoPart.Type.SCENE
            )
        }
        val ret = (blackSegments.zip(scenes).flatMap { (a, b) -> listOf(a, b) } + blackSegments.last()).toMutableList()
        if (ret.first().time.start > Duration.ZERO) {
            ret.add(
                0,
                VideoPart(
                    DurationSpan(
                        Duration.ZERO,
                        ret.first().time.start
                    ),
                    VideoPart.Type.SCENE
                )
            )
        }
        val duration = this.duration
        if (duration != null && ret.last().time.end < duration) {
            ret.add(
                VideoPart(
                    DurationSpan(
                        ret.last().time.end,
                        duration
                    ),
                    VideoPart.Type.SCENE
                )
            )
        }
        VideoParts(ret)
    }
}