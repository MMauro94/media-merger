package com.github.mmauro94.media_merger.config

import com.github.mmauro94.media_merger.util.toTimeString
import java.math.BigDecimal
import java.time.Duration

data class FFMpegConfig(
    val hardwareAcceleration: String? = "auto",
    val blackdetect: FFMpegBlackdetectConfig = FFMpegBlackdetectConfig()
)

data class FFMpegBlackdetectConfig(
    val minDuration: Duration = Duration.ofMillis(100),
    val pictureBlackThreshold: BigDecimal? = BigDecimal("0.99"),
    val pixelBlackThreshold: BigDecimal? = null
) {

    fun toFilenameString() = buildString {
        append("minDuration_" + minDuration.toTimeString('.'))
        pictureBlackThreshold?.let {
            append("@pic_th_" + it.toPlainString())
        }
        pixelBlackThreshold?.let {
            append("@pix_th_" + it.toPlainString())
        }
    }
}