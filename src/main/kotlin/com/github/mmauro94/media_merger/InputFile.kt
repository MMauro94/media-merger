package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.util.toSecondsDuration
import com.github.mmauro94.media_merger.util.findWalkingUp
import com.github.mmauro94.media_merger.util.log.Logger
import com.github.mmauro94.media_merger.video_part.VideoPartsProvider
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnix
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixFileIdentification
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import java.io.File
import java.io.IOException

class InputFile private constructor(
    val inputFilesProvider: () -> InputFiles<*>,
    val file: File,
    val mkvIdentification: MkvToolnixFileIdentification,
    val ffprobeResult: FFmpegProbeResult
) {

    val inputFiles by lazy {
        inputFilesProvider().also {
            check(this@InputFile in it)
        }
    }

    private var _tracks: List<Track>? = null

    val tracks by lazy { _tracks!! }

    val duration = ffprobeResult.format.duration.toSecondsDuration()

    /**
     * Only single track video files can be a main track
     */
    private val canBeAMainFile by lazy { tracks.count { it.isVideoTrack() } == 1 }

    /**
     * Some file may be external to the main file (e.g. subtitle tracks)
     * This lazy value searches for the main file, if it exists.
     * The main file is searched starting from the directory of this file, and going up.
     * See [canBeAMainFile].
     * Only files in the same group are considered.
     * If a file named with a prefix of this file name exists, the shortest is taken, otherwise any other file can be it.
     *
     * Example:
     *  - This file: Show.S01E01.en.srt
     *  - Main file: Show.S01E01.mkv
     *
     * Another example:
     *  - This file: Show.S01E01.mp3
     *  - Main file: Show.S01E01.mkv
     *
     *  If a file n
     */
    val mainFile: InputFile? by lazy {
        //Main files cannot have main files
        if (!canBeAMainFile) {
            val inputFiles = inputFiles
                .filter { it.canBeAMainFile } //Filter only files that can actually be a main file
                .toMutableList()

            file.parentFile.findWalkingUp(true) { dir ->
                val filesToAnalyze = inputFiles.filter { it.file.parentFile == dir } //Keep only files inside the directory dir

                filesToAnalyze
                    .filter { file.nameWithoutExtension.startsWith(dir.nameWithoutExtension) } //Try to find files that are a prefix of me
                    .minByOrNull { it.file.nameWithoutExtension.length } //And take the shortest one
                    ?: filesToAnalyze.firstOrNull() //If not found, take the first file that can be a main file if it exists
                //When the expression returns null, we go up one level
            }
        } else null
    }

    val framerate by lazy { detectFramerate() ?: mainFile?.detectFramerate() }

    val videoParts: VideoPartsProvider? by lazy {
        val videoTrack = tracks.singleOrNull { it.isVideoTrack() }
        (if (videoTrack != null) {
            VideoPartsProvider(this, Main.config.ffmpeg.blackdetect, videoTrack.startTime)
        } else null) ?: mainFile?.videoParts
    }

    override fun toString(): String = file.name

    override fun equals(other: Any?) = other is InputFile && other.file == file

    override fun hashCode() = file.hashCode()

    class ParseException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        private fun new(
            inputFilesProvider: () -> InputFiles<*>,
            file: File,
            mkvIdentification: MkvToolnixFileIdentification,
            ffprobeResult: FFmpegProbeResult,
            logger: Logger
        ): InputFile {
            val input = InputFile(inputFilesProvider, file, mkvIdentification, ffprobeResult)
            input._tracks = tracks(input, logger)
            return input
        }

        fun parse(inputFilesProvider: () -> InputFiles<*>, file: File, logger : Logger): InputFile {
            val mkvId = MkvToolnix.identify(file)
            if (!mkvId.container.recognized) {
                throw ParseException("mkvmerge doesn't recognize the file ${file.absolutePath}")
            }
            val probeResult = try {
                FFprobe().probe(file.absolutePath)
            } catch (e: IOException) {
                throw ParseException("Error ffprobing file ${file.absolutePath}", e)
            }
            return new(inputFilesProvider, file, mkvId, probeResult, logger)
        }

        private fun tracks(inputFile: InputFile, logger: Logger): List<Track> {
            val ret = ArrayList<Track>(inputFile.mkvIdentification.tracks.size)
            inputFile.mkvIdentification.tracks.forEach { t ->
                val ff = inputFile.ffprobeResult.streams.find { it.index.toLong() == t.id }
                if (ff != null && t.id == ff.index.toLong()) {
                    Track.from(inputFile, t, ff, logger)?.let {
                        ret.add(it)
                    }
                }
            }
            return ret
        }
    }
}