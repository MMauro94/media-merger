package com.github.mmauro94.shows_merger.subtitles.srt

import com.github.mmauro94.shows_merger.subtitles.PlainSubtitleLine
import com.github.mmauro94.shows_merger.util.DurationSpan
import java.io.OutputStreamWriter
import java.time.Duration
import kotlin.math.pow

class SRTSubtitleLine(time: DurationSpan, text: String) : PlainSubtitleLine(time, text) {

    private fun Duration.toSrtString(): String {
        return toHours().toString() + ":" +
                toMinutesPart().toString().padStart(2, '0') + ":" +
                toSecondsPart().toString().padStart(2, '0') + "," +
                toMillisPart().toString().padStart(3, '0')
    }

    fun write(index: Int, osw: OutputStreamWriter) {
        osw.appendln(index.toString())
        osw.appendln("${time.start.toSrtString()} --> ${time.end.toSrtString()}")
        osw.appendln(text)
        osw.appendln()
    }
}
