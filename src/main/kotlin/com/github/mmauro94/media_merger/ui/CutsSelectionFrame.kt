package com.github.mmauro94.media_merger.ui

import com.github.mmauro94.media_merger.InputFile
import com.github.mmauro94.media_merger.cuts.Cuts
import com.github.mmauro94.media_merger.cuts.computeCuts
import com.github.mmauro94.media_merger.util.toTimeString
import com.github.mmauro94.media_merger.video_part.VideoParts
import com.github.mmauro94.media_merger.video_part.matchWithTarget
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.time.Duration
import javax.swing.*

class CutsSelectionFrame(
    val input: FileInfo,
    val target: FileInfo,
    val onSelected: (Cuts?) -> Unit
) : JFrame() {

    data class FileInfo(
        val inputFile: InputFile,
        val videoPartsProvider: (Duration) -> VideoParts
    )

    private val inputComp = VideoPartsComponent(true)
    private val cutsComp = CutsComponent()
    private val targetComp = VideoPartsComponent(false)
    private val accuracyLabel = JLabel()

    private fun recalcCuts() {
        val matches = targetComp.info!!.videoParts.let { targetParts -> inputComp.info?.videoParts?.matchWithTarget(targetParts) }
        if(matches != null) {
            cutsComp.cuts = matches.first.computeCuts()
            accuracyLabel.text = "Accuracy: %.2f".format(matches.second.accuracy)
        } else {
            cutsComp.cuts = null
            accuracyLabel.text = "Accuracy: --"
        }
    }

    init {
        inputComp.info = VideoPartsComponent.Info(input.inputFile, input.videoPartsProvider(Duration.ofSeconds(1)))
        targetComp.info = VideoPartsComponent.Info(target.inputFile, target.videoPartsProvider(Duration.ofSeconds(1)))
        recalcCuts()

        add(Box(BoxLayout.Y_AXIS).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

            add(SliderWithLabel(
                labelText = "Zoom",
                transform = { Zoom(it / 500.0) },
                formatValue = { "%.2fx".format(it.value) },
                onChange = {
                    inputComp.zoom = it
                    cutsComp.zoom = it
                    targetComp.zoom = it
                    revalidate()
                },
                JSlider().apply {
                    minimum = 10
                    maximum = 1000
                    value = 500
                }
            ).leftAlign())
            add(Box.createRigidArea(Dimension(0, 8)))
            add(minDurationSlider("Input min duration", input, inputComp, { recalcCuts() }).leftAlign())
            add(Box.createRigidArea(Dimension(0, 8)))
            add(minDurationSlider("Target min duration", target, targetComp, { recalcCuts() }).leftAlign())
            add(Box.createRigidArea(Dimension(0, 8)))
            add(
                JScrollPane(
                    Box(BoxLayout.Y_AXIS).apply {
                        add(JLabel("Input file (" + input.inputFile.file.absolutePath + ")").leftAlign())
                        add(inputComp.leftAlign())
                        add(cutsComp.leftAlign())
                        add(targetComp.leftAlign())
                        add(JLabel("Target file (" + target.inputFile.file.absolutePath + ")").leftAlign())
                        add(Box.createVerticalGlue())
                    },
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
                ).leftAlign().apply {
                    border = BorderFactory.createEmptyBorder()
                    maximumSize = Dimension(10000, preferredSize.height)
                }
            )
            add(accuracyLabel.leftAlign())
            add(Box.createVerticalGlue())
            add(Box.createRigidArea(Dimension(0, 8)))
            add(Box(BoxLayout.X_AXIS).apply {
                leftAlign()
                add(Box.createHorizontalGlue())
                add(JButton("Skip").apply {
                    addActionListener {
                        askCancel()
                    }
                })
                add(Box.createRigidArea(Dimension(8, 0)))
                add(JButton("OK").apply {
                    addActionListener {
                        onSelected(cutsComp.cuts)
                        close()
                    }
                })
            })
        })
        setSize(800, 400)
        setLocationRelativeTo(null)
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                askCancel()
            }
        })
    }

    private fun askCancel(): Boolean {
        return if (JOptionPane.showConfirmDialog(
                this,
                "Do you really wish to skip this group?",
                "Confirm skipping",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            ) == JOptionPane.YES_OPTION
        ) {
            onSelected(null)
            close()
            true
        } else false
    }

    private fun close() {
        isVisible = false
        dispose()
    }
}


private fun minDurationSlider(
    labelText: String,
    fileInfo: CutsSelectionFrame.FileInfo,
    videoPartsComponent: VideoPartsComponent,
    onChange: () -> Unit
): SliderWithLabel<Duration> {
    val slider = JSlider().apply {
        minimum = 0
        maximum = 5000
        value = 1000
    }
    return SliderWithLabel(
        labelText = labelText,
        transform = { Duration.ofMillis(it.toLong()) },
        formatValue = { it.toTimeString() },
        slider = slider,
        onChange = {
            if (!slider.valueIsAdjusting) {
                val file = videoPartsComponent.info?.file
                videoPartsComponent.info = if (file == null) null else VideoPartsComponent.Info(file, fileInfo.videoPartsProvider(it))
                onChange()
            }
        },
    )
}