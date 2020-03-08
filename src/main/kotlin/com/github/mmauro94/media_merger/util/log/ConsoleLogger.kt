package com.github.mmauro94.media_merger.util.log

import com.github.mmauro94.media_merger.util.OUT
import org.fusesource.jansi.Ansi

object ConsoleLogger : Logger {

    override fun invoke(message: String, type: LogType) {
        OUT.println(Ansi.ansi().fg(type.color).a(message).reset())
    }
}