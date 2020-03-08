package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.group.Grouper
import com.github.mmauro94.media_merger.util.Reporter
import com.github.mmauro94.media_merger.util.progress.ProgressHandler

class InputFilesDetector<G : Group<G>>(val grouper: Grouper<G>) {

    private var inputFiles: List<InputFiles<G>>? = null

    fun getOrReadInputFiles(reporter: Reporter) = inputFiles.let {
        it ?: InputFiles.detect(grouper, Main.workingDir, reporter).sorted().apply {
            inputFiles = this
        }
    }

    fun reloadFiles(reporter: Reporter): List<InputFiles<G>> {
        inputFiles = null
        return getOrReadInputFiles(reporter)
    }
}