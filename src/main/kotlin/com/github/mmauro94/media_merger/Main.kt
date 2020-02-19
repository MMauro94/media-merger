package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.config.Config
import com.github.mmauro94.media_merger.config.ConfigParseException
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import java.io.File


object Main {

    /*
     * Missing things TODO:
     *  - Add a mode that calculates offset based only on the first black fragment
     *  - Add a stretch adjustment strategy (precise, known_only)
     *  - Add a way to specify adjustment for subtitles only once
     *  - Better error handling
     *  - [Option to convert the video if not in suitable format]
     *  - [Better output]
     *  - [Rename language option]
     *  - [Episode name format]
     */

    var config: Config? = null; private set
    lateinit var workingDir: File; private set
    lateinit var mainLanguages: LinkedHashSet<MkvToolnixLanguage>; private set
    lateinit var additionalLanguagesToKeep: Set<MkvToolnixLanguage>; private set
    lateinit var inputFilesDetector: InputFilesDetector<*>; private set

    val outputDir by lazy { File(workingDir, "OUTPUT") }


    @JvmStatic
    fun main(args: Array<String>) {
        workingDir = File(args.getOrNull(0) ?: System.getProperty("user.dir") ?: "").absoluteFile
        config = try {
            Config.parse()
        } catch (cpe: ConfigParseException) {
            System.err.println("config.json error: ${cpe.message}")
            null
        }

        println("----- MMAURO's MEDIA MERGER UTILITY -----")
        println("Working directory: $workingDir")
        println()

        mainLanguages =askLanguages(
            question = "What are the main languages?",
            defaultValue = config?.defaultLanguages?.toCollection(LinkedHashSet())
        )
        println()

        additionalLanguagesToKeep = askLanguages(
            question = "What are the additional languages to keep?",
            defaultValue = config?.defaultAdditionalLanguagesToKeep?.toCollection(LinkedHashSet()) ?: LinkedHashSet()
        )
        println()

        val mediaType = askEnum(
            question = "What do you need to merge?",
            enum = MediaType.ANY
        )
        println()

        inputFilesDetector = mediaType.inputFileDetectorFactory()
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
                "Reload files" to {
                    inputFilesDetector.reloadFiles()
                }
            ),
            exitAfterSelection = { false }
        )
    }


    private fun mergeFiles() {
        MergeMode.values().forEachIndexed { i, mm ->
            println("${i + 1}) ${mm.description}")
        }
        val mergeMode = MergeMode.values()[askInt("Select merge mode:", 1, MergeMode.values().size) - 1]

        inputFilesDetector.getOrReadInputFiles()
            .mapNotNull {
                it.selectTracks()?.operation(mergeMode)
            }
            .forEach {
                it()
            }
    }

    fun justRenameFiles() {
        TODO()/*
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
        }*/
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
        inputFilesDetector.getOrReadInputFiles().forEach {
            println("${it.group}:")
            it.forEach { f ->
                println("\t${f.file.name}")
            }
            println()
        }
    }

    private fun seeSelectedTracks() {
        inputFilesDetector.getOrReadInputFiles().forEach {
            println("${it.group}:")
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