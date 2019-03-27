package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnix
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixFileIdentification
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import java.io.File

class InputFile private constructor(
    val file: File,
    val mkvIdentification: MkvToolnixFileIdentification,
    val ffprobeResult: FFmpegProbeResult
) {
    private var _tracks: List<Track>? = null

    val tracks by lazy { _tracks!! }

    val duration = ffprobeResult.format.duration.asSecondsDuration()

    override fun toString() = file.name!!

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

        fun parse(file: File) = InputFile.new(file, MkvToolnix.identify(file), FFprobe().probe(file.absolutePath))

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