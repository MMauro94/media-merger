package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.group.Grouper
import com.github.mmauro94.media_merger.util.*
import java.io.File

data class InputFiles<G : Group<G>>(
    val group: G,
    val inputFiles: List<InputFile>
) : Iterable<InputFile>, Comparable<InputFiles<G>> {

    override fun iterator() = inputFiles.iterator()

    fun outputName() = group.outputName()

    fun allTracks() = sequence {
        inputFiles.forEach {
            yieldAll(it.tracks)
        }
    }

    companion object {

        val VIDEO_EXTENSIONS = listOf("avi", "mp4", "mkv", "mov", "ogv", "mpg", "mpeg", "m4v")
        val AUDIO_EXTENSIONS = listOf("mp3", "ac3", "aac", "flac", "m4a", "oga")
        val SUBTITLES_EXTENSIONS = listOf("srt", "ssa", "idx", "sub")
        val EXTENSIONS_TO_IDENTIFY = VIDEO_EXTENSIONS + AUDIO_EXTENSIONS + SUBTITLES_EXTENSIONS

        fun <G : Group<G>> detect(grouper: Grouper<G>, dir: File, progress: ProgressHandler): List<InputFiles<G>> {
            progress.indeterminate("Listing files...")
            val allFiles = dir.walkTopDown().toList()
            progress.indeterminate("Grouping files...")
            val groupedFiles = allFiles
                .filter { it.extension in EXTENSIONS_TO_IDENTIFY }
                .filterNot { it.name.contains("@adjusted") || it.name.contains("@extracted") }
                .groupBy { grouper.detectGroup(it.name) }
                .filterKeys { it != null }


            val inputFiles = mutableMapOf<G, InputFiles<G>>()
            val ret = HashMap<G, MutableList<InputFile>>()

            var i = 0
            val max = groupedFiles.values.sumBy { it.size }
            groupedFiles.forEach { (ei, files) ->
                check(ei != null)
                files.forEach { f ->
                    progress.discrete(i, max, "Identifying ${f.name}")
                    try {
                        ret.add(ei, InputFile.parse({ inputFiles.getValue(ei) }, f))
                    } catch (e: InputFile.ParseException) {
                        System.err.println("Unable to parse file: ${e.message}")
                    }
                    i++
                }
            }
            progress.discrete(i, max, "Identifying complete")

            for ((key, value) in ret) {
                inputFiles[key] = InputFiles(key, value)
            }
            return inputFiles.values.toList()
        }
    }

    fun selectTracks(): SelectedTracks<G>? {
        val videoTrack = allTracks().selectVideoTrack()
        if (videoTrack?.durationOrFileDuration == null) {
            System.err.println("Video track $videoTrack without duration")
            return null
        }

        val languageTracks = allTracks()
            .groupBy { it.language } //Group by language
            .mapValues { (_, tracks) ->
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
                    .firstOrNull()

                val forcedSubtitleTrack = subtitleTracks
                    .filter { it.isForced }
                    .firstOrNull()

                if (audioTrack == null && subtitleTrack == null) {
                    null
                } else {
                    SelectedTracks.LanguageTracks().apply {
                        this.audioTrack.track = audioTrack
                        this.subtitleTrack.track = subtitleTrack
                        this.forcedSubtitleTrack.track = forcedSubtitleTrack
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