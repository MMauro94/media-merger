package com.github.mmauro94.media_merger.util.log

import com.github.mmauro94.media_merger.Main
import java.io.File

open class Logger(
    val log: (message: String, type: LogType) -> Unit,
    val debugTransform: (String) -> String = { it },
    val debugFile: File? = null,
    printToDebug: Boolean = Main.debug
) : LoggerContainer<Logger> {

    private val debugLogs = mutableListOf<String>()
    var printToDebug = printToDebug; private set

    override val baseLogger get() = this

    override fun create(logger: Logger) = logger

    operator fun invoke(message: String, type: LogType) {
        debug(message)
        log(message, type)
    }

    fun debug(message: String = "") {
        if (debugFile != null) {
            if (printToDebug) {
                debugFile.appendText(debugTransform(message) + "\n")
            } else {
                debugLogs.add(debugTransform(message))
            }
        }
    }

    fun forceDebug() {
        if (debugFile != null) {
            if (!printToDebug) {
                printToDebug = true
                debugFile.appendText(debugLogs.joinToString("\n") + "\n")
                debugLogs.clear()
            }
        }
    }

    fun warn(message: String) = invoke(message, LogType.WARN)

    fun err(message: String) = invoke(message, LogType.ERR)
}