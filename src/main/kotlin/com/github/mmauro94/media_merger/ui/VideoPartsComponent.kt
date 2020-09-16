package com.github.mmauro94.media_merger.ui

import com.github.mmauro94.media_merger.InputFile
import com.github.mmauro94.media_merger.util.toTimeString
import com.github.mmauro94.media_merger.video_part.VideoPart
import com.github.mmauro94.media_merger.video_part.VideoParts
import com.github.mmauro94.media_merger.video_part.duration
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.time.Duration
import javax.swing.JPanel
import kotlin.math.roundToInt

class VideoPartsComponent(
    val topMarks: Boolean
) : JPanel() {

    data class Info(
        val file: InputFile,
        val videoParts: VideoParts
    )

    init {
        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                info?.videoParts?.let { parts ->
                    for (part in parts) {
                        if (e.x > zoom.calcX(part.time.start) && e.x < zoom.calcX(part.time.end)) {
                            hoveredPart = part
                            repaint()
                            break
                        }
                    }
                }
            }
        })
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                hoveredPart?.let { vp ->
                    info?.let { info ->
                        VideoPartDialog(info.file.file, vp).apply {
                            isVisible = true
                        }
                    }
                }
            }

            override fun mouseExited(e: MouseEvent) {
                hoveredPart = null
                repaint()
            }
        })
    }

    var zoom: Zoom = Zoom.DEFAULT
        set(value) {
            field = value
            update()
        }

    var hoveredPart: VideoPart? = null
    var info: Info? = null
        set(value) {
            hoveredPart = null
            field = value
            update()
        }

    fun update() {
        info?.videoParts?.let {
            preferredSize = Dimension(zoom.calcX(it.asIterable().duration()) + RIGHT_PADDING, 50)
        }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        (g as Graphics2D).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        info?.videoParts?.let { parts ->

            g.color = Color.LIGHT_GRAY
            g.fillRect(0, 0, width, height)

            for (part in parts) {
                g.color = when (part.type) {
                    VideoPart.Type.BLACK_SEGMENT -> if (part == hoveredPart) Color(0x616161) else Color.BLACK
                    VideoPart.Type.SCENE -> if (part == hoveredPart) Color(0x81D4FA) else Color(0x29B6F6)
                }
                val start = zoom.calcX(part.time.start)
                val end = zoom.calcX(part.time.end)
                g.fillRect(start, 0, end - start, height)
                g.color = Color.RED
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
                    while (prev < width) {
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