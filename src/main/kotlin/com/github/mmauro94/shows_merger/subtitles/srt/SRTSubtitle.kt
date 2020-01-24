package com.github.mmauro94.shows_merger.subtitles.srt

import com.github.mmauro94.shows_merger.subtitles.Subtitle
import com.github.mmauro94.shows_merger.subtitles.SubtitleCompanion
import com.github.mmauro94.shows_merger.util.DurationSpan
import java.io.File
import java.time.Duration

/**
 * An SRT subtitle.
 */
class SRTSubtitle(lines: List<SRTSubtitleText>) : Subtitle<SRTSubtitleText>(lines) {

    override fun SRTSubtitleText.changeTime(time: DurationSpan): SRTSubtitleText {
        return SRTSubtitleText(time, text)
    }

    override fun withLines(items: List<SRTSubtitleText>): Subtitle<SRTSubtitleText> {
        return SRTSubtitle(items)
    }

    override fun save(outputFile: File) {
        require(outputFile.extension == "srt")
        outputFile.writer().use { o ->
            items.forEachIndexed { i, l ->
                l.write(i + 1, o)
            }
        }
    }

    companion object : SubtitleCompanion<SRTSubtitle> {

        override val extension = "srt"

        private val TIME = "\\s*(\\d{1,2}):(\\d{1,2}):(\\d{1,2}),(\\d{1,3})\\s*"
        private val REGEX =
            "(\\d+)\n$TIME-->$TIME\n(.*?)(?:\n\n|$)".toRegex(
                RegexOption.DOT_MATCHES_ALL
            )

        /**
         * Parses a [Duration] using the [MatchResult.groupValues] starting at [index]
         */
        private fun MatchResult.durationStartingAt(index: Int): Duration {
            val h = groupValues[index]
            val m = groupValues[index + 1]
            val s = groupValues[index + 2]
            val ms = groupValues[index + 3]
            return Duration.parse("PT${h}H${m}M${s}.${ms}S")
        }


        override fun parse(file: File): SRTSubtitle {
            return SRTSubtitle(
                REGEX.findAll(file.readText().replace("\r\n", "\n").replace('\r', '\n'))
                    .map {
                        SRTSubtitleText(
                            DurationSpan(
                                it.durationStartingAt(2),
                                it.durationStartingAt(6)
                            ),
                            it.groupValues[10]
                        )
                    }
                    .toList()
            )
        }
    }
}