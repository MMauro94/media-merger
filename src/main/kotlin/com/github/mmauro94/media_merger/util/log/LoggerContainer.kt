package com.github.mmauro94.media_merger.util.log

import java.io.OutputStreamWriter

interface LoggerContainer<T> {

    val baseLogger: Logger

    fun create(logger: Logger): T

    fun withDebug(writer: OutputStreamWriter?): T = create(Logger(baseLogger.log, writer))
}