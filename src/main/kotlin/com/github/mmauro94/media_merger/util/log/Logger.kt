package com.github.mmauro94.media_merger.util.log

interface Logger {
    operator fun invoke(message: String, type: LogType)

    fun warn(message: String) = invoke(message, LogType.WARN)

    fun err(message: String) = invoke(message, LogType.ERR)

    fun prepend(str: String) = object : Logger {
        override fun invoke(message: String, type: LogType) {
            print(str)
            this@Logger.invoke(message, type)
        }
    }
}