package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnix
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixFileIdentification
import com.github.mmauro94.shows_merger.video_part.detectVideoParts
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import java.io.File
import java.time.Duration

private val BLACK_SEGMENTS_MIN_DURATION = Duration.ofMillis(250)!!
private const val BLACK_SEGMENTS_LIMITED_SECONDS = 30L

class InputFile private constructor(
    val file: File,
    val mkvIdentification: MkvToolnixFileIdentification,
    val ffprobeResult: FFmpegProbeResult
) {
    private var _tracks: List<Track>? = null

    val tracks by lazy { _tracks!! }

    val duration = ffprobeResult.format.duration.asSecondsDuration()

    val framerate by lazy { detectFramerate() }

    val videoParts by lazy {
        if (tracks.any { it.isVideoTrack() }) {
            detectVideoParts(BLACK_SEGMENTS_MIN_DURATION)
        } else null
    }

    val videoPartsLimited by lazy {
        if (tracks.any { it.isVideoTrack() }) {
            detectVideoParts(BLACK_SEGMENTS_MIN_DURATION, BLACK_SEGMENTS_LIMITED_SECONDS)
        } else null
    }

    override fun toString(): String = file.name

    override fun equals(other: Any?) = other is InputFile && other.file == file

    override fun hashCode() = file.hashCode()

    companion object {
        private fun new(
            file: File,
            mkvIdentification: MkvToolnixFileIdentification,
            ffprobeResult: FFmpegProbeResult
        ): InputFile {
            val input = InputFile(file, mkvIdentification, ffprobeResult)
            input._tracks = tracks(input)
            return input
        }

        fun parse(file: File) = new(file, MkvToolnix.identify(file), FFprobe().probe(file.absolutePath))

        private fun tracks(inputFile: InputFile): List<Track> {
            val ret = ArrayList<Track>(inputFile.mkvIdentification.tracks.size)
            inputFile.mkvIdentification.tracks.forEach { t ->
                val ff = inputFile.ffprobeResult.streams.find { it.index.toLong() == t.id }
                if (ff != null && t.id == ff.index.toLong()) {
                    Track.from(inputFile, t, ff)?.let {
                        ret.add(it)
                    }
                }
            }
            return ret
        }
    }
}