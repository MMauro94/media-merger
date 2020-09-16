package com.github.mmauro94.media_merger.ui

import com.github.mmauro94.media_merger.util.toTimeString
import com.github.mmauro94.media_merger.video_part.VideoPart
import com.github.mmauro94.media_merger.video_part.VideoParts
import com.github.mmauro94.media_merger.video_part.duration
import java.awt.*
import java.time.Duration
import javax.swing.JPanel
import kotlin.math.roundToInt

class VideoPartsComponent(
    val topMarks: Boolean
) : JPanel() {

    var zoom: Zoom = Zoom.DEFAULT
        set(value) {
            field = value
            update()
        }

    var parts: VideoParts? = null
        set(value) {
            field = value
            update()
        }

    override fun invalidate() {
        update()
        super.invalidate()
    }

    fun update() {
        parts?.let {
            preferredSize = Dimension(zoom.calcX(it.asIterable().duration()) + RIGHT_PADDING, 50)
            size = preferredSize
        }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        (g as Graphics2D).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        parts?.let { parts ->

            g.color = Color.LIGHT_GRAY
            g.fillRect(0, 0, width, height)

            for (part in parts) {
                g.color = when (part.type) {
                    VideoPart.Type.BLACK_SEGMENT -> Color.BLACK
                    VideoPart.Type.SCENE -> Color(0x29B6F6)
                }
                g.fillRect(zoom.calcX(part.time.start), 0, zoom.calcX(part.time.end), height)
            }

            var y1 = 0
            var y2 = 5
            val fontBounds = Rectangle()
            fontBounds.y = 8
            fontBounds.height = 8
            fontBounds.width = 16
            if (!topMarks) {
                y1 = height - y1
                y2 = height - y2
                fontBounds.y = height - fontBounds.y - fontBounds.height
            }
            g.color = Color.GREEN
            val mark = MARKS.asSequence()
                .map { it to zoom.msWidth * it }
                .filter { it.second > 10 }
                .minByOrNull { it.second }
            if (mark != null) {
                val w = mark.second
                if (w > 10) {
                    var prev = 0.0
                    var i = 0
                    while (prev < width - RIGHT_PADDING) {
                        prev += w
                        i++

                        val x = prev.roundToInt()
                        g.drawLine(x, y1, x, y2)
                        if (w > 24 || i % 2 == 0) {
                            fontBounds.x = x - 8
                            g.drawCenteredString(
                                fontBounds,
                                Duration.ofMillis(mark.first * i.toLong()).toTimeString(),
                                Font("sans-serif", Font.PLAIN, 8)
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val RIGHT_PADDING = 100
        private val MARKS = listOf(1000, 10000, 30000, 60000, 300000)
    }
}