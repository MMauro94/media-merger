package com.github.mmauro94.shows_merger

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class AudioAdjustment(
    val track: Track,
    val adjustment: Adjustment
) {
    val outputExtension = track.mkvTrack.codec.toLowerCase().let { c ->
        when {
            c.contains("ac-3") -> "ac3"
            c.contains("aac") -> "aac"
            c.contains("mp3") -> "mp3"
            c.contains("flac") -> "flac"
            else -> "mkv"
        }
    }

    val outputFile = File(
        track.file.parentFile,
        track.file.nameWithoutExtension +
                "_" + track.id +
                "_" + adjustment.stretchFactor.ratio.toString().replace('.', '_') +
                "_" + track.language.iso639_2 +
                ".$outputExtension"
    )

    /**
     * Example command to cut audio (no blanks yet):
     * ffmpeg -vn -sn -i file.mkv -filter_complex `
        "[0:a]atrim=duration=30[aa];`
         [0:a]atrim=start=40:end=50,asetpts=PTS-STARTPTS[ba];`
         [aa][ba]concat=v=0:a=1[outa]" `
       -map [outa] audio.ac3
     */

    fun adjust(progress: String): Boolean {
        val ratio = adjustment.stretchFactor.ratio
        return if (ratio.compareTo(BigDecimal.ONE) != 0) {
            val builder = FFmpegBuilder()
                .setInput(track.file.absolutePath)
                .addOutput(outputFile.absolutePath)
                .disableVideo()
                .disableSubtitle()
                .addExtraArgs("-map", "0:${track.id}")
                .setAudioFilter("atempo=$ratio")
                .done()

            println(builder.build().joinToString(" "))
            FFmpegExecutor(FFmpeg(), FFprobe()).apply {
                createJob(builder) { prg ->
                    //val percentage = prg.out_time_ns / adjustment.targetDuration.toNanos().toDouble()
                    val percentage = 0 //TODO

                    println(
                        String.format(
                            "[%s, %.0f%%] %s, speed:%.2fx",
                            progress,
                            percentage * 100.0,
                            FFmpegUtils.toTimecode(prg.out_time_ns, TimeUnit.NANOSECONDS),
                            prg.speed
                        )
                    )
                }.run()
            }
            true
        } else false
    }
}