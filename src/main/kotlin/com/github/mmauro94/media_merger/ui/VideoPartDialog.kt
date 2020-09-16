package com.github.mmauro94.media_merger.ui

import com.github.mmauro94.media_merger.util.DurationSpan
import com.github.mmauro94.media_merger.util.newTmpFile
import com.github.mmauro94.media_merger.util.toTimeString
import com.github.mmauro94.media_merger.util.toTotalSeconds
import com.github.mmauro94.media_merger.video_part.VideoPart
import com.github.mmauro94.media_merger.video_part.VideoPart.Type.BLACK_SEGMENT
import com.github.mmauro94.media_merger.video_part.VideoPart.Type.SCENE
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.ImageObserver
import java.io.File
import java.io.IOException
import java.time.Duration
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt


class VideoPartDialog(val file: File, val videoPart: VideoPart) : JDialog() {

    val seekLabel = JLabel()
    var seek: Duration = Duration.ZERO
        set(value) {
            field = value
            redetectImage()
        }
    var img: Image? = null
        set(value) {
            field = value
            revalidate()
            repaint()
        }

    private fun updateSeekLabel(seek: Duration = this.seek) {
        val timecode = videoPart.time.start + seek
        seekLabel.text = buildString {
            append("Seek frame: (")
            append(timecode.toTimeString())
            append("; ")
            if (seek.isNegative) {
                append("-")
            } else {
                append("+")
            }
            append(seek.toTimeString())
            append(")")
        }
    }

    init {
        add(Box(BoxLayout.Y_AXIS).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

            addInfo("Start time", videoPart.time.start.toTimeString())
            add(Box.createRigidArea(Dimension(0, 8)))
            addInfo("End time", videoPart.time.end.toTimeString())
            add(Box.createRigidArea(Dimension(0, 8)))
            addInfo("Duration", videoPart.time.duration.toTimeString())
            add(Box.createRigidArea(Dimension(0, 8)))
            addInfo(
                "Type", when (videoPart.type) {
                    BLACK_SEGMENT -> "Black segment"
                    SCENE -> "Scene"
                }
            )
            add(Box.createRigidArea(Dimension(0, 8)))
            add(seekLabel.leftAlign())
            updateSeekLabel()
            add(JSlider().apply {
                leftAlign()
                minimum = 0
                maximum = videoPart.time.duration.toMillis().toInt()
                value = 0
                addChangeListener {
                    val s = Duration.ofMillis(value.toLong())
                    updateSeekLabel(s)
                    if (!valueIsAdjusting) {
                        seek = s
                    }
                }
            })
            add(Box.createRigidArea(Dimension(0, 8)))


            val sizeObserver = ImageObserver { _, infoFlags, _, _, _, _ ->
                if (infoFlags.and(ImageObserver.HEIGHT) != 0 && infoFlags.and(ImageObserver.WIDTH) != 0) {
                    repaint()
                    false
                } else true
            }

            fun getImageDimension(): Dimension? {
                val imgH = img?.getHeight(sizeObserver)
                val imgW = img?.getWidth(sizeObserver)
                return if (imgH != null && imgH > 0 && imgW != null && imgW > 0) {
                    Dimension(imgW, imgH)
                } else null
            }

            val imagePanel = object : JPanel() {

                init {
                    addComponentListener(object : ComponentAdapter() {
                        override fun componentResized(e: ComponentEvent) {
                            calcSize()
                        }
                    })
                }

                private fun calcSize() {
                    val dim = getImageDimension()
                    preferredSize = if (dim == null) {
                        Dimension(width, ((width / 16.0) * 9.0).roundToInt())
                    } else {
                        Dimension(width, ((width / dim.width.toFloat()) * dim.height).roundToInt())
                    }
                }

                override fun invalidate() {
                    calcSize()
                    super.invalidate()
                }

                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    g.color = Color.BLACK
                    g.drawRect(0, 0, width - 1, height - 1)
                    img.let { img ->
                        val dim = getImageDimension()
                        if (img == null || dim == null) {
                            g.drawCenteredString(Rectangle(width, height), "Loading image...", Font("sans-serif", Font.PLAIN, 16))
                        } else {
                            val panelRatio = width.toFloat() / height.toFloat()
                            val imgRatio = dim.width.toFloat() / dim.height.toFloat()
                            if (panelRatio < imgRatio) {
                                val newHeight = (1 / imgRatio) * width
                                g.drawImage(img, 0, ((height - newHeight) / 2).roundToInt(), width, newHeight.roundToInt(), null)
                            } else {
                                val newWidth = height * imgRatio
                                g.drawImage(img, ((width - newWidth) / 2).roundToInt(), 0, newWidth.roundToInt(), height, null)
                            }
                        }
                    }
                }
            }
            add(imagePanel.leftAlign())
            add(Box.createVerticalGlue())
            add(Box.createRigidArea(Dimension(0, 8)))

            add(Box(BoxLayout.X_AXIS).apply {
                leftAlign()
                add(Box.createHorizontalGlue())
                add(JButton("Close").apply {
                    addActionListener {
                        this@VideoPartDialog.isVisible = false
                        dispose()
                    }
                })
            })
        })

        title = "Video part info"
        isModal = true
        redetectImage()
        setSize(500, 400)
        setLocationRelativeTo(null)
    }

    private fun redetectImage() {
        val seek = this.seek
        thread(start = true) {
            val output = File(newTmpFile().absolutePath + ".bmp")
            val builder = FFmpegBuilder()
                .addExtraArgs("-ss", (videoPart.time.start + seek).toTotalSeconds())
                .setInput(file.absolutePath)
                .addOutput(output.absolutePath)
                .addExtraArgs("-frames:v", "1")
                .done()
            FFmpegExecutor(FFmpeg(), FFprobe()).apply {
                createJob(builder).run()
            }
            val img = try {
                ImageIO.read(output)
            } catch (ioe: IOException) {
                JOptionPane.showMessageDialog(
                    this@VideoPartDialog,
                    ioe.stackTraceToString(),
                    "Error reading image",
                    JOptionPane.ERROR_MESSAGE
                )
                null
            }
            if (seek == this.seek) {
                this.img = img
            }
        }
    }

    private fun Box.addInfo(text: String, info: String) {
        add(Box(BoxLayout.LINE_AXIS).apply {
            leftAlign()
            add(JLabel(text).apply {
                //preferredSize = Dimension(100, 0)
                //maximumSize = Dimension(100, 10000)
                preferredSize = Dimension(60, 0)
            })
            add(JTextField(info).apply {
                isEnabled = false
                maximumSize = Dimension(10000, 30)
            })
        })
    }
}

fun main() {
    VideoPartDialog(
        File("C:\\Users\\molin\\Desktop\\TO MERGE\\pic\\eng\\Star Trek Picard S01E06 The Impossible Box 1080p AMZN WEBrip x265 DDP5.1 D0ct0rLew[SEV].mkv"),
        VideoPart(DurationSpan(Duration.ofMinutes(1), Duration.ofMinutes(2)), SCENE)
    ).apply {
        isVisible = true
    }
}