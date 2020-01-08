package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnix
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixFileIdentification
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import java.io.File
import java.time.Duration

class InputFile private constructor(
    val file: File,
    val mkvIdentification: MkvToolnixFileIdentification,
    val ffprobeResult: FFmpegProbeResult
) {
    private var _tracks: List<Track>? = null

    val tracks by lazy { _tracks!! }

    val duration = ffprobeResult.format.duration.asSecondsDuration()

    val framerate by lazy { detectFramerate() }

    val blackSegments by lazy {
        if (tracks.any { it.isVideoTrack() }) {
            detectBlackSegments(Duration.ofMillis(250), Duration.ofSeconds(30))
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