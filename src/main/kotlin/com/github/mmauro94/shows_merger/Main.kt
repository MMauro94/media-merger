package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.shows_merger.show_info.ShowInfoException
import com.github.mmauro94.shows_merger.show_provider.ShowProvider
import com.github.mmauro94.shows_merger.show_provider.TmdbShowProvider
import com.github.mmauro94.shows_merger.show_provider.TvdbShowProvider
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption


object Main {

    val workingDir: File = File("").absoluteFile
    private var inputFiles: List<InputFiles>? = null
    var showProvider: ShowProvider<*>? = null

    private fun inputFiles() = inputFiles.let {
        it ?: InputFiles.detect(workingDir).sorted().apply {
            inputFiles = this
        }
    }

    private fun changeShowProvider() {
        showProvider = when (askEnum("Select show info provider", listOf("tmdb", "tvdb"))) {
            "tmdb" -> TmdbShowProvider
            "tvdb" -> TvdbShowProvider
            else -> throw IllegalStateException()
        }
    }

    private fun reloadFiles() {
        inputFiles = null
        inputFiles()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("----- MMAURO's SHOWS MERGER UTILITY -----")
        while (MergeOptions.MAIN_LANGUAGES.isEmpty()) {
            MergeOptions.MAIN_LANGUAGES.addAll(askLanguages("What are the main languages?"))
        }
        println()
        mainMenu()
    }

    private fun mainMenu() {
        menu(
            premenu = {
                println("--- Main menu ---")
            },
            map = linkedMapOf(
                "Merge files" to ::mergeFiles,
                "Just rename files" to ::justRenameFiles,
                "See detected files" to ::seeDetectedFiles,
                "See selected tracks" to ::seeSelectedTracks,
                "Edit merge options" to ::editMergeOptions,
                "Reload files" to ::reloadFiles
            ),
            exitAfterSelection = { false }
        )
    }


    fun selectTvShow() {
        val q = askString("Name of TV show to search:")
        val results = try {
            if (showProvider == null) {
                changeShowProvider()
            }
            showProvider!!.searchShow(q)
        } catch (e: ShowInfoException) {
            System.err.println("Error searching for show")
            if (e.message != null && e.message.isNotBlank()) {
                System.err.println(e.message)
            }
            return
        }
        val map = results.associate {
            it.name to {
                inputFiles = null
                MergeOptions.TV_SHOW = it
                Unit
            }
        }.toMap(LinkedHashMap())
        map["-- Search again --"] = {
            selectTvShow()
        }
        menu(map)
    }

    private fun mergeFiles() {
        if (MergeOptions.TV_SHOW == null) {
            println("No show selected!")
            if (askYesNo("Select show?", true)) selectTvShow()
        }
        inputFiles()
            .mapNotNull {
                it.selectTracks()?.operation()
            }
            .forEach {
                it()
            }
    }

    fun justRenameFiles() {
        if (MergeOptions.TV_SHOW == null) {
            System.out.println("Must select show!")
            selectTvShow()
        }
        if (MergeOptions.TV_SHOW == null) {
            System.err.println("Cannot rename if show not selected!")
        } else {
            inputFiles = null
            inputFiles().forEach {
                val outputName = it.episode.outputName()
                if (outputName != null) {
                    it.inputFiles.forEach { f ->
                        try {
                            Files.move(
                                f.file.toPath(),
                                File(f.file.parentFile, outputName + "." + f.file.extension).toPath(),
                                StandardCopyOption.ATOMIC_MOVE
                            )
                        } catch (ioe: IOException) {
                            System.err.println(ioe.message)
                        }
                    }
                }
            }
        }
    }

    private fun editMergeOptions() {
        menu(
            premenu = {
                println("--- Edit merge options ---")
            },
            mapF = {
                val currentShow = MergeOptions.TV_SHOW?.name ?: "N/A"
                linkedMapOf(
                    "Select TV Show (Current: $currentShow)" to ::selectTvShow,
                    "Change max duration error (${MergeOptions.MAX_DURATION_ERROR.humanStr()})" to {
                        inputFiles = null
                        MergeOptions.MAX_DURATION_ERROR = askDuration("New duration", MergeOptions.MAX_DURATION_ERROR)
                    },
                    "Edit other languages to keep (${MergeOptions.OTHER_LANGUAGES_TO_KEEP.map { it.iso639_2 }})" to {
                        inputFiles = null
                        editLanguagesSet(MergeOptions.OTHER_LANGUAGES_TO_KEEP)
                    }
                )
            }
        )
    }

    private fun editLanguagesSet(set: MutableSet<MkvToolnixLanguage>) {
        menu(linkedMapOf(
            "Add" to {
                set.add(askLanguage())
                Unit
            },
            "Remove" to {
                set.remove(askLanguage())
                Unit
            },
            "Set" to {
                set.clear()
                set.addAll(askLanguages("Languages:"))
                Unit
            }
        ), premenu = {
            if (set.isEmpty()) {
                println("Currently NO languages")
            } else {
                println("Current languages:")
                set.forEachIndexed { i, it ->
                    println("${i + 1}) $it")
                }
            }
            println()
        }, exitAfterSelection = { it == 3 })
    }

    private fun seeDetectedFiles() {
        inputFiles().forEach {
            println("${it.episode}:")
            it.forEach { f ->
                println("\t${f.file.name}")
            }
            println()
        }
    }

    private fun seeSelectedTracks() {
        inputFiles().forEach {
            println("${it.episode}:")
            val selectedTracks = it.selectTracks()
            if (selectedTracks == null) {
                println("\tSkipped")
            } else {
                println("\tVideo track: ${selectedTracks.videoTrack}")
                selectedTracks.languageTracks.forEach { (lang, lt) ->
                    if (lt.audioTrack.track != null) {
                        println("\t${lang.iso639_2} audio: ${lt.audioTrack}")
                    }
                    if (lt.subtitleTrack.track != null) {
                        println("\t${lang.iso639_2} subtitles: ${lt.subtitleTrack}")
                    }
                    if (lt.forcedSubtitleTrack.track != null) {
                        println("\t${lang.iso639_2} forced subtitles: ${lt.forcedSubtitleTrack}")
                    }
                }
            }
            println()
        }
    }

}