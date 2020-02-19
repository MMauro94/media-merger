package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.config.Config
import com.github.mmauro94.media_merger.config.ConfigParseException
import com.github.mmauro94.media_merger.strategy.AdjustmentStrategies
import com.github.mmauro94.media_merger.strategy.CutsAdjustmentStrategy
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption


object Main {

    /*
     * Missing things TODO:
     *  - Add a mode that calculates offset based only on the first black fragment
     *  - Add a way to specify adjustment for subtitles only once
     *  - Better error handling
     *  - [Option to convert the video if not in suitable format]
     *  - [Better output]
     *  - [Rename language option]
     *  - [Episode name format]
     */

    lateinit var config: Config; private set
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
            Config()
        }

        println("----- MMAURO's MEDIA MERGER UTILITY -----")
        println("Working directory: $workingDir")
        println()

        mainLanguages = askLanguages(
            question = "What are the main languages?",
            defaultValue = config.defaultLanguages?.toCollection(LinkedHashSet())
        )
        println()

        additionalLanguagesToKeep = askLanguages(
            question = "What are the additional languages to keep?",
            defaultValue = config.defaultAdditionalLanguagesToKeep.toCollection(LinkedHashSet())
        )
        println()

        val mediaType = askEnum(
            question = "What do you need to merge?",
            defaultValue = MediaType.ANY
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
                    Unit
                }
            ),
            exitAfterSelection = { false }
        )
    }


    private fun mergeFiles() {
        val adjustmentStrategies = AdjustmentStrategies.ask()

        inputFilesDetector.getOrReadInputFiles()
            .mapNotNull {
                it.selectTracks()?.operation(adjustmentStrategies)
            }
            .forEach {
                it()
            }
    }

    private fun justRenameFiles() {
        inputFilesDetector.inputFiles().forEach {
            val outputName = it.outputName()
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
            } else {
                System.err.println("Cannot rename group ${it.group}")
            }
        }
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