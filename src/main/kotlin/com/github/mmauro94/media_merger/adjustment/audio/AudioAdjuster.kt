package com.github.mmauro94.media_merger.adjustment.audio

import com.github.mmauro94.media_merger.Track
import com.github.mmauro94.media_merger.adjustment.Adjustment
import com.github.mmauro94.media_merger.adjustment.TrackAdjuster
import com.github.mmauro94.media_merger.util.Reporter
import com.github.mmauro94.media_merger.util.progress.ProgressHandler
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import java.io.File
import java.time.Duration

/**
 * Base instance of [TrackAdjuster] for all adjustments to be made on an audio track.
 */
abstract class AudioAdjuster<T>(
    track: Track,
    adjustment: Adjustment<T>,
    outputFile: File
) : TrackAdjuster<T>(track, adjustment, outputFile) {

    init {
        require(track.isAudioTrack())
    }

    /**
     * Changes the [FFmpegBuilder] with adjustment specific options
     */
    protected abstract fun FFmpegBuilder.fillBuilder()

    /**
     * Changes the [FFmpegOutputBuilder] with adjustment specific options
     */
    protected abstract fun FFmpegOutputBuilder.fillOutputBuilder()

    /**
     * The duration the output file should have, can be null if unknown. Used only for progress
     */
    protected abstract val targetDuration: Duration?

    override fun doAdjust(reporter: Reporter): Boolean {
        val builder = FFmpegBuilder()
            .setInput(track.file.absolutePath)
            .apply { fillBuilder() }
            .addOutput(outputFile.absolutePath)
            .apply { fillOutputBuilder() }
            .done()
        FFmpegExecutor(FFmpeg(), FFprobe()).apply {
            createJob(builder) { prg ->
                reporter.progress.ffmpeg(prg, targetDuration)
            }.run()
        }
        return true
    }
}