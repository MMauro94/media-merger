package com.github.mmauro94.media_merger.util.log

import java.io.OutputStreamWriter

open class Logger(
    val log: (message: String, type: LogType) -> Unit,
    val debugWriter: OutputStreamWriter?
) : LoggerContainer<Logger> {

    override val baseLogger get() = this

    override fun create(logger: Logger) = logger

    operator fun invoke(message: String, type: LogType) {
        debug(message)
        log(message, type)
    }

    fun debug(message: String) {
        debugWriter?.appendln(message)
    }

    fun warn(message: String) = invoke(message, LogType.WARN)

    fun err(message: String) = invoke(message, LogType.ERR)

    fun prepend(str: String) = Logger({ message, type ->
        this@Logger.log(str + message, type)
    }, debugWriter)
}