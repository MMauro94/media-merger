package com.github.mmauro94.media_merger.util.log

import java.io.File
import java.io.OutputStreamWriter

open class Logger(
    val log: (message: String, type: LogType) -> Unit,
    val debugTransform: (String) -> String = { it },
    debugFile: Pair<File, OutputStreamWriter?>? = null
) : LoggerContainer<Logger> {

    private val logs = mutableListOf<String>()
    var debugFile: Pair<File, OutputStreamWriter?>? = debugFile; private set

    override val baseLogger get() = this

    override fun create(logger: Logger) = logger

    operator fun invoke(message: String, type: LogType) {
        debug(message)
        log(message, type)
    }

    fun debug(message: String = "") {
        debugFile?.second.let {
            it?.appendln(debugTransform(message)) ?: logs.add(debugTransform(message))
        }
    }

    fun forceDebug() {
        debugFile?.let {
            if (it.second == null) {
                val writer = it.first.writer()
                debugFile = it.first to writer
                for (log in logs) {
                    writer.appendln(log)
                }
                writer.flush()
                logs.clear()
            }
        }
    }

    fun warn(message: String) = invoke(message, LogType.WARN)

    fun err(message: String) = invoke(message, LogType.ERR)
}