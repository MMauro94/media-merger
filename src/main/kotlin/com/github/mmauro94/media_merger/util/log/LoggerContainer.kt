package com.github.mmauro94.media_merger.util.log

import com.github.mmauro94.media_merger.Main
import java.io.File

interface LoggerContainer<T> {

    val baseLogger: Logger

    fun create(logger: Logger): T

    fun prepend(str: String) = create(
        Logger(
            log = { message, type -> baseLogger.log(message.split("\n").joinToString("\n") { str + it }, type) },
            debugFile = baseLogger.debugFile
        )
    )

    fun prependDebug(str: String) = create(
        Logger(
            log = baseLogger.log,
            debugTransform = { message -> message.split("\n").joinToString("\n") { str + it } },
            debugFile = baseLogger.debugFile
        )
    )

    fun <R> withDebug(file: File, block: (T) -> R): R {
        val w = if (Main.debug) file.writer()
        else null
        val logger = Logger(baseLogger.log, baseLogger.debugTransform, file to w)

        try {
            return block(create(logger))
        } finally {
            logger.debugFile?.second?.close()
        }
    }
}

inline fun <T, R> LoggerContainer<T>.withPrependDebug(str: String, block: T.() -> R): R {
    return block(prependDebug(str))
}