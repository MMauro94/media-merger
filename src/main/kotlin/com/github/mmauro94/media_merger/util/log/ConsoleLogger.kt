package com.github.mmauro94.media_merger.util.log

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.util.OUT
import org.fusesource.jansi.Ansi
import java.io.File

object ConsoleLogger : Logger(
    { message, type ->
        OUT.println(Ansi.ansi().fg(type.color).a(message).reset())
    }, null
)