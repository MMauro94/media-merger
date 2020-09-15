package com.github.mmauro94.media_merger.util.ask

import com.github.mmauro94.media_merger.util.parseTimeStringOrNull
import com.github.mmauro94.media_merger.util.toTimeString
import java.time.Duration

object DurationCliAsker : AbstractCliAsker<Duration>() {
    override fun parse(str: String, default: Duration?) = str.parseTimeStringOrNull() ?: str.toLongOrNull()?.let { Duration.ofSeconds(it) }
    override fun itemToString(item: Duration) = item.toTimeString()
}