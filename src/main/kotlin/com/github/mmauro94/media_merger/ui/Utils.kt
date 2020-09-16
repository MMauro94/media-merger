package com.github.mmauro94.media_merger.ui

import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.font.FontRenderContext
import javax.swing.Box
import javax.swing.JComponent
import kotlin.math.roundToInt

fun Graphics.drawCenteredString(rect: Rectangle, string: String, font: Font) {
    val frc = FontRenderContext(null, true, true)
    val r2D = font.getStringBounds(string, frc)
    val rWidth = r2D.width.roundToInt()
    val rHeight = r2D.height.roundToInt()
    val rX = r2D.x.roundToInt()
    val rY = r2D.y.roundToInt()
    val a = rect.width / 2 - rWidth / 2 - rX
    val b = rect.height / 2 - rHeight / 2 - rY
    this.font = font
    drawString(string, rect.x + a, rect.y + b)
}

fun <T : JComponent> T.leftAlign() = apply { alignmentX = Box.LEFT_ALIGNMENT }
