package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.group.Grouper
import com.github.mmauro94.media_merger.util.Reporter
import com.github.mmauro94.media_merger.util.add
import com.github.mmauro94.media_merger.util.log.Logger
import com.github.mmauro94.media_merger.util.log.withPrependDebug
import com.github.mmauro94.media_merger.util.sortWithPreferences
import java.io.File

data class InputFiles<G : Group<G>>(
    val group: G,
    val inputFiles: List<InputFile>
) : Iterable<InputFile>, Comparable<InputFiles<G>> {

    override fun iterator() = inputFiles.iterator()

    fun outputName() = group.outputName()
    val debugFile = group.debugFile

    fun allTracks() = sequence {
        inputFiles.forEach {
            yieldAll(it.tracks)
        }
    }

    fun selectVideoTrack(logger: Logger): Track? {
        return allTracks()
            .filter { it.isVideoTrack() }
            .sortWithPreferences({
                it.mkvTrack.codec.contains("265")
            })
            .sortedByDescending {
                it.normalizedPixelHeight
            }
            .firstOrNull().apply {
                if (this == null) {
                    logger.warn("No video tracks found for group $group")
                }
            }
    }

    companion object {

        val VIDEO_EXTENSIONS = listOf("avi", "mp4", "mkv", "mov", "ogv", "mpg", "mpeg", "m4v")
        val AUDIO_EXTENSIONS = listOf("mp3", "ac3", "aac", "flac", "m4a", "oga")
        val SUBTITLES_EXTENSIONS = listOf("srt", "ssa", "idx", "sub")
        val EXTENSIONS_TO_IDENTIFY = VIDEO_EXTENSIONS + AUDIO_EXTENSIONS + SUBTITLES_EXTENSIONS

        fun <G : Group<G>> detect(grouper: Grouper<G>, dir: File, reporter: Reporter): List<InputFiles<G>> {
            val groupedFiles = grouper.detectGroups(dir, reporter)

            val inputFiles = mutableMapOf<G, InputFiles<G>>()
            val ret = HashMap<G, MutableList<InputFile>>()

            var i = 0
            val max = groupedFiles.values.sumOf { it.size }
            for ((ei, files) in groupedFiles) {
                val groupRep = reporter.withDebug(ei.debugFile)
                files.forEach { f ->
                    groupRep.progress.discrete(i, max, "Identifying ${f.name}")
                    try {
                        ret.add(ei, InputFile.parse({ inputFiles.getValue(ei) }, f, groupRep.log))
                    } catch (e: InputFile.ParseException) {
                        groupRep.log.err("Unable to parse file: ${e.message}")
                    }
                    i++
                }
                if(Main.test) {
                    reporter.log.warn("Test mode active, only first group will be analyzed")
                    break
                }
            }
            reporter.progress.discrete(i, max, "Identifying complete")

            for ((key, value) in ret) {
                inputFiles[key] = InputFiles(key, value)
            }
            return inputFiles.values.toList()
        }
    }

    fun selectTracks(logger: Logger): SelectedTracks<G>? {
        val videoTrack = selectVideoTrack(logger)
        if (videoTrack?.durationOrFileDuration == null) {
            logger.warn("Video track $videoTrack without duration")
            return null
        }
        logger.debug("--- TRACK SELECTION FOR $group ---")
        logger.debug("Video track: $videoTrack")

        val languageTracks = allTracks()
            .groupBy { it.language } //Group by language
            .mapValues { (lang, tracks) ->
                logger.debug("For language $lang:")
                logger.withPrependDebug("   ") {
                    val audioTrack = tracks
                        .asSequence()
                        .filter { it.isAudioTrack() }
                        .sortWithPreferences({
                            it.mkvTrack.codec.contains("DTS", true)
                        }, {
                            it.mkvTrack.codec.contains("AC-3", true)
                        }, {
                            it.mkvTrack.codec.contains("AAC", true)
                        }, {
                            it.mkvTrack.codec.contains("FLAC", true)
                        }, {
                            it.isOnItsFile
                        }, {
                            sameFile(it, videoTrack)
                        })
                        .also {
                            debug("Sorted audio tracks:")
                            it.forEach { t ->
                                debug("  - $t")
                            }
                        }
                        .firstOrNull()

                    val subtitleTracks = tracks
                        .asSequence()
                        .filter { it.isSubtitlesTrack() }
                        .sortWithPreferences({
                            it.mkvTrack.properties?.textSubtitles == true
                        }, {
                            it.isOnItsFile
                        }, {
                            it.mkvTrack.properties?.trackName?.contains("SDH", ignoreCase = true) == true
                        }, {
                            sameFile(it, videoTrack)
                        })

                    val subtitleTrack = subtitleTracks
                        .filter { !it.isForced }
                        .also {
                            debug("Sorted subtitle tracks:")
                            it.forEach { t ->
                                debug("  - $t")
                            }
                        }
                        .firstOrNull()

                    val forcedSubtitleTrack = subtitleTracks
                        .filter { it.isForced }
                        .also {
                            debug("Sorted forced subtitle tracks:")
                            it.forEach { t ->
                                debug("  - $t")
                            }
                        }
                        .firstOrNull()

                    if (audioTrack == null && subtitleTrack == null) {
                        debug("Neither audio track or subtitle track for this language")
                        null
                    } else {
                        SelectedTracks.LanguageTracks().apply {
                            this.audioTrack.track = audioTrack
                            this.subtitleTrack.track = subtitleTrack
                            this.forcedSubtitleTrack.track = forcedSubtitleTrack
                        }
                    }
                }
            }
            .filterValues { it != null }
            .mapValues { it.value as SelectedTracks.LanguageTracks }
            .toMap()

        return SelectedTracks(group, videoTrack, languageTracks)
    }


    override fun compareTo(other: InputFiles<G>) = group.compareTo(other.group)
}