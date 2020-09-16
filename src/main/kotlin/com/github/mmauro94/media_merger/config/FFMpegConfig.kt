package com.github.mmauro94.media_merger.config

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.util.cli.askOrNullifyIf
import com.github.mmauro94.media_merger.util.cli.type.BigDecimalCliType
import com.github.mmauro94.media_merger.util.cli.type.DurationCliType
import java.math.BigDecimal
import java.time.Duration

data class FFMpegConfig(
    val hardwareAcceleration: String? = "auto",
    val blackdetect: FFMpegBlackdetectConfig = FFMpegBlackdetectConfig()
)

data class FFMpegBlackdetectThresholds(
    val pictureBlackThreshold: BigDecimal = BigDecimal("0.99"),
    val pixelBlackThreshold: BigDecimal = BigDecimal("0.01")
) {
    companion object {
        fun ask(): FFMpegBlackdetectThresholds {
            return FFMpegBlackdetectThresholds(
                pictureBlackThreshold = BigDecimalCliType.ask(
                    question = "Select min percentage (0-1) of black pixels",
                    default = Main.config.ffmpeg.blackdetect.thresholds.pictureBlackThreshold,
                    isValid = { this >= BigDecimal.ZERO && this <= BigDecimal.ONE }
                ),
                pixelBlackThreshold = BigDecimalCliType.ask(
                    question = "Select max luminance percentage (0-1) of single pixel",
                    default = Main.config.ffmpeg.blackdetect.thresholds.pixelBlackThreshold,
                    isValid = { this >= BigDecimal.ZERO && this <= BigDecimal.ONE }
                ),
            )
        }
    }
}

data class FFMpegBlackdetectConfig(
    val minDuration: Duration? = Duration.ofSeconds(1),
    val thresholds: FFMpegBlackdetectThresholds = FFMpegBlackdetectThresholds()
) {

    companion object {
        fun ask(): FFMpegBlackdetectConfig {
            return FFMpegBlackdetectConfig(
                minDuration = DurationCliType.asker(isValid = { !isNegative }).askOrNullifyIf(
                    nullifyString = "ask",
                    question = "Give min duration of black segments, or type 'ask' to ask for each group",
                    default = Main.config.ffmpeg.blackdetect.minDuration
                ),
                thresholds = FFMpegBlackdetectThresholds.ask()
            )
        }
    }
}