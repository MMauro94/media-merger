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

operator fun List<BlackSegment>.times(stretchFactor: StretchFactor): List<BlackSegment> {
    return map {
        (it * stretchFactor)!!
    }
}

fun InputFile.detectBlackSegments(minDuration: Duration, secondsLimits: Long? = null): List<BlackSegment> {
    val fileRegex = (Regex.escape(file.nameWithoutExtension + "_blackframes") + "(?:_([0-9]+))?\\.txt").toRegex()
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
            file.nameWithoutExtension + "_blackframes" + (if (secondsLimits != null) "_$secondsLimits" else "") + ".txt"
        )
    }

    if (!blackFramesFile.exists()) {
        val builder = FFmpegBuilder()
            .setVerbosity(FFmpegBuilder.Verbosity.INFO)
            .setInput(file.absolutePath)
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
    return blackFramesFile.readLines()
        .mapNotNull { regex.matchEntire(it) }
        .map {
            BlackSegment(
                start = it.groups[1]!!.value.toBigDecimal().asSecondsDuration(),
                end = it.groups[2]!!.value.toBigDecimal().asSecondsDuration()
            )
        }
        .filter {
            if (secondsLimits != null) it.end < Duration.ofSeconds(secondsLimits) else true
        }
}

fun List<BlackSegment>.print() {
    if (isNotEmpty()) {
        zipWithNext().forEach { (a, b) ->
            println(a)
            println("Scene start=" + a.end + ", end=" + b.start + ", duration=" + (b.start-a.end))
        }
        println(last())
    }
}

fun main() {
    val eng =
        InputFile.parse(File("C:\\Users\\molin\\Desktop\\MON\\ENG\\The.Big.Bang.Theory.S05E01.The.Skank.Reflex.Analysis.1080p.BluRay.x264.DTS-HDMA.5.1-LeRalouf.mkv"))
    val ita =
        InputFile.parse(File("C:\\Users\\molin\\Desktop\\MON\\ITA\\The.Big.Bang.Theory.5x01.L.analisi.del.riflesso.della.sgualdrina.ITA-ENG.720p.DLMux.h264-DarkSideMux.mkv"))
    (eng.detectBlackSegments(Duration.ofMillis(250))).print()
}