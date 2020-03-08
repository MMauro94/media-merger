package com.github.mmauro94.media_merger.util

import com.github.mmauro94.media_merger.util.log.Logger
import com.github.mmauro94.media_merger.util.progress.ProgressHandler
import com.github.mmauro94.media_merger.util.progress.ProgressSplitter

abstract class Reporter : ProgressSplitter<Reporter> {
    abstract val progress: ProgressHandler
    abstract val log: Logger

    override val baseHandler get() = progress

    override fun create(progressHandler: ProgressHandler) = object : Reporter() {
        override val progress = progressHandler
        override val log = this@Reporter.log
    }
}