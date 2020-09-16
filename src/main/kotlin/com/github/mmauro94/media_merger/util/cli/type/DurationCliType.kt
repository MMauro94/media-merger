package com.github.mmauro94.media_merger.util.cli.type

import com.github.mmauro94.media_merger.util.parseTimeStringOrNull
import com.github.mmauro94.media_merger.util.toTimeString
import java.time.Duration

object DurationCliType : CliType<Duration>() {
    override fun parse(str: String) = str.parseTimeStringOrNull() ?: str.toLongOrNull()?.let { Duration.ofSeconds(it) }
    override fun toString(item: Duration) = item.toTimeString()
}