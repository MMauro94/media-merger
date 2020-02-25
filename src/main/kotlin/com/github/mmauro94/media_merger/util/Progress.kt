package com.github.mmauro94.media_merger.util

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi

typealias ProgressReporter = (Progress) -> Unit


sealed class Progress(val progress: Float?, val message: String?) {
    init {
        require(progress == null || progress in 0.0..1.0)
    }
}

class IndeterminateProgress(message: String?) : Progress(progress = null, message = message)

class DiscreteProgress(val current: Int, val max: Int, message: String?) : Progress(
    progress = if (max == 0) 1f else current / max.toFloat(),
    message = message
) {

    init {
        require(current >= 0)
        require(max >= 0)
        require(current <= max)
    }
}

class ConsoleProgressHandler {

    private var first = true
    operator fun invoke(progress: Progress) {
        if (!first) {
            print(ansi().restoreCursorPosition().eraseLine(Ansi.Erase.FORWARD).saveCursorPosition())
        } else {
            print(ansi().saveCursorPosition())
            first = false
        }
        if (progress.progress != null) {
            print(ansi().fgBrightBlue().format("%.0f%%", progress.progress * 100))
            if (progress.message != null) {
                print(ansi().fgDefault().a(", ${progress.message}").reset())
            }
            println()
            if(progress.progress == 1f) {
                println()
            }
        }
    }
}