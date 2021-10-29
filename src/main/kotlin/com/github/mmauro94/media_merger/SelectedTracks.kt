package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.adjustment.Adjustment
import com.github.mmauro94.media_merger.adjustment.Adjustments
import com.github.mmauro94.media_merger.adjustment.CutsAdjustment
import com.github.mmauro94.media_merger.adjustment.LinearDriftAdjustment
import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.strategy.AdjustmentStrategies
import com.github.mmauro94.media_merger.util.Reporter
import com.github.mmauro94.media_merger.util.addTrack
import com.github.mmauro94.media_merger.util.log.withPrependDebug
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
        var linearDrift: LinearDrift? = null
    ) {
        override fun toString(): String {
            return if (track == null) "None"
            else "$track" + (linearDrift?.let { ", stretch factor: $it" }
                ?: "") + offset.let { ", offset factor: ${it.toMillis()}" }
        }
    }

    data class LanguageTracks(
        val audioTrack: TrackWithOptions = TrackWithOptions(),
        val subtitleTrack: TrackWithOptions = TrackWithOptions(),
        val forcedSubtitleTrack: TrackWithOptions = TrackWithOptions()
    )

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

    fun merge(adjustmentStrategies: AdjustmentStrategies, reporter: Reporter) {
        val outputFilenamePrefix = group.outputName() ?: videoTrack.file.nameWithoutExtension

        reporter.log.debug("")

        val allAdjustments = ArrayList<Pair<Adjustments, (Track) -> Unit>>()
        try {
            val detectProgress = reporter.split(0f, adjustmentStrategies.detectProgressWeight, "Detecting files adjustments...")
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
                                    trackAdjustments += LinearDriftAdjustment(adj.linearDrift)
                                } else {
                                    it.linearDrift = adj.linearDrift
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
                                            it.linearDrift = null
                                        })
                                )
                            }
                    }
                }
        } catch (e: AdjustmentDetectionImpossible) {
            reporter.log.err("Unable to merge group $group! See logs for details")
            reporter.log.forceDebug()
            return
        }

        reporter.log.debug("Actual selected tracks:")
        reporter.log.withPrependDebug("   ") {
            for ((lang, tracks) in languageTracks) {
                debug("For $lang:")
                if (tracks.audioTrack.track != null) {
                    debug("  - Audio track: " + tracks.audioTrack)
                }
                if (tracks.subtitleTrack.track != null) {
                    debug("  - Subtitle track: " + tracks.subtitleTrack)
                }
                if (tracks.forcedSubtitleTrack.track != null) {
                    debug("  - Forced subtitle track: " + tracks.forcedSubtitleTrack)
                }
            }
        }
        val adjustmentsProgress = reporter.split(adjustmentStrategies.detectProgressWeight, .9f, "Adjusting files...")
        allAdjustments.forEachIndexed { i, (aa, f) ->
            aa.adjust(adjustmentsProgress.split(i, allAdjustments.size, "Adjusting ${aa.inputTrack}"))?.let { res ->
                f(res)
            }
        }

        val outputFile = File(Main.outputDir, "$outputFilenamePrefix.mkv")
        var tracksCount = 0
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
                comparables.add { it.iso639_3 } //Then sorted by iso code
                val sortedLanguages = languageTracks.toSortedMap(
                    compareBy(*comparables.toTypedArray())
                ).filterKeys {
                    it in Main.mainLanguages || it in Main.additionalLanguagesToKeep //Remove unwanted languages
                }
                sortedLanguages.forEach { (lang, tracks) ->
                    addTrack(tracks.audioTrack) {
                        isDefault = lang == sortedLanguages.keys.first()
                        isForced = false
                        name = ""
                        language = lang
                        tracksCount++
                    }
                }
                sortedLanguages.forEach { (lang, tracks) ->
                    addTrack(tracks.subtitleTrack) {
                        isDefault = lang == sortedLanguages.keys.first()
                        isForced = false
                        name = ""
                        language = lang
                        tracksCount++
                    }

                    addTrack(tracks.forcedSubtitleTrack) {
                        isForced = true
                        isDefault = false
                        name = "Forced"
                        language = lang
                        tracksCount++
                    }
                }
            }

        reporter.log.debug()
        reporter.log.debug("--- MKV EXECUTION ---")
        reporter.log.debug("mkvmerge " + command.commandArgs().joinToString(" "))
        reporter.log.debug()

        val mkvMergeProgress = reporter.split(.9f, 1f, "Merging $tracksCount tracks...")

        val result = command.executeLazy()
        result.output.forEach { line ->
            if (!line.isProgressLine) {
                reporter.log.debug(line.toString())
            }
            line.progress?.let {
                mkvMergeProgress.progress.ratio(it, line.message)
            }
        }

        if (!result.success) {
            reporter.log.forceDebug()
            if (result.output.hasErrors()) {
                reporter.log.debug("MKV MERGE FINISHED WITH ERROR")
            } else {
                reporter.log.debug("MKV MERGE FINISHED WITH WARNINGS")
            }
        }
    }
}


fun MkvMergeCommand.addTrack(
    track: SelectedTracks.TrackWithOptions,
    f: MkvMergeCommand.InputFile.TrackOptions.() -> Unit
) {
    track.track?.let {
        addTrack(it.mkvTrack) {
            if (track.linearDrift != null || track.offset != Duration.ZERO) {
                sync(
                    track.offset,
                    track.linearDrift.let { sf ->
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