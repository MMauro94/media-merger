package com.github.mmauro94.shows_merger.audio_adjustment

import com.github.mmauro94.shows_merger.InputFile
import com.github.mmauro94.shows_merger.Track
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

abstract class AbstractAudioAdjustment<T>(
    val adjustment: T
) {
    protected var targetDuration: Duration? = null

    protected abstract val outputConcat: List<String>


    protected abstract fun shouldAdjust(): Boolean

    protected open fun prepare(inputTrack: Track) {}

    protected abstract fun FFmpegBuilder.fillBuilder(inputTrack: Track)

    protected abstract fun FFmpegOutputBuilder.fillOutputBuilder(inputTrack: Track)

    fun adjust(inputTrack: Track, progress: String): Track? {
        val outputFile = File(
            inputTrack.file.parentFile,
            inputTrack.file.nameWithoutExtension + "_" + inputTrack.id + "__" + outputConcat.joinToString(separator = "__") + "." + inputTrack.audioExtension
        )

        return when {
            outputFile.exists() -> null
            shouldAdjust() -> {
                prepare(inputTrack)
                val builder = FFmpegBuilder()
                    .setInput(inputTrack.file.absolutePath)
                    .apply { fillBuilder(inputTrack) }
                    .addOutput(outputFile.absolutePath)
                    .apply { fillOutputBuilder(inputTrack) }
                    .done()
                FFmpegExecutor(FFmpeg(), FFprobe()).apply {
                    createJob(builder) { prg ->
                        val targetTotalNanos = targetDuration?.toNanos()?.toDouble()
                        val percentage = if (targetTotalNanos != null) {
                            (prg.out_time_ns / targetTotalNanos) * 100.0
                        } else null

                        println(
                            String.format(
                                "[%s, %s] %s, speed:%.2fx",
                                progress,
                                if (percentage != null) "%.0f%%".format(percentage) else "N/A",
                                FFmpegUtils.toTimecode(prg.out_time_ns, TimeUnit.NANOSECONDS),
                                prg.speed
                            )
                        )
                    }.run()
                }
                InputFile.parse(outputFile).tracks.single()
            }
            else -> null
        }
    }
}

