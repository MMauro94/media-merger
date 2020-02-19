package com.github.mmauro94.media_merger

enum class MediaType(val inputFileDetectorFactory: () -> InputFilesDetector<*>) {
    SHOW({ InputFilesDetector(com.github.mmauro94.media_merger.show.ShowGrouper.create()) }),
    MOVIE({ TODO() }),
    ANY({ TODO() });
}