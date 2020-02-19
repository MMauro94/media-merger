package com.github.mmauro94.media_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnix
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixFileIdentification
import com.github.mmauro94.media_merger.util.asSecondsDuration
import com.github.mmauro94.media_merger.video_part.VideoPartsProvider
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import java.io.File
import java.io.IOException
import java.time.Duration

private val BLACK_SEGMENTS_MIN_DURATION = Duration.ofMillis(100)!!

class InputFile private constructor(
    val file: File,
    val mkvIdentification: MkvToolnixFileIdentification,
    val ffprobeResult: FFmpegProbeResult
) {
    private var _tracks: List<Track>? = null

    val tracks by lazy { _tracks!! }

    val duration = ffprobeResult.format.duration.asSecondsDuration()

    /**
     * Some file may be external to the main file (e.g. subtitle tracks)
     * This lazy value searches for the main file, if it exists.
     * A main file should be named with a prefix of this file name.
     *
     * Example:
     *  - This file: Show.S01E01.en.srt
     *  - Main file: Show.S01E01.mkv
     *
     * Another example:
     *  - This file: Show.S01E01.mp3
     *  - Main file: Show.S01E01.mkv
     */
    val mainFile by lazy {
        //Only non-video single-track files can have a main file
        if (tracks.size == 1 && !tracks.single().isVideoTrack()) {
            val f = file.parentFile
                .listFiles() //List all the files in the same directory
                ?.filter { file.nameWithoutExtension.startsWith(it.nameWithoutExtension) } //Keep only files with the right prefix
                ?.filterNot { it.name == file.name } //Remove itself
                ?.sortedBy { it.name } //Sorts so that we avoid loops (two files with the same name, each main of each other)
                ?.minBy { it.nameWithoutExtension.length } //Take the one with the shorter name
            if (f != null) {
                //If we found a worthy file, it should have already been parsed, so we search it in the input files
                Main.inputFilesDetector.inputFiles()
                    .flatMap { it.inputFiles }
                    .find { it.file.absolutePath == f.absolutePath }
            } else null
        } else null
    }

    val framerate by lazy { detectFramerate() ?: mainFile?.detectFramerate() }

    val videoParts: VideoPartsProvider? by lazy {
        val videoTrack = tracks.singleOrNull { it.isVideoTrack() }
        (if(videoTrack != null) {
            VideoPartsProvider(this, BLACK_SEGMENTS_MIN_DURATION, videoTrack.startTime)
        } else null) ?: mainFile?.videoParts
    }

    override fun toString(): String = file.name

    override fun equals(other: Any?) = other is InputFile && other.file == file

    override fun hashCode() = file.hashCode()

    class ParseException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

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

        fun parse(file: File): InputFile {
            val mkvId = MkvToolnix.identify(file)
            if (!mkvId.container.recognized) {
                throw ParseException("mkvmerge doesn't recognize the file ${file.absolutePath}")
            }
            val probeResult = try {
                FFprobe().probe(file.absolutePath)
            } catch (e: IOException) {
                throw ParseException("Error ffprobing file ${file.absolutePath}", e)
            }
            return new(file, mkvId, probeResult)
        }

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