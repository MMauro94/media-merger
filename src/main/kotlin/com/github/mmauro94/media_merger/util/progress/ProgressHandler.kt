package com.github.mmauro94.media_merger.util.progress

import net.bramp.ffmpeg.FFmpegUtils
import java.time.Duration
import java.util.concurrent.TimeUnit
import net.bramp.ffmpeg.progress.Progress as FFmpegProgress

interface ProgressHandler : ProgressSplitter<ProgressHandler> {

    fun handle(main: ProgressWithMessage, vararg previouses: ProgressWithMessage)

    override val baseHandler get() = this

    override fun create(progressHandler: ProgressHandler) = progressHandler


    fun handle(progress: Progress, message: String?) = this.handle(
        ProgressWithMessage(progress, message)
    )

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

