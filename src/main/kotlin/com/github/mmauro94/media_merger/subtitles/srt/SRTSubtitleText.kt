package com.github.mmauro94.media_merger.subtitles.srt

import com.github.mmauro94.media_merger.subtitles.PlainSubtitleText
import com.github.mmauro94.media_merger.util.DurationSpan
import java.io.OutputStreamWriter
import java.time.Duration

/**
 * An SRT subtitle text
 */
class SRTSubtitleText(time: DurationSpan, text: String) : PlainSubtitleText(time, text) {

    /**
     * Converts a [Duration] to a [String] with the format used in SRT files.
     */
    private fun Duration.toSrtString(): String {
        return toHours().toString() + ":" +
                toMinutesPart().toString().padStart(2, '0') + ":" +
                toSecondsPart().toString().padStart(2, '0') + "," +
                toMillisPart().toString().padStart(3, '0')
    }

    /**
     * Writes this SRT text in the specified [outputStreamWriter]
     * @param index the index of the subtitle item
     */
    fun write(index: Int, outputStreamWriter: OutputStreamWriter) {
        outputStreamWriter.appendln(index.toString())
        outputStreamWriter.appendln("${time.start.toSrtString()} --> ${time.end.toSrtString()}")
        outputStreamWriter.appendln(text)
        outputStreamWriter.appendln()
    }
}
