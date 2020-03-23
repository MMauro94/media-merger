package com.github.mmauro94.media_merger.util.log

import org.fusesource.jansi.Ansi

enum class LogType(val color : Ansi.Color) {
    WARN(Ansi.Color.YELLOW),
    ERR(Ansi.Color.RED)
}