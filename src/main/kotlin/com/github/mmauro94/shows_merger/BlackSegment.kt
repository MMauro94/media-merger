package com.github.mmauro94.shows_merger

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.io.PrintStream
import java.time.Duration
import java.util.concurrent.TimeUnit

data class BlackSegment(val start: Duration, val end: Duration) {

    init {
        require(start < end)
    }

    val duration: Duration = end - start
}

private operator fun Duration.times(stretchFactor: StretchFactor): Duration {
    return Duration.ofNanos(toNanos().toBigDecimal().multiply(stretchFactor.factor).toBigInteger().longValueExact())
}

operator fun BlackSegment?.times(stretchFactor: StretchFactor): BlackSegment? {
    return if (this == null) null else BlackSegment(start * stretchFactor, end * stretchFactor)
}

fun InputFile.detectBlackSegments(minDuration: Duration, limit: Duration?): List<BlackSegment> {
    val blackframesFile = File(file.parentFile, file.nameWithoutExtension + "_blackframes.txt")
    if (!blackframesFile.exists()) {
        val builder = FFmpegBuilder()
            .setVerbosity(FFmpegBuilder.Verbosity.INFO)
            .setInput(file.absolutePath)
            .addStdoutOutput()
            .apply {
                if (limit != null) {
                    addExtraArgs("-t", limit.toTotalSeconds())
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
            val ps = PrintStream(blackframesFile)
            try {
                System.setOut(ps)
                createJob(builder) { prg ->
                    val lod = (limit ?: duration)
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
    return blackframesFile.readLines()
        .mapNotNull { regex.matchEntire(it) }
        .map {
            BlackSegment(
                start = Duration.ofNanos(it.groups[1]!!.value.toBigDecimal().setScale(9).unscaledValue().longValueExact()),
                end = Duration.ofNanos(it.groups[2]!!.value.toBigDecimal().setScale(9).unscaledValue().longValueExact())
            )
        }
}
