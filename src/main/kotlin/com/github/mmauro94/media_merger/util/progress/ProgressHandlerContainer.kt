package com.github.mmauro94.media_merger.util.progress

interface ProgressHandlerContainer<T> {

    val baseHandler: ProgressHandler

    fun create(progressHandler: ProgressHandler): T

    fun split(current: Int, max: Int, message: String?): T {
        require(current < max)
        return split(
            globalProgressSpan = ProgressSpan(
                start = Progress.of(current, max),
                end = Progress.of(current + 1, max)
            ),
            message = message
        )
    }

    fun split(frozenGlobalProgress: Progress, message: String?): T {
        return split(ProgressSpan(frozenGlobalProgress, frozenGlobalProgress), message)
    }

    fun split(globalRatioStart: Float, globalRatioEnd: Float, message: String?): T {
        require(globalRatioStart <= globalRatioEnd)
        return split(
            ProgressSpan(
                Progress(globalRatioStart),
                Progress(globalRatioEnd)
            ), message
        )
    }

    fun split(globalProgressSpan: ProgressSpan, message: String?): T {
        return create(object : ProgressHandler {
            override fun handle(main: ProgressWithMessage, vararg previouses: ProgressWithMessage) {
                this@ProgressHandlerContainer.baseHandler.handle(
                    ProgressWithMessage(
                        progress = globalProgressSpan.interpolate(main.progress.ratio ?: 0f),
                        message = message
                    ),
                    main,
                    *previouses
                )
            }
        })
    }
}