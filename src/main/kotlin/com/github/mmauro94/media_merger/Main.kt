package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.config.Config
import com.github.mmauro94.media_merger.config.ConfigParseException
import com.github.mmauro94.media_merger.strategy.AdjustmentStrategies
import com.github.mmauro94.media_merger.util.askLanguages
import com.github.mmauro94.media_merger.util.menu
import com.github.mmauro94.media_merger.util.selectEnum
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess


object Main {

    /*
     * Missing things TODO:
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
        AnsiConsole.systemInstall()

        workingDir = File(args.getOrNull(0) ?: System.getProperty("user.dir") ?: "").absoluteFile

        config = try {
            Config.parse()
        } catch (cpe: ConfigParseException) {
            System.err.println("config.json error: ${cpe.message}")
            Config()
        }
        println(ansi().bgBrightGreen().fgBlack().a("----- MEDIA-MERGER UTILITY -----").reset())
        println(ansi().fgDefault().a("Working directory: ").fgGreen().a(workingDir.toString()).reset())
        println()

        mainLanguages = askLanguages(
            question = "What are the main languages?",
            defaultValue = config.defaultLanguages?.toCollection(LinkedHashSet())
        )

        additionalLanguagesToKeep = askLanguages(
            question = "What are the additional languages to keep?",
            defaultValue = config.defaultAdditionalLanguagesToKeep.toCollection(LinkedHashSet())
        )

        val mediaType = selectEnum(
            question = "What do you need to merge?",
            defaultValue = MediaType.ANY
        )

        inputFilesDetector = mediaType.inputFileDetectorFactory()

        mainMenu()
    }

    private fun mainMenu() {
        menu(
            title = "--- Main menu ---",
            items = linkedMapOf(
                "Merge files" to ::mergeFiles,
                "Just rename files" to ::justRenameFiles,
                "See detected files" to ::seeDetectedFiles,
                "See selected tracks" to ::seeSelectedTracks,
                "Reload files" to {
                    inputFilesDetector.reloadFiles()
                    Unit
                },
                "Quit" to { exitProcess(0) }
            )
        )
    }


    private fun mergeFiles() {
        val adjustmentStrategies = AdjustmentStrategies.ask()
        println()

        inputFilesDetector.getOrReadInputFiles()
            .mapNotNull {
                it.selectTracks()?.operation(adjustmentStrategies)
            }
            .forEach {
                it()
            }
    }

    private fun justRenameFiles() {
        inputFilesDetector.getOrReadInputFiles().forEach {
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
                println("\t\t${f.mainFile?.file?.name}")
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