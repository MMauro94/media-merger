package com.github.mmauro94.media_merger

import com.github.mmauro94.media_merger.group.show.ShowGrouper
import com.github.mmauro94.media_merger.group.movie.MovieGrouper
import com.github.mmauro94.media_merger.group.any.AnyGrouper

enum class MediaType(val inputFileDetectorFactory: () -> InputFilesDetector<*>) {
    SHOW({ InputFilesDetector(ShowGrouper.create()) }),
    MOVIE({ InputFilesDetector(MovieGrouper.create()) }),
    ANY({ InputFilesDetector(AnyGrouper) });
}