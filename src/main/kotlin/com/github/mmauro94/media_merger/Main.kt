package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.config.Config
import com.github.mmauro94.media_merger.config.ConfigParseException
import com.github.mmauro94.media_merger.strategy.AdjustmentStrategies
import com.github.mmauro94.media_merger.util.*
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
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
        while (true) {
            println()
            menu(
                title = "--- Main menu ---",
                items = linkedMapOf(
                    "Merge files" to ::mergeFiles,
                    "Just rename files" to ::justRenameFiles,
                    "See detected files" to ::seeDetectedFiles,
                    "See selected tracks" to ::seeSelectedTracks,
                    "Reload files" to {
                        ConsoleProgressHandler().use { progress ->
                            inputFilesDetector.reloadFiles(progress)
                        }
                        Unit
                    },
                    "Quit" to { exitProcess(0) }
                )
            )
        }
    }


    private fun mergeFiles() {
        val adjustmentStrategies = AdjustmentStrategies.ask()
        println()

        ConsoleProgressHandler().use { progress ->
            val inputFiles = inputFilesDetector.getOrReadInputFiles(progress.split(0f, 0.1f, "Identifying..."))

            val mergeProgress = progress.split(.1f, 1f, "Merging...")

            for ((i, it) in inputFiles.withIndex()) {
                it.selectTracks()?.merge(adjustmentStrategies, mergeProgress.split(i, inputFiles.size, "Merging group ${it.group}"))
            }

            progress.finished("Merging of all files completed successfully")
        }
    }

    private fun justRenameFiles() {
        ConsoleProgressHandler().use { progress ->
            val inputFiles = inputFilesDetector.getOrReadInputFiles(progress.split(0f, 0.9f, "Identifying..."))

            val renameProgressHandler = progress.split(0.9f, 1f, "Renaming...")
            inputFiles.forEachIndexed { i, files ->
                val groupProgressHandler = renameProgressHandler.split(i, inputFiles.size, "Renaming group ${files.group}...")

                val outputName = files.outputName()
                if (outputName != null) {
                    files.inputFiles.forEachIndexed { j, f ->
                        groupProgressHandler.discrete(j, files.inputFiles.size, "Renaming file ${f.file.name}")
                        try {
                            Files.move(
                                f.file.toPath(),
                                File(f.file.parentFile, outputName + "." + f.file.extension).toPath(),
                                StandardCopyOption.ATOMIC_MOVE
                            )
                        } catch (ioe: IOException) {
                            //System.err.println(ioe.message)
                            //TODO Warn
                        }
                    }
                } else {
                    groupProgressHandler.indeterminate("Cannot rename group ${files.group}")
                    //System.err.println("Cannot rename group ${files.group}")
                    //TODO Warn
                }
            }

            progress.finished("Rename complete")
        }
    }

    private fun seeDetectedFiles() {
        ConsoleProgressHandler().use { progress ->
            val files = inputFilesDetector.getOrReadInputFiles(progress)

            files.forEachIndexed { i, it ->
                println("${it.group}:")
                it.forEach { f ->
                    println("\t${f.file.name}")
                }
                if (i != files.lastIndex) {
                    println()
                }
            }
        }
    }

    private fun seeSelectedTracks() {
        ConsoleProgressHandler().use { progress ->
            inputFilesDetector.getOrReadInputFiles(progress).forEach {
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
}