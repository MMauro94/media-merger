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

/**
 * Represents an operation (adjustment) to apply to an audio track
 * @param T the type of the class defining the parameters for the operation
 */
abstract class AbstractAudioAdjustment<T>(
    val adjustment: T
) {
    /**
     * The duration that the output file should have.
     * Should be filled in the [prepare] function, if available.
     * Used only for displaying progress.
     */
    protected var targetDuration: Duration? = null

    /**
     * List of parameters to concat to the output audio file.
     * Should contain the type of operation and its params in a simplified form.
     */
    protected abstract val outputConcat: List<String>

    /**
     * Whether the adjustment should be done.
     * This should returns false for adjustment that make no send (e.g. move the audio track by 0 seconds)
     */
    protected abstract fun shouldAdjust(): Boolean

    /**
     * Called before other methods by the [adjust] function.
     * Should take care of initializing common things needed by [fillBuilder] and [fillOutputBuilder].
     * Should also take care of changing [targetDuration], if needed.
     */
    protected open fun prepare(inputTrack: Track) {}

    /**
     * Changes the [FFmpegBuilder] with adjustment specific options
     * @param inputTrack the track that is being edited
     */
    protected abstract fun FFmpegBuilder.fillBuilder(inputTrack: Track)

    /**
     * Changes the [FFmpegOutputBuilder] with adjustment specific options
     * @param inputTrack the track that is being edited
     */
    protected abstract fun FFmpegOutputBuilder.fillOutputBuilder(inputTrack: Track)

    /**
     * Adjusts the given [inputTrack] with the [adjustment]
     *
     * Returns a [Track] instance representing the track of the newly adjusted audio file,
     * or null if the adjustment didn't have to be done.
     *
     * If a file with the same name is already present, the actual adjustment is not done
     */
    fun adjust(inputTrack: Track, progress: String): Track? {
        val outputFile = File(
            inputTrack.file.parentFile,
            inputTrack.file.nameWithoutExtension + "_" + inputTrack.id + "__" + outputConcat.joinToString(separator = "__") + "." + inputTrack.audioExtension
        )

        val res = when {
            outputFile.exists() -> true
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
                true
            }
            else -> false
        }
        return if(res) {
            val track = InputFile.parse(outputFile).tracks.single()
            require(track.isAudioTrack())
            return track
        } else null
    }
}

