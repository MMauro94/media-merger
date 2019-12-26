package com.github.mmauro94.shows_merger

import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.TvSeason
import org.apache.tools.ant.taskdefs.Input
import java.io.File

data class InputFiles(
    val episodeInfo: EpisodeInfo,
    val inputFiles: List<InputFile>
) : Iterable<InputFile>, Comparable<InputFiles> {

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
            if (ret.isEmpty()) {
                println()
                System.err.println("No files identified!")
            } else println("OK")
            return ret.map { InputFiles(it.key, it.value) }
        }

        private fun detectInner(dir: File): Map<EpisodeInfo, List<InputFile>> {
            val seasons = HashMap<Int, TvSeason?>()
            val show = MergeOptions.TV_SHOW
            val info: Pair<BaseTvShow, (Int) -> TvSeason?>? = if (show == null) null
            else {
                show to { sn ->
                    seasons.getOrPut(sn) {
                        tmdb?.let { tmdb ->
                            tmdb.tvSeasonsService().season(
                                show.id,
                                sn,
                                MergeOptions.MAIN_LANGUAGES.first().iso639_1 ?: "en"
                            ).execute().body().apply {
                                if (this == null) {
                                    System.err.println("Unable to download season $sn info")
                                }
                            }
                        }
                    }
                }
            }

            val ret = HashMap<EpisodeInfo, MutableList<InputFile>>()
            val listFiles: Array<File> = dir.listFiles() ?: emptyArray()
            val files = listFiles
                .filter { it.extension in EXTENSIONS_TO_IDENTIFY }
                .groupBy { it.name.detectEpisodeInfo(info) }
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

    override fun compareTo(other: InputFiles) = episodeInfo.compareTo(other.episodeInfo)
}