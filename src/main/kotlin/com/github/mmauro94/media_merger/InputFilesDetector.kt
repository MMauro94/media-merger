package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.group.Group
import com.github.mmauro94.media_merger.group.Grouper

class InputFilesDetector<G : Group<G>>(val grouper: Grouper<G>) {

    private var inputFiles: List<InputFiles<G>>? = null

    fun getOrReadInputFiles() = inputFiles.let {
        it ?: InputFiles.detect(grouper, Main.workingDir).sorted().apply {
            inputFiles = this
        }
    }

    fun reloadFiles(): List<InputFiles<G>> {
        inputFiles = null
        return getOrReadInputFiles()
    }
}