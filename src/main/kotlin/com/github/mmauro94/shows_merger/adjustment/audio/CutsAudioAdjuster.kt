package com.github.mmauro94.shows_merger.adjustment.audio

import com.github.mmauro94.shows_merger.*
import com.github.mmauro94.shows_merger.adjustment.AbstractAdjustment
import com.github.mmauro94.shows_merger.adjustment.CutsAdjustment
import com.github.mmauro94.shows_merger.cuts.Cut
import com.github.mmauro94.shows_merger.cuts.Cuts
import com.github.mmauro94.shows_merger.cuts.Empty
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import java.time.Duration

class CutsAudioAdjustmer(
    adjustment: CutsAdjustment
) : AudioAdjustmer<CutsAdjustment>(adjustment) {


    private val cutParts = adjustment.cuts.getCutParts()
    private var filters: List<Filter>

    init {
        targetDuration = cutParts.fold(Duration.ZERO) { acc, it -> acc + it.duration }

        val input = cutParts.filterIsInstance<Empty>().size.toString() + ":${inputTrack.id}"
        val cutsCount = cutParts.count { it is Cut }
        filters = listOf(
            listOf(
                Filter(
                    "[$input]asplit=$cutsCount",
                    *Array(cutsCount) { i -> "in$i" })
            ),
            buildCutFilters()
        ).flatten()
    }


    private class Filter(val filter: String, vararg val outs: String) {
        override fun toString() = filter + outs.joinToString(separator = "") { "[$it]" }
    }

    private fun buildCutFilters(): List<Filter> {
        val ret = mutableListOf<Filter>()
        val toConcat = mutableListOf<String>()

        var inIndex = 0
        for ((i, piece) in cutParts.withIndex()) {
            when (piece) {
                is Cut -> {
                    val part = "part$i"
                    val f =
                        Filter(
                            "[in${inIndex++}]atrim=start=" + piece.time.start.toTotalSeconds() + ":end=" + piece.time.end.toTotalSeconds() + ",asetpts=PTS-STARTPTS",
                            part
                        )
                    ret.add(f)
                    toConcat.add(part)
                }
                is Empty -> {
                    toConcat.add(cutParts.filterIsInstance<Empty>().indexOf(piece).toString())
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



    override fun FFmpegBuilder.fillBuilder(inputTrack: Track) {
        cutParts.forEach {
            if (it is Empty) {
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