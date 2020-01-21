package com.github.mmauro94.shows_merger

import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnix
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.mkvtoolnix_wrapper.hasErrors
import com.github.mmauro94.mkvtoolnix_wrapper.merge.MkvMergeCommand
import com.github.mmauro94.shows_merger.adjustment.Adjustment
import com.github.mmauro94.shows_merger.adjustment.Adjustments
import com.github.mmauro94.shows_merger.adjustment.CutsAdjustment
import com.github.mmauro94.shows_merger.adjustment.StretchAdjustment
import java.io.File
import java.time.Duration

data class SelectedTracks(
    val episode: Episode,
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

    fun operation(mergeMode: MergeMode): () -> Unit {
        val outputFilenamePrefix = episode.outputName() ?: videoTrack.file.nameWithoutExtension

        val allAdjustments = ArrayList<Pair<Adjustments, (Track) -> Unit>>()
        var needsCheck = false
        try {
            allFiles()
                .filterNot { it == videoTrack.inputFile }
                .forEach { inputFile ->
                    val pair = selectAdjustments(mergeMode, inputFile, videoTrack.inputFile)
                    val (adj, adjNeedsCheck) = pair
                    needsCheck = needsCheck || adjNeedsCheck
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
            return {}
        }
        return {
            allAdjustments.forEach { (aa, f) ->
                aa.adjust()?.let { res ->
                    f(res)
                }
            }

            val outputFile = File(
                Main.outputDir,
                outputFilenamePrefix
                        + (if (needsCheck) "_needscheck" else "")
                        + ".mkv"
            )

            val result = MkvToolnix.merge(outputFile)
                .addTrack(videoTrack) {
                    isDefault = true
                    isForced = false
                    name = ""
                }
                .apply {
                    val comparables = mutableListOf<(MkvToolnixLanguage) -> Comparable<*>>()
                    MergeOptions.MAIN_LANGUAGES.forEach { l ->
                        comparables.add { it != l } //First all main languages
                    }
                    comparables.add { it.iso639_2 } //Then sorted by iso code
                    val sortedLanguages = languageTracks.toSortedMap(
                        compareBy(*comparables.toTypedArray())
                    ).filterKeys {
                        it in MergeOptions.MAIN_LANGUAGES ||
                                it in MergeOptions.OTHER_LANGUAGES_TO_KEEP //Remove non wanted languages
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
                }.executeAndPrint(true)
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
}

fun Sequence<Track>.selectVideoTrack(): Track? {
    return asSequence()
        .filter { it.isVideoTrack() }
        .sortWithPreferences({
            it.mkvTrack.codec.contains("265")
        })
        .sortedByDescending {
            it.mkvTrack.properties?.pixelDimensions?.height
        }
        .firstOrNull().apply {
            if (this == null) {
                System.err.println("No video tracks found")
            }
        }
}

fun InputFiles.selectTracks(): SelectedTracks? {
    val videoTrack = allTracks().selectVideoTrack() ?: return null
    if (videoTrack.durationOrFileDuration == null) {
        System.err.println("Video track $videoTrack without duration")
        return null
    }


    val languageTracks = allTracks()
        .groupBy { it.language } //Group by language
        .mapValues { (_, tracks) ->
            val audioTrack = tracks
                .asSequence()
                .filter { it.isAudioTrack() }
                .sortWithPreferences({
                    it.mkvTrack.codec.contains("DTS", true)
                }, {
                    it.mkvTrack.codec.contains("AC-3", true)
                }, {
                    it.mkvTrack.codec.contains("AAC", true)
                }, {
                    it.mkvTrack.codec.contains("FLAC", true)
                }, {
                    it.isOnItsFile
                }, {
                    sameFile(it, videoTrack)
                })
                .firstOrNull()

            val subtitleTracks = tracks
                .asSequence()
                .filter { it.isSubtitlesTrack() }
                .sortWithPreferences({
                    it.mkvTrack.properties?.textSubtitles == true
                }, {
                    it.isOnItsFile
                }, {
                    it.mkvTrack.properties?.trackName?.contains("SDH", ignoreCase = true) == true
                }, {
                    sameFile(it, videoTrack)
                })

            val subtitleTrack = subtitleTracks
                .filter { !it.isForced }
                .firstOrNull()

            val forcedSubtitleTrack = subtitleTracks
                .filter { it.isForced }
                .firstOrNull()

            if (audioTrack == null && subtitleTrack == null) {
                null
            } else {
                SelectedTracks.LanguageTracks().apply {
                    this.audioTrack.track = audioTrack
                    this.subtitleTrack.track = subtitleTrack
                    this.forcedSubtitleTrack.track = forcedSubtitleTrack
                }
            }
        }
        .filterValues { it != null }
        .mapValues { it.value as SelectedTracks.LanguageTracks }
        .toMap()

    return SelectedTracks(episode, videoTrack, languageTracks)
}

fun MkvMergeCommand.addTrack(
    track: SelectedTracks.TrackWithOptions,
    f: MkvMergeCommand.InputFile.TrackOptions.() -> Unit
) {
    track.track?.let {
        addTrack(it.mkvTrack) {
            if (track.stretchFactor != null || track.offset > Duration.ZERO) {
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