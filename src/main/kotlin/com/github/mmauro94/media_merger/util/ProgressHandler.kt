package com.github.mmauro94.media_merger.util

import net.bramp.ffmpeg.FFmpegUtils
import org.fusesource.jansi.Ansi
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.properties.Delegates
import net.bramp.ffmpeg.progress.Progress as FFmpegProgress


abstract class ProgressHandler {

    abstract fun handle(main: ProgressWithMessage, vararg previouses: ProgressWithMessage)

    fun split(current: Int, max: Int, message: String?): ProgressHandler {
        require(current < max)
        return split(
            globalProgressSpan = ProgressSpan(
                start = Progress.of(current, max),
                end = Progress.of(current + 1, max)
            ),
            message = message
        )
    }

    fun split(frozenGlobalProgress: Progress, message: String?): ProgressHandler {
        return split(ProgressSpan(frozenGlobalProgress, frozenGlobalProgress), message)
    }

    fun split(globalRatioStart: Float, globalRatioEnd: Float, message: String?): ProgressHandler {
        require(globalRatioStart <= globalRatioEnd)
        return split(ProgressSpan(Progress(globalRatioStart), Progress(globalRatioEnd)), message)
    }

    fun split(globalProgressSpan: ProgressSpan, message: String?): ProgressHandler {
        val outerHandler = this

        return object : ProgressHandler() {
            override fun handle(main: ProgressWithMessage, vararg previouses: ProgressWithMessage) {
                outerHandler.handle(
                    ProgressWithMessage(
                        progress = globalProgressSpan.interpolate(main.progress.ratio ?: 0f),
                        message = message
                    ),
                    main,
                    *previouses
                )
            }
        }
    }


    fun handle(progress: Progress, message: String?) = this.handle(ProgressWithMessage(progress, message))

    fun indeterminate(message: String?) = handle(Progress.INDETERMINATE, message)

    fun discrete(index: Int, max: Int, message: String?) = handle(Progress.of(index, max), message)

    fun ratio(ratio: Float, message: String?) = handle(Progress(ratio), message)

    fun finished(message: String?) = ratio(1f, message)

    fun ffmpeg(ffmpegProgress: FFmpegProgress, duration: Duration?, timecodeOffset: Duration = Duration.ZERO) {
        val timecode = FFmpegUtils.toTimecode(
            ffmpegProgress.out_time_ns + timecodeOffset.toNanos(),
            TimeUnit.NANOSECONDS
        )
        val message = "Currently at %s, speed %.2fx".format(timecode, ffmpegProgress.speed)

        if (duration == null) {
            indeterminate(message)
        } else {
            ratio((ffmpegProgress.out_time_ns / duration.toNanos().toFloat()).coerceIn(0f, 1f), message)
        }
    }
}

class ConsoleProgressHandler : ProgressHandler(), AutoCloseable {

    companion object {
        val OUT = System.out
    }

    private var closed = false
    private var first = true
    private var maxSize = 0

    private fun handleSingle(progress: ProgressWithMessage) {
        OUT.print(Ansi.ansi().eraseLine(Ansi.Erase.FORWARD))
        if (progress.progress.ratio != null) {
            OUT.print(Ansi.ansi().fgBrightBlue().a((progress.progress.ratio * 100).toInt().toString().padStart(3) + '%'))
        } else {
            OUT.print(Ansi.ansi().fgBrightBlue().a("----"))
        }
        if (progress.message != null) {
            OUT.print(Ansi.ansi().fgDefault().a(", ${progress.message}").reset())
        }
        OUT.println()
    }

    override fun handle(main: ProgressWithMessage, vararg previouses: ProgressWithMessage) {
        if (!first) {
            OUT.print(Ansi.ansi().restoreCursorPosition())
            repeat(max(0, maxSize - previouses.size)) {
                OUT.println(Ansi.ansi().eraseLine(Ansi.Erase.FORWARD))
            }
        } else {
            OUT.print(Ansi.ansi().saveCursorPosition())
            first = false
        }
        maxSize = max(maxSize, previouses.size)
        previouses.reversedArray().forEach(::handleSingle)
        handleSingle(main)
    }


    override fun close() {
        check(!closed)
        OUT.println()
    }
}