package com.github.mmauro94.media_merger.video_part

import com.github.mmauro94.media_merger.config.FFMpegBlackdetectConfig
import java.math.BigDecimal

data class CachedBlackSegmentsConfig(val pictureBlackThreshold: BigDecimal?, val pixelBlackThreshold: BigDecimal?)

fun FFMpegBlackdetectConfig.toCachedConfig() = CachedBlackSegmentsConfig(pictureBlackThreshold, pixelBlackThreshold)