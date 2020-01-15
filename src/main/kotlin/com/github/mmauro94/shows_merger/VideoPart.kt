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

data class VideoPart(val start: Duration, val end: Duration, val type: Type) {

    enum class Type {
        BLACK_SEGMENT, SCENE;
    }

    init {
        start.requireMillisPrecision()
        end.requireMillisPrecision()

        require(start < end)
    }

    val duration: Duration = end - start
    val halfDuration: Duration = duration.dividedBy(2L).makeMillisPrecision()
    val middle: Duration = start + halfDuration

    fun print() {
        val type = when (type) {
            Type.BLACK_SEGMENT -> "BlackSegment"
            Type.SCENE -> "Scene"
        }.padEnd(15)

        val start = start.toString().padEnd(15)
        val end = end.toString().padEnd(15)
        val duration = duration.toString().padEnd(15)
        println(type + "start=$start end=$end duration=$duration")
    }
}

data class VideoParts(val parts: List<VideoPart>) {

    init {
        require(parts.first().start == Duration.ZERO)
        parts.zipWithNext().forEach { (a, b) ->
            require(a.type != b.type)
        }
    }

    fun print() {
        parts.forEach {
            it.print()
        }
    }
}

private operator fun Duration.times(stretchFactor: StretchFactor): Duration {
    return Duration.ofNanos(toNanos().toBigDecimal().multiply(stretchFactor.factor).toBigInteger().longValueExact())
}

operator fun VideoPart.times(stretchFactor: StretchFactor): VideoPart {
    return this.copy(start = (start * stretchFactor).makeMillisPrecision(), end = (end * stretchFactor).makeMillisPrecision())
}

operator fun VideoParts.times(stretchFactor: StretchFactor) = VideoParts(parts.map { it * stretchFactor })

fun VideoParts.scenes() = parts.filter { it.type == VideoPart.Type.SCENE }

fun VideoParts.blackSegments() = parts.filter { it.type == VideoPart.Type.BLACK_SEGMENT }

fun InputFile.detectVideoParts(minDuration: Duration, secondsLimits: Long? = null): VideoParts? {
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
    val blackSegments = blackFramesFile.readLines()
        .mapNotNull { regex.matchEntire(it) }
        .map {
            VideoPart(
                start = it.groups[1]!!.value.toBigDecimal().asSecondsDuration(),
                end = it.groups[2]!!.value.toBigDecimal().asSecondsDuration(),
                type = VideoPart.Type.BLACK_SEGMENT
            )
        }
        .filter {
            if (secondsLimits != null) it.end < Duration.ofSeconds(secondsLimits) else true
        }
        .toMutableList()
    return if (blackSegments.isEmpty()) null
    else {
        val scenes = blackSegments.zipWithNext { a, b ->
            VideoPart(a.end, b.start, VideoPart.Type.SCENE)
        }
        val ret = (blackSegments.zip(scenes).flatMap { (a, b) -> listOf(a, b) } + blackSegments.last()).toMutableList()
        if (ret.first().start > Duration.ZERO) {
            ret.add(0, VideoPart(Duration.ZERO, ret.first().start, VideoPart.Type.SCENE))
        }
        val duration = this.duration?.makeMillisPrecision()
        if (duration != null && ret.last().end < duration) {
            ret.add(VideoPart(ret.last().end, duration, VideoPart.Type.SCENE))
        }
        VideoParts(ret)
    }
}

fun List<VideoPart>.print() {
    forEach {
        println(it)
    }
}

data class VideoPartMatch(val input: VideoPart, val target: VideoPart) {
    init {
        require(input.type == target.type)
    }

    val type = input.type
}

fun VideoParts.matchWithTarget(targets: VideoParts): List<VideoPartMatch>? {
    if (this.parts.isNotEmpty() && this.parts.size == targets.parts.size && this.parts.first().type == this.parts.first().type) {
        val zip = this.scenes().zip(targets.scenes())
        if (zip.count { (input, target) -> (input.duration - target.duration).abs() < Duration.ofMillis(150) } / zip.size.toFloat() > 0.8) {
            return this.parts.zip(targets.parts).map { (input, target) ->
                VideoPartMatch(input, target)
            }
        }
    }
    return null
}

fun main() {
    val eng =
        InputFile.parse(File("C:\\Users\\molin\\Desktop\\MON\\ENG\\The.Big.Bang.Theory.S05E01.The.Skank.Reflex.Analysis.1080p.BluRay.x264.DTS-HDMA.5.1-LeRalouf.mkv"))
            .detectVideoParts(Duration.ofMillis(250))!!
    val ita =
        InputFile.parse(File("C:\\Users\\molin\\Desktop\\MON\\ITA\\The.Big.Bang.Theory.5x01.L.analisi.del.riflesso.della.sgualdrina.ITA-ENG.720p.DLMux.h264-DarkSideMux.mkv"))
            .detectVideoParts(Duration.ofMillis(250))?.times(StretchFactor.COMMON_25_TO_23)!!

    val max = eng.scenes().zip(ita.scenes()).map {
        (it.first.duration - it.second.duration).abs()
    }
    println(max)
    println(ita.matchWithTarget(eng)?.forEach {
        println(it)
    })
}