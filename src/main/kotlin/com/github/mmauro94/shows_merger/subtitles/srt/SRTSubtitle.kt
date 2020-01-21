package com.github.mmauro94.shows_merger.subtitles.srt

import com.github.mmauro94.shows_merger.subtitles.Subtitle
import com.github.mmauro94.shows_merger.subtitles.SubtitleCompanion
import com.github.mmauro94.shows_merger.util.DurationSpan
import java.io.File
import java.time.Duration

class SRTSubtitle(lines: List<SRTSubtitleLine>) : Subtitle<SRTSubtitleLine>(lines) {

    override fun SRTSubtitleLine.changeTime(time: DurationSpan): SRTSubtitleLine {
        return SRTSubtitleLine(time, text)
    }

    override fun withLines(lines: List<SRTSubtitleLine>): Subtitle<SRTSubtitleLine> {
        return SRTSubtitle(lines)
    }

    override fun save(outputFile: File) {
        require(outputFile.extension == "srt")
        outputFile.writer().use { o ->
            lines.forEachIndexed { i, l ->
                l.write(i, o)
            }
        }
    }

    companion object : SubtitleCompanion<SRTSubtitle> {

        override val extension = "srt"

        private val NEWLINE = "(?:\\n|\\r|\\r\\n)"
        private val REGEX =
            "(\\d+)\\s*$NEWLINE(\\d{1,2}):(\\d{1,2}):(\\d{1,2}),(\\d{1,3})\\s*-->\\s*(\\d{1,2}):(\\d{1,2}):(\\d{1,2}),(\\d{1,3})\\s*$NEWLINE(.*?)(?:$NEWLINE$NEWLINE|\$)".toRegex(
                RegexOption.DOT_MATCHES_ALL
            )

        private fun MatchResult.durationStartingAt(i: Int): Duration {
            val h = groupValues[i]
            val m = groupValues[i + 1]
            val s = groupValues[i + 2]
            val ms = groupValues[i + 3]
            return Duration.parse("PT${h}H${m}M${s}.${ms}S")
        }


        override fun parse(file: File): SRTSubtitle {
            return SRTSubtitle(
                REGEX.findAll(file.readText())
                    .map {
                        SRTSubtitleLine(
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