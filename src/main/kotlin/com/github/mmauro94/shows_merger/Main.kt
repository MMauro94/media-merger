package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import java.io.File


object Main {

    //val workingDir = File("").absoluteFile
    val workingDir = File("C:\\Users\\molin\\Desktop\\TO SERVER\\to mux\\S01").absoluteFile
    //val workingDir = File("C:\\Users\\molin\\Desktop\\TO SERVER\\to mux\\std").absoluteFile


    @JvmStatic
    fun main(args: Array<String>) {
        println("----- MMAURO's SHOWS MERGER UTILITY -----")
        mainMenu()
    }

    private fun mainMenu() {
        menu(linkedMapOf(
            "See detected files" to ::seeDetectedFiles,
            "Merge files!" to ::mergeFiles,
            "Edit merge options" to ::editMergeOptions,
            "Edit files" to {}
        ), exitAfterSelection = { false })
    }

    fun mergeFiles() {
        InputFiles.detect(workingDir)
            .asSequence()
            .drop(1)
            .take(1)
            .map {
                it.selectTracks()?.operation()
            }
            .filterNotNull()
            .forEach {
                it()
            }
    }


    private fun editMergeOptions() {
        menu(
            linkedMapOf(
                "Edit main languages (${MergeOptions.MAIN_LANGUAGES.map { it.iso639_2 }})" to {
                    editLanguagesSet(MergeOptions.MAIN_LANGUAGES)
                },
                "Edit other languages to keep (${MergeOptions.OTHER_LANGUAGES_TO_KEEP.map { it.iso639_2 }})" to {
                    editLanguagesSet(MergeOptions.OTHER_LANGUAGES_TO_KEEP)
                }
            )
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
                set.addAll(askLanguages())
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
        }, exitAfterSelection = { it == 3 })
    }

    private fun seeDetectedFiles() {
        InputFiles.detect(workingDir).forEach {
            println("${it.episodeInfo}:")
            it.forEach { f ->
                println("\t${f.file.name}")
            }
            println()
        }
    }

}