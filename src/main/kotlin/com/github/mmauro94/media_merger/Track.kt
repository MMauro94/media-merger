package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.util.Reporter
import com.github.mmauro94.media_merger.util.find
import com.github.mmauro94.media_merger.util.findWalkingUp
import com.github.mmauro94.media_merger.util.log.Logger
import com.github.mmauro94.media_merger.util.log.withPrependDebug
import com.github.mmauro94.media_merger.util.toSecondsDuration
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnix
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixTrack
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixTrackType
import net.bramp.ffmpeg.probe.FFmpegStream
import java.io.File
import java.time.Duration
import java.util.*

class Track(
    val inputFile: InputFile,
    val mkvTrack: MkvToolnixTrack,
    val ffprobeStream: FFmpegStream,
    val language: MkvToolnixLanguage
) {

    val id = mkvTrack.id

    val file = inputFile.file

    val duration = ffprobeStream.duration.toSecondsDuration()

    val durationOrFileDuration = duration ?: inputFile.duration

    val isOnItsFile by lazy { inputFile.tracks.size == 1 }

    val startTime by lazy { ffprobeStream.start_time.toSecondsDuration(Duration.ZERO)!! }

    val normalizedPixelHeight: Int? by lazy {
        mkvTrack.properties?.pixelDimensions?.height?.let { height ->
            val heights = intArrayOf(480, 720, 1080, 2160)
            heights.firstOrNull { height in (it - 20)..(it + 20) } ?: height
        }
    }

    private val extensionFromCodecId = mkvTrack.properties?.codecId.let { c ->
        when {
            c == null -> null
            c.matches("A_AAC/MPEG([24])/.+".toRegex()) -> "aac"
            else -> when (c) {
                //Audio
                "A_DTS" -> "dts"
                "A_AC3", " A_EAC3" -> "ac3"
                "A_FLAC" -> "flac"
                "A_MPEG/L2" -> "mp2"
                "A_MPEG/L3" -> "mp3"
                "A_OPUS" -> "opus"
                "A_PCM/INT/LIT", "A_PCM/INT/BIG" -> "wav"
                "A_VORBIS" -> "ogg"
                "A_WAVPACK4" -> "wv"
                "A_TRUEHD", "A_MLP" -> "mlp"
                "A_ALAC" -> "caf"

                //Subs
                "S_TEXT/UTF8", "S_TEXT/ASCII" -> "srt"
                "S_HDMV/PGS" -> "sup"
                "S_VOBSUB" -> "sub"
                "S_TEXT/USF" -> "usf"
                "S_TEXT/WEBVTT" -> "webvtt"
                else -> null
            }
        }
    }

    private val extensionFromCodec = mkvTrack.codec.let { c ->
        when (c) {
            "AC-3" -> "ac3"
            "AAC" -> "aac"
            "MP3" -> "mp3"
            "FLAC" -> "flac"
            "Vorbis" -> "ogg"
            "Opus" -> "opus"
            "PCM" -> "wav"
            "SubRip/SRT" -> "srt"
            else -> null
        }
    }

    val extension = extensionFromCodecId ?: extensionFromCodec ?: "unknown"

    val isForced by lazy {
        mkvTrack.isForced() == true ||
                mkvTrack.properties?.trackName?.contains("forced", ignoreCase = true) == true ||
                (isOnItsFile && inputFile.file.findWalkingUp(false) {
                    it.name.contains("forced", ignoreCase = true)
                } ?: false) ||
                (isOnItsFile && inputFile.file.length() in 1..(1024 * 10)) //< 10KiB
    }

    fun isAudioTrack() =
        mkvTrack.type == MkvToolnixTrackType.audio && ffprobeStream.codec_type == FFmpegStream.CodecType.AUDIO

    fun isVideoTrack() =
        mkvTrack.type == MkvToolnixTrackType.video && ffprobeStream.codec_type == FFmpegStream.CodecType.VIDEO

    fun isSubtitlesTrack() = mkvTrack.type == MkvToolnixTrackType.subtitles

    fun fileOrExtracted(reporter: Reporter): File {
        return if (isOnItsFile) inputFile.file
        else {
            val extractedFile =
                File(inputFile.file.parentFile, inputFile.file.nameWithoutExtension + "@extracted@track$id.$extension")
            if (!extractedFile.exists()) {
                reporter.progress.indeterminate("Extracting $this")
                MkvToolnix.extract(inputFile.file).extractTracks {
                    addTrack(mkvTrack, extractedFile)
                }.executeAndPrint()
                reporter.progress.indeterminate("$this extracted successfully")
            }
            extractedFile
        }
    }

    override fun equals(other: Any?) = other is Track && other.inputFile == inputFile && other.id == id
    override fun hashCode() = Objects.hash(inputFile, id)

    override fun toString() = "${mkvTrack.type} track $id of file ${file.name}"

    companion object {
        fun from(
            inputFile: InputFile,
            mkvTrack: MkvToolnixTrack,
            ffprobeStream: FFmpegStream,
            logger: Logger
        ): Track? {
            val file = mkvTrack.fileIdentification.fileName

            var language = mkvTrack.properties?.language
            if ((language == null || language.isUndefined()) && mkvTrack.fileIdentification.tracks.first { it.type == mkvTrack.type } == mkvTrack) {
                logger.debug("Detecting language for track ${mkvTrack.id} of file ${file.name} from filename")
                language = file.findLanguage(logger.prependDebug("  "))
            }
            return if (language == null) {
                logger.warn("Track ${mkvTrack.id} of file ${file.name} skipped because of no language")
                null
            } else {
                logger.debug("Detected language for track ${mkvTrack.id} of file ${file.name} is $language")
                Track(inputFile, mkvTrack, ffprobeStream, language)
            }
        }

        private fun File.findLanguage(logger: Logger): MkvToolnixLanguage? {
            return findWalkingUp(false) { f ->
                val split = f.nameWithoutExtension.split(Regex("(\\s+|[_.\\-])")).filterNot { it.isBlank() }
                logger.debug("Name split: $split")
                val map = split
                    .groupingBy { s ->
                        val lang = MkvToolnixLanguage.find(s.lowercase())
                        if (Main.config.languagesDetectWhitelist.isEmpty() || lang in Main.config.languagesDetectWhitelist) {
                            logger.debug("File part '$s' matching with language $lang")
                            lang
                        } else null
                    }
                    .eachCount()
                    .filterKeys { it != null }
                val max = map.values.maxOrNull()
                map.entries.singleOrNull { it.value == max }?.key
            }
        }
    }
}

fun sameFile(track1: Track, track2: Track) = track1.file.absolutePath == track2.file.absolutePath