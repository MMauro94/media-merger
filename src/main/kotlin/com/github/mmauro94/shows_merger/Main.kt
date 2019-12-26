package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import java.io.File
import java.time.Instant
import java.time.ZoneId


object Main {

    val workingDir: File = File("").absoluteFile
    private var inputFiles: List<InputFiles>? = null

    private fun inputFiles() = inputFiles.let {
        it ?: InputFiles.detect(workingDir).sorted().apply {
            inputFiles = this
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
                "See detected files" to ::seeDetectedFiles,
                "See selected tracks" to ::seeSelectedTracks,
                "Edit merge options" to ::editMergeOptions,
                "Reload files" to ::reloadFiles
            ),
            exitAfterSelection = { false }
        )
    }


    private fun selectTvShow() {
        tmdb?.let { tmdb ->
            val q = askString("Name of TV show to search:")
            val search =
                tmdb.searchService().tv(q, 1, MergeOptions.MAIN_LANGUAGES.first().iso639_1 ?: "en", null, null)
                    .execute()
            val body = search.body()
            if (body != null) {
                val map = body.results.asSequence().associate {
                    val year = it.first_air_date?.let { d ->
                        "(" + Instant.ofEpochMilli(d.time).atZone(ZoneId.systemDefault()).year + ")"
                    }
                    var name = it.name ?: ""
                    if (year != null && !name.endsWith(year)) {
                        name += " $year"
                    }
                    "$name - TMDB ID:${it.id}" to {
                        inputFiles = null
                        MergeOptions.TV_SHOW = it
                        Unit
                    }
                }.toMap(LinkedHashMap())
                map["-- Search again --"] = {
                    selectTvShow()
                }
                menu(map)
            } else {
                System.err.println("Error searching for show")
                search.errorBody()?.let {
                    System.err.println(it.string())
                }
            }
        }
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
            println("${it.episodeInfo}:")
            it.forEach { f ->
                println("\t${f.file.name}")
            }
            println()
        }
    }

    private fun seeSelectedTracks() {
        inputFiles().forEach {
            println("${it.episodeInfo}:")
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