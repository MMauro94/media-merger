package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixTrack
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixTrackType
import net.bramp.ffmpeg.probe.FFmpegStream
import java.util.*

class Track(
    val inputFile: InputFile,
    val mkvTrack: MkvToolnixTrack,
    val ffprobeStream: FFmpegStream,
    val language: MkvToolnixLanguage
) {

    val id = mkvTrack.id

    val file = inputFile.file

    val duration = ffprobeStream.duration.asSecondsDuration()

    val durationOrFileDuration = duration ?: inputFile.duration

    val isForced =
        mkvTrack.isForced() == true ||
                mkvTrack.properties?.trackName?.contains("forced") == true ||
                mkvTrack.fileIdentification.fileName.parentFile.name.contains("forced")

    val isOnItsFile by lazy { inputFile.tracks.size == 1 }

    fun isAudioTrack() =
        mkvTrack.type == MkvToolnixTrackType.audio && ffprobeStream.codec_type == FFmpegStream.CodecType.AUDIO

    fun isVideoTrack() =
        mkvTrack.type == MkvToolnixTrackType.video && ffprobeStream.codec_type == FFmpegStream.CodecType.VIDEO

    fun isSubtitlesTrack() = mkvTrack.type == MkvToolnixTrackType.subtitles

    override fun equals(other: Any?) = other is Track && other.inputFile == inputFile && other.id == id
    override fun hashCode() = Objects.hash(inputFile, id)

    override fun toString() = "Track $id of file ${file.name}"

    companion object {
        fun from(inputFile: InputFile, mkvTrack: MkvToolnixTrack, ffprobeStream: FFmpegStream): Track? {
            val file = mkvTrack.fileIdentification.fileName

            var language = mkvTrack.properties?.language
            if ((language == null || language.isUndefined()) && mkvTrack.fileIdentification.tracks.count { it.type == mkvTrack.type } == 1) {
                language = file.name.findLanguage() ?: file.parentFile.name.findLanguage()
            }
            return if (language == null) {
                System.err.println("Track ${mkvTrack.id} of file ${file.name} skipped because of no language")
                null
            } else {
                Track(inputFile, mkvTrack, ffprobeStream, language)
            }
        }

        private fun String.findLanguage() : MkvToolnixLanguage? {
            return split(Regex("(\\s+|_)")).asSequence()
                .filter { it.length == 3 }
                .map { MkvToolnixLanguage.all[it.toLowerCase()] }
                .filterNotNull()
                .singleOrNull()
        }
    }
}