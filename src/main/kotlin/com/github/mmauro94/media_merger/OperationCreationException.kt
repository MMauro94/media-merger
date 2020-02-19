package com.github.mmauro94.media_merger

import java.io.File

class OperationCreationException(
    message: String,
    val writeToFile : String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

    fun printTo(file: File) {
        file.parentFile.mkdirs()
        file.printWriter().use { pw ->
            pw.appendln(message)
            if(writeToFile != null) {
                pw.appendln()
                pw.appendln(writeToFile)
            }
            pw.appendln()
            printStackTrace(pw)
        }
    }
}