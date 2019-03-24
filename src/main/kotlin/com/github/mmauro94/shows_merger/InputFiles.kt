package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnix
import net.bramp.ffmpeg.FFprobe
import java.io.File
import java.util.HashMap

data class InputFiles(
    val episodeInfo: EpisodeInfo,
    val inputFiles: List<InputFile>
) : Iterable<InputFile> {

    override fun iterator() = inputFiles.iterator()

    fun allTracks() = sequence {
        inputFiles.forEach {
            yieldAll(it.tracks)
        }
    }

    companion object {

        fun detect(dir: File): List<InputFiles> {
            print("Identifying files")
            val ret = detectInner(dir)
            println("OK")
            return ret.map { InputFiles(it.key, it.value) }
        }

        private fun detectInner(dir: File): Map<EpisodeInfo, List<InputFile>> {
            val ret = HashMap<EpisodeInfo, MutableList<InputFile>>()
            val listFiles = dir.listFiles()
            val files = listFiles
                .filter { it.extension in EXTENSIONS_TO_IDENTIFY }
                .groupBy { it.name.detectEpisodeInfo() }
                .filterKeys { it != null }

            files.forEach { (ei, files) ->
                if (ei != null) {
                    files.forEach { f ->
                        ret.add(ei, InputFile.parse(f))
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
}