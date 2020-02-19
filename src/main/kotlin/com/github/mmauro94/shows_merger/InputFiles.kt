package com.github.mmauro94.shows_merger

import com.github.mmauro94.shows_merger.show.Episode
import com.github.mmauro94.shows_merger.show.detectEpisode
import com.github.mmauro94.shows_merger.util.add
import com.github.mmauro94.shows_merger.util.addAll
import java.io.File

data class InputFiles(
    val episode: Episode,
    val inputFiles: List<InputFile>
) : Iterable<InputFile>, Comparable<InputFiles> {

    override fun iterator() = inputFiles.iterator()

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

        fun detect(dir: File): List<InputFiles> {
            print("Identifying files")
            val ret = detectInner(dir)
            if (ret.isEmpty()) {
                println()
                System.err.println("No files identified!")
            } else println("OK")
            return ret.map { InputFiles(it.key, it.value) }
        }

        private fun detectInner(dir: File): Map<Episode, List<InputFile>> {
            val show = MergeOptions.TV_SHOW

            val ret = HashMap<Episode, MutableList<InputFile>>()
            val listFiles: Array<File> = dir.listFiles() ?: emptyArray()
            val files = listFiles
                .filter { it.extension in EXTENSIONS_TO_IDENTIFY }
                .filterNot { it.name.contains("@adjusted") || it.name.contains("@extracted") }
                .groupBy { it.name.detectEpisode(show) }
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
                    ret.addAll(detectInner(it))
                }
            return ret
        }
    }

    override fun compareTo(other: InputFiles) = episode.compareTo(other.episode)
}