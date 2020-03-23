package com.github.mmauro94.media_merger.util

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.util.log.ConsoleLogger
import com.github.mmauro94.media_merger.util.log.Logger
import com.github.mmauro94.media_merger.util.progress.ProgressHandler
import com.github.mmauro94.media_merger.util.progress.ProgressWithMessage
import org.fusesource.jansi.Ansi
import java.io.File
import kotlin.math.max

class ConsoleReporter : Reporter(), AutoCloseable {

    private val globalLog = if (Main.debug) File(Main.outputDir, "global_debug.txt").writer() else null
    private var closed = false
    private var last: Pair<ProgressWithMessage, Array<out ProgressWithMessage>>? = null
    private var maxSize = 0

    override val log = Logger({ message, type ->
        if (last != null) {
            OUT.print(Ansi.ansi().restoreCursorPosition())
        }
        ConsoleLogger.invoke(message, type)
        last?.let { (main, previouses) ->
            OUT.print(Ansi.ansi().saveCursorPosition())
            progress.handle(main, *previouses)
        }
    }, globalLog)

    override val progress = object : ProgressHandler {
        private fun handleSingle(progress: ProgressWithMessage) {
            OUT.print(Ansi.ansi().eraseLine(Ansi.Erase.FORWARD))
            if (progress.progress.ratio != null) {
                OUT.print(Ansi.ansi().fgBrightBlue().a((progress.progress.ratio * 100).toInt().toString().padStart(3) + '%'))
            } else {
                OUT.print(Ansi.ansi().fgBrightBlue().a("----"))
            }
            if (progress.message != null) {
                OUT.print(Ansi.ansi().fgDefault().a(", ${progress.message}").reset())
            }
            OUT.println()
        }

        override fun handle(main: ProgressWithMessage, vararg previouses: ProgressWithMessage) {
            if (last != null) {
                OUT.print(Ansi.ansi().restoreCursorPosition())
                repeat(max(0, maxSize - previouses.size)) {
                    OUT.println(Ansi.ansi().eraseLine(Ansi.Erase.FORWARD))
                }
            } else {
                OUT.print(Ansi.ansi().saveCursorPosition())
            }
            last = main to previouses
            maxSize = max(maxSize, previouses.size)
            previouses.reversedArray().forEach(::handleSingle)
            handleSingle(main)
        }
    }

    override fun close() {
        check(!closed)
        globalLog?.close()
        OUT.println()
    }
}