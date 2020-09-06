package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.config.Config
import com.github.mmauro94.media_merger.config.ConfigParseException
import com.github.mmauro94.media_merger.strategy.AdjustmentStrategies
import com.github.mmauro94.media_merger.util.*
import com.github.mmauro94.media_merger.util.log.ConsoleLogger
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
     *  - User help guide
     *  - [Option to convert the video if not in suitable format]
     */

    lateinit var config: Config; private set
    lateinit var workingDir: File; private set
    lateinit var mainLanguages: LinkedHashSet<MkvToolnixLanguage>; private set
    lateinit var additionalLanguagesToKeep: Set<MkvToolnixLanguage>; private set
    lateinit var inputFilesDetector: InputFilesDetector<*>; private set
    var test: Boolean = false
    var debug: Boolean = false

    val outputDir by lazy { File(workingDir, "OUTPUT") }
    val globalLog: File by lazy { File(outputDir, "global_debug.txt") }


    @JvmStatic
    fun main(args: Array<String>) {
        AnsiConsole.systemInstall()
        val options = args.toMutableList()
        if (options.remove("--debug")) {
            debug = true
        }
        if (options.remove("--test")) {
            test = true
        }
        val providedConfigFiles = options
            .filter { it.startsWith("--config=") }
            .map {
                options.remove(it)
                File(it.removePrefix("--config="))
            }

        workingDir = File(options.getOrNull(0) ?: System.getProperty("user.dir") ?: "").absoluteFile

        config = try {
            ConsoleReporter().use { cr ->
                Config.parse(listOf(Config.SYSTEM_CONFIG_FILE) + providedConfigFiles + File(workingDir, "config.json"), cr)
            }
        } catch (cpe: ConfigParseException) {
            println(ansi().fgRed().a("config.json parsing error: ${cpe.message}"))
            println(ansi().fgYellow().a("Using default config").reset())
            Config()
        }

        println(providedConfigFiles)

        if (!outputDir.exists()) {
            outputDir.mkdir()
        }

        println(ansi().bgBrightGreen().fgBlack().a("----- MEDIA-MERGER UTILITY -----").reset())
        println(ansi().fgDefault().a("Working directory: ").fgGreen().a(workingDir.toString()).reset())
        if (debug) println(ansi().fgBlue().a("Debug mode active").reset())
        if (test) println(ansi().fgBlue().a("Test mode active").reset())
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
                items = mapOf<String, () -> Unit>(
                    "Merge files" to ::mergeFiles,
                    "Just rename files" to ::justRenameFiles,
                    "See detected files" to ::seeDetectedFiles,
                    "See selected tracks" to ::seeSelectedTracks,
                    "Reload files" to {
                        ConsoleReporter().use { progress ->
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

        ConsoleReporter().use { reporter ->
            reporter.log.debug("Selected linear drift strategy: ${adjustmentStrategies.linearDrift}")
            reporter.log.debug("Selected cut strategy: ${adjustmentStrategies.cuts}\n")

            val inputFiles = inputFilesDetector.getOrReadInputFiles(reporter.split(0f, 0.1f, "Identifying..."))
            reporter.log.debug("Identified ${inputFiles.size} groups")
            for (group in inputFiles) {
                reporter.log.debug("Group ${group.group}:")
                for (inputFile in group.inputFiles) {
                    reporter.log.debug("\t- $inputFile")
                }
            }

            val mergeReporter = reporter.split(.1f, 1f, "Merging...")

            for ((i, it) in inputFiles.withIndex()) {
                val r = mergeReporter
                    .withDebug(it.debugFile)
                    .split(i, inputFiles.size, "Merging group ${it.group}")
                it.selectTracks(r.log)?.merge(adjustmentStrategies, r)
            }

            reporter.progress.finished("Merging of all files completed successfully")
        }
    }

    private fun justRenameFiles() {
        ConsoleReporter().use { reporter ->
            val inputFiles = inputFilesDetector.getOrReadInputFiles(reporter.split(0f, 0.9f, "Identifying..."))

            val renameProgressHandler = reporter.split(0.9f, 1f, "Renaming...")
            inputFiles.forEachIndexed { i, files ->
                val groupReporter = renameProgressHandler.split(i, inputFiles.size, "Renaming group ${files.group}...")

                val outputName = files.outputName()
                if (outputName != null) {
                    files.inputFiles.forEachIndexed { j, f ->
                        groupReporter.progress.discrete(j, files.inputFiles.size, "Renaming file ${f.file.name}")
                        try {
                            Files.move(
                                f.file.toPath(),
                                File(f.file.parentFile, outputName + "." + f.file.extension).toPath(),
                                StandardCopyOption.ATOMIC_MOVE
                            )
                        } catch (ioe: IOException) {
                            groupReporter.log.err("Unable to rename file ${f.file.name}: ${ioe.message}")
                        }
                    }
                } else {
                    groupReporter.log.warn("Cannot rename group ${files.group}: insufficient information")
                }
            }

            reporter.progress.finished("Rename complete")
        }
    }

    private fun seeDetectedFiles() {
        ConsoleReporter().use { progress ->
            val files = inputFilesDetector.getOrReadInputFiles(progress)

            println()
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
        ConsoleReporter().use { reporter ->
            inputFilesDetector.getOrReadInputFiles(reporter).forEach {
                println("${it.group}:")
                val selectedTracks = it.selectTracks(ConsoleLogger.prepend("\t"))
                if (selectedTracks == null) {
                    println(ansi().fgRed().a("\tSkipped").reset())
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