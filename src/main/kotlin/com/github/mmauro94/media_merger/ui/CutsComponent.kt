package com.github.mmauro94.media_merger.ui

import com.github.mmauro94.media_merger.cuts.Cuts
import java.awt.*
import javax.swing.JPanel

class CutsComponent : JPanel() {

    var cuts: Cuts? = null
        set(value) {
            field = value
            update()
        }
    var zoom: Zoom = Zoom.DEFAULT
        set(value) {
            field = value
            update()
        }

    fun update() {
        cuts?.let { cuts ->
            val duration = cuts.cuts.maxOf { maxOf(it.time.end, it.targetTime.end) }
            preferredSize = Dimension(zoom.calcX(duration), 50)
            size = preferredSize
        }
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        (g as Graphics2D).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setPaintMode()

        cuts?.let { cuts ->
            for ((i, cut) in cuts.cuts.withIndex()) {
                g.color = if (i % 2 == 0) Color.GRAY else Color.WHITE
                g.fillPolygon(
                    intArrayOf(
                        zoom.calcX(cut.time.start),
                        zoom.calcX(cut.time.end),
                        zoom.calcX(cut.targetTime.end),
                        zoom.calcX(cut.targetTime.start)
                    ),
                    intArrayOf(0, 0, height, height),
                    4
                )
                //g.fillRect(zoom.calcX(cut.time.start), 0, zoom.calcX(cut.time.end), height)

            }
        }
    }
}