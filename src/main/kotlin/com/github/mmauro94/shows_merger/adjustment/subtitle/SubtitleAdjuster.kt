package com.github.mmauro94.shows_merger.adjustment.subtitle

import com.github.mmauro94.shows_merger.Track
import com.github.mmauro94.shows_merger.adjustment.Adjustment
import com.github.mmauro94.shows_merger.adjustment.TrackAdjuster
import com.github.mmauro94.shows_merger.subtitles.Subtitle
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
 * Base instance of [TrackAdjuster] for all adjustments to be made on a subtitle track.
 */
abstract class SubtitleAdjuster<T>(
    track: Track,
    adjustment: Adjustment<T>,
    outputFile: File
) : TrackAdjuster<T>(track, adjustment, outputFile) {

    init {
        require(track.isSubtitlesTrack())
    }

    abstract fun applyTransformations(subtitle: Subtitle<*>): Subtitle<*>

    override fun doAdjust(): Boolean {
        val file = track.fileOrExtracted()
        val subtitle = Subtitle.parse(file)
        return if(subtitle != null) {
            applyTransformations(subtitle).save(outputFile)
            true
        } else false
    }
}