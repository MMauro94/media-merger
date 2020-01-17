package com.github.mmauro94.shows_merger.audio_adjustment

import com.github.mmauro94.shows_merger.*
import com.github.mmauro94.shows_merger.cuts.Cut
import com.github.mmauro94.shows_merger.cuts.Cuts
import com.github.mmauro94.shows_merger.cuts.Silence
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import java.time.Duration

class CutsAudioAdjustment(
    adjustment: Cuts
) : AbstractAudioAdjustment<Cuts>(adjustment) {

    override val outputConcat = listOf("cuts" + adjustment.hashCode())

    private val silenceOrCuts = adjustment.getCutParts()

    override fun shouldAdjust(): Boolean {
        return !adjustment.isEmptyOffset()
    }   

    private class Filter(val filter: String, vararg val outs: String) {
        override fun toString() = filter + outs.joinToString(separator = "") { "[$it]" }
    }

    private fun buildCutFilters(): List<Filter> {
        val ret = mutableListOf<Filter>()
        val toConcat = mutableListOf<String>()

        var inIndex = 0
        for ((i, piece) in silenceOrCuts.withIndex()) {
            when (piece) {
                is Cut -> {
                    val part = "part$i"
                    val f = Filter(
                        "[in${inIndex++}]atrim=start=" + piece.startCut.toTotalSeconds() + ":end=" + piece.endCut.toTotalSeconds() + ",asetpts=PTS-STARTPTS",
                        part
                    )
                    ret.add(f)
                    toConcat.add(part)
                }
                is Silence -> {
                    toConcat.add(silenceOrCuts.filterIsInstance<Silence>().indexOf(piece).toString())
                }
            }
        }

        if (toConcat.isNotEmpty()) {
            ret.add(Filter(
                toConcat.joinToString(separator = "") { "[$it]" } + "concat=n=${toConcat.size}:v=0:a=1",
                "outa"
            ))
        }
        return ret
    }

    private lateinit var filters: List<Filter>

    override fun prepare(inputTrack: Track) {
        targetDuration = silenceOrCuts.fold(Duration.ZERO) { acc, it -> acc + it.duration }

        val input = silenceOrCuts.filterIsInstance<Silence>().size.toString() + ":${inputTrack.id}"
        val cutsCount = silenceOrCuts.count { it is Cut }
        filters = listOf(
            listOf(Filter("[$input]asplit=$cutsCount", *Array(cutsCount) { i -> "in$i" })),
            buildCutFilters()
        ).flatten()
    }

    override fun FFmpegBuilder.fillBuilder(inputTrack: Track) {
        silenceOrCuts.forEach {
            if (it is Silence) {
                addExtraArgs("-f", "lavfi", "-t", it.duration.toTotalSeconds(), "-i", "anullsrc")
            }
        }

        filters.joinToString("; ").let {
            setComplexFilter("\"$it\"")
        }
    }

    override fun FFmpegOutputBuilder.fillOutputBuilder(inputTrack: Track) {
        addExtraArgs("-map", "[" + filters.last().outs.single() + "]")
    }
}