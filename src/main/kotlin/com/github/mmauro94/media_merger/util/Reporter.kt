package com.github.mmauro94.media_merger.util

import com.github.mmauro94.media_merger.util.log.Logger
import com.github.mmauro94.media_merger.util.log.LoggerContainer
import com.github.mmauro94.media_merger.util.progress.ProgressHandler
import com.github.mmauro94.media_merger.util.progress.ProgressHandlerContainer

abstract class Reporter : ProgressHandlerContainer<Reporter>, LoggerContainer<Reporter> {
    abstract val progress: ProgressHandler
    abstract val log: Logger

    override val baseHandler get() = progress
    override val baseLogger get() = log

    override fun create(progressHandler: ProgressHandler) = object : Reporter() {
        override val progress = progressHandler
        override val log = this@Reporter.log
    }

    override fun create(logger: Logger) = object : Reporter() {
        override val progress = this@Reporter.progress
        override val log = logger
    }
}