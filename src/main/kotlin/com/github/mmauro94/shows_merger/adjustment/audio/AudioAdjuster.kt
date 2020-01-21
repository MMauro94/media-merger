package com.github.mmauro94.shows_merger.adjustment.audio

import com.github.mmauro94.shows_merger.InputFile
import com.github.mmauro94.shows_merger.Track
import com.github.mmauro94.shows_merger.adjustment.AbstractAdjustment
import com.github.mmauro94.shows_merger.adjustment.TrackAdjustmer
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

abstract class AudioAdjustmer<T:AbstractAdjustment>(adjustment: T) : TrackAdjustmer<T>(adjustment) {

    protected var targetDuration : Duration? = null

    override fun adjust(outputFile: File, inputTrack: Track): Track? {
        val res = when {
            outputFile.exists() -> true
            else -> {
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
                                "TODO", //TODO
                                if (percentage != null) "%.0f%%".format(percentage) else "N/A",
                                FFmpegUtils.toTimecode(prg.out_time_ns, TimeUnit.NANOSECONDS),
                                prg.speed
                            )
                        )
                    }.run()
                }
                true
            }
        }
        return if(res) {
            val track = InputFile.parse(outputFile).tracks.single()
            require(track.isAudioTrack())
            return track
        } else null
    }
}