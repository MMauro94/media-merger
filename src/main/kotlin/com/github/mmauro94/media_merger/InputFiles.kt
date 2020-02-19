package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.group.Grouper
import com.github.mmauro94.media_merger.util.add
import com.github.mmauro94.media_merger.util.addAll
import com.github.mmauro94.media_merger.util.sortWithPreferences
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

        fun <G : Group<G>> detect(grouper: Grouper<G>, dir: File): List<InputFiles<G>> {
            print("Identifying files")
            val ret = detectInner(grouper, dir)
            if (ret.isEmpty()) {
                println()
                System.err.println("No files identified!")
            } else println("OK")
            return ret.map { InputFiles(it.key, it.value) }
        }

        private fun <G : Group<G>> detectInner(grouper: Grouper<G>, dir: File): Map<G, List<InputFile>> {
            val ret = HashMap<G, MutableList<InputFile>>()
            val listFiles: Array<File> = dir.listFiles() ?: emptyArray()
            val files = listFiles
                .filter { it.extension in EXTENSIONS_TO_IDENTIFY }
                .filterNot { it.name.contains("@adjusted") || it.name.contains("@extracted") }
                .groupBy { grouper.detectGroup(it.name) }
                .filterKeys { it != null }

            files.forEach { (ei, files) ->
                if (ei != null) {
                    files.forEach { f ->
                        try {
                            ret.add(ei, InputFile.parse(f))
                        } catch (e: InputFile.ParseException) {
                            System.err.println("Unable to parse file: ${e.message}")
                        }
                        print(".")
                    }
                }
            }
            listFiles.asSequence()
                .filter { it.isDirectory }
                .forEach {
                    ret.addAll(detectInner(grouper, it))
                }
            return ret
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