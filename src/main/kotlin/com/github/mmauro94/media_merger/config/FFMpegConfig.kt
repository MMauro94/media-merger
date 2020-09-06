package com.github.mmauro94.media_merger.config

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.util.askBigDecimal
import com.github.mmauro94.media_merger.util.askDuration
import java.math.BigDecimal
import java.time.Duration

data class FFMpegConfig(
    val hardwareAcceleration: String? = "auto",
    val blackdetect: FFMpegBlackdetectConfig = FFMpegBlackdetectConfig()
)

data class FFMpegBlackdetectConfig(
    val minDuration: Duration = Duration.ofSeconds(1),
    val pictureBlackThreshold: BigDecimal = BigDecimal("0.99"),
    val pixelBlackThreshold: BigDecimal = BigDecimal("0.05")
) {

    companion object {
        fun ask(): FFMpegBlackdetectConfig {
            return FFMpegBlackdetectConfig(
                minDuration = askDuration(
                    question = "Give min duration of black segments",
                    default = Main.config.ffmpeg.blackdetect.minDuration
                ),
                pictureBlackThreshold = askBigDecimal(
                    question = "Select min percentage (0-1) of black pixels",
                    default = Main.config.ffmpeg.blackdetect.pictureBlackThreshold,
                    isValid = { this >= BigDecimal.ZERO && this <= BigDecimal.ONE }
                ),
                pixelBlackThreshold = askBigDecimal(
                    question = "Select max luminance percentage (0-1) of single pixel",
                    default = Main.config.ffmpeg.blackdetect.pixelBlackThreshold,
                    isValid = { this >= BigDecimal.ZERO && this <= BigDecimal.ONE }
                ),
            )
        }
    }
}