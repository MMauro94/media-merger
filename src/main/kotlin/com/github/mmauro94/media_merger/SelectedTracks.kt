package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.adjustment.Adjustment
import com.github.mmauro94.media_merger.adjustment.Adjustments
import com.github.mmauro94.media_merger.adjustment.CutsAdjustment
import com.github.mmauro94.media_merger.adjustment.StretchAdjustment
import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.strategy.AdjustmentStrategies
import com.github.mmauro94.media_merger.util.ProgressHandler
import com.github.mmauro94.media_merger.util.addTrack
import com.github.mmauro94.media_merger.util.sortWithPreferences
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnix
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.mkvtoolnix_wrapper.hasErrors
import com.github.mmauro94.mkvtoolnix_wrapper.merge.MkvMergeCommand
import java.io.File
import java.time.Duration

data class SelectedTracks<G : Group<G>>(
    val group: G,
    val videoTrack: Track,
    val languageTracks: Map<MkvToolnixLanguage, LanguageTracks>
) {
    data class TrackWithOptions(
        var track: Track? = null,
        var offset: Duration = Duration.ZERO,
        var stretchFactor: StretchFactor? = null
    ) {
        override fun toString(): String {
            return if (track == null) "None"
            else "$track" + (stretchFactor?.let { ", stretch factor: $it" }
                ?: "") + offset.let { ", offset factor: ${it.toMillis()}" }
        }
    }

    data class LanguageTracks(
        val audioTrack: TrackWithOptions = TrackWithOptions(),
        val subtitleTrack: TrackWithOptions = TrackWithOptions(),
        val forcedSubtitleTrack: TrackWithOptions = TrackWithOptions()
    ) {
        fun countTracks(): Int {
            return sequenceOf(audioTrack.track, subtitleTrack.track, forcedSubtitleTrack.track).filterNotNull().count()
        }
    }

    fun allTracks() = languageTracks.asSequence()
        .flatMap {
            sequenceOf(
                it.value.audioTrack,
                it.value.subtitleTrack,
                it.value.forcedSubtitleTrack
            )
        }
        .filterNotNull()
        .toSet()

    fun allFiles() = allTracks()
        .mapNotNull { it.track?.inputFile }
        .toSet()

    fun merge(adjustmentStrategies: AdjustmentStrategies, progress: ProgressHandler) {
        val outputFilenamePrefix = group.outputName() ?: videoTrack.file.nameWithoutExtension

        val allAdjustments = ArrayList<Pair<Adjustments, (Track) -> Unit>>()
        try {
            val detectProgress = progress.split(0f, adjustmentStrategies.detectProgressWeight, "Detecting files adjustments...")
            val filteredFiles = allFiles().filterNot { it == videoTrack.inputFile }

            filteredFiles
                .forEachIndexed { i, inputFile ->
                    val adj = selectAdjustments(
                        adjustmentStrategies,
                        inputFile,
                        videoTrack.inputFile,
                        detectProgress.split(i, filteredFiles.size, "Detecting adjustments for file ${inputFile.file.name}...")
                    )
                    if (!adj.isEmpty()) {
                        allTracks()
                            .filter { it.track?.inputFile == inputFile }
                            .forEach {
                                val track = it.track!!
                                val trackAdjustments = mutableListOf<Adjustment<*>>()
                                val offset = adj.cuts.optOffset()

                                if (track.isAudioTrack() || offset == null) {
                                    trackAdjustments += StretchAdjustment(adj.stretchFactor)
                                } else {
                                    it.stretchFactor = adj.stretchFactor
                                }

                                if (offset != null) {
                                    it.offset = offset
                                } else {
                                    trackAdjustments += CutsAdjustment(adj.cuts)
                                }

                                allAdjustments.add(
                                    Pair(
                                        Adjustments(
                                            it.track!!,
                                            trackAdjustments
                                        ), { t ->
                                            it.track = t
                                            it.stretchFactor = null
                                        })
                                )
                            }
                    }
                }
        } catch (e: OperationCreationException) {
            e.printTo(File(Main.outputDir, "$outputFilenamePrefix.err.txt"))
            return
        }

        val adjustmentsProgress = progress.split(adjustmentStrategies.detectProgressWeight, .9f, "Detecting files adjustments...")
        allAdjustments.forEachIndexed { i, (aa, f) ->
            aa.adjust(adjustmentsProgress.split(i, allAdjustments.size, "Adjusting ${aa.inputTrack}"))?.let { res ->
                f(res)
            }
        }

        val outputFile = File(Main.outputDir, "$outputFilenamePrefix.mkv")
        val command = MkvToolnix.merge(outputFile)
            .addTrack(videoTrack) {
                isDefault = true
                isForced = false
                name = ""
            }
            .apply {
                val comparables = mutableListOf<(MkvToolnixLanguage) -> Comparable<*>>()
                Main.mainLanguages.forEach { l ->
                    comparables.add { it != l } //First all main languages
                }
                comparables.add { it.iso639_2 } //Then sorted by iso code
                val sortedLanguages = languageTracks.toSortedMap(
                    compareBy(*comparables.toTypedArray())
                ).filterKeys {
                    it in Main.mainLanguages ||
                            it in Main.additionalLanguagesToKeep //Remove non wanted languages
                }
                sortedLanguages.forEach { (lang, tracks) ->
                    addTrack(tracks.audioTrack) {
                        isDefault = lang.iso639_2 == "eng"
                        isForced = false
                        name = ""
                        language = lang
                    }
                }
                sortedLanguages.forEach { (lang, tracks) ->
                    addTrack(tracks.subtitleTrack) {
                        isDefault = lang.iso639_2 == "eng"
                        isForced = false
                        name = ""
                        language = lang
                    }

                    addTrack(tracks.forcedSubtitleTrack) {
                        isForced = true
                        isDefault = false
                        name = "Forced"
                        language = lang
                    }
                }
            }
        val tracksCount = 1 + languageTracks.entries.sumBy { it.value.countTracks() }
        val mkvMergeProgress = progress.split(.9f, 1f, "Merging $tracksCount tracks...")

        val result = command.executeLazy()
        result.output.forEach { line ->
            line.progress?.let {
                mkvMergeProgress.ratio(it, line.message)
            }
        }

        if (!result.success) {
            val part = if (!result.output.hasErrors()) "mergewarn" else "mergeerr"
            File(outputFile.parentFile, outputFile.nameWithoutExtension + ".$part.txt").printWriter().use { pw ->
                result.output.forEach {
                    pw.println(it)
                }
            }
        }
    }
}

fun Sequence<Track>.selectVideoTrack(): Track? {
    return asSequence()
        .filter { it.isVideoTrack() }
        .sortWithPreferences({
            it.mkvTrack.codec.contains("265")
        })
        .sortedByDescending {
            it.normalizedPixelHeight
        }
        .firstOrNull().apply {
            if (this == null) {
                System.err.println("No video tracks found")
            }
        }
}

fun MkvMergeCommand.addTrack(
    track: SelectedTracks.TrackWithOptions,
    f: MkvMergeCommand.InputFile.TrackOptions.() -> Unit
) {
    track.track?.let {
        addTrack(it.mkvTrack) {
            if (track.stretchFactor != null || track.offset != Duration.ZERO) {
                sync(
                    track.offset,
                    track.stretchFactor.let { sf ->
                        if (sf == null) null else Pair(
                            sf.durationMultiplier.toFloat(),
                            null
                        )
                    }
                )
            }
            apply(f)
        }
    }
}