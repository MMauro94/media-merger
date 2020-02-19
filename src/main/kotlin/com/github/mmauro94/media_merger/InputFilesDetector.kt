package com.github.mmauro94.media_merger

class InputFilesDetector<G : Group<G>>(val grouper: Grouper<G>) {

    private var inputFiles: List<InputFiles<G>>? = null

    fun inputFiles() = inputFiles!!

    fun getOrReadInputFiles() = inputFiles.let {
        it ?: InputFiles.detect(grouper, Main.workingDir).sorted().apply {
            inputFiles = this
        }
    }

    fun reloadFiles() {
        inputFiles = null
        getOrReadInputFiles()
    }
}