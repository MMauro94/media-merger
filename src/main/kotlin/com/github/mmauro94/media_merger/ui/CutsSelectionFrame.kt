package com.github.mmauro94.media_merger.ui

import com.github.mmauro94.media_merger.cuts.Cuts
import com.github.mmauro94.media_merger.cuts.computeCuts
import com.github.mmauro94.media_merger.util.toTimeString
import com.github.mmauro94.media_merger.video_part.VideoParts
import com.github.mmauro94.media_merger.video_part.matchWithTarget
import java.awt.Dimension
import java.time.Duration
import javax.swing.*

class CutsSelectionFrame(
    val inputVideoPartsProvider: (Duration) -> VideoParts,
    val targetVideoPartsProvider: (Duration) -> VideoParts,
    val onSelected: (Cuts?) -> Unit
) : JFrame() {

    init {
        val input = VideoPartsComponent(true)
        val cuts = CutsComponent()
        val target = VideoPartsComponent(false)

        fun recalcCuts() {
            val matches = target.parts?.let { targetParts -> input.parts?.matchWithTarget(targetParts) }
            cuts.cuts = matches?.first?.computeCuts()
        }
        input.parts = inputVideoPartsProvider(Duration.ofSeconds(1))
        target.parts = targetVideoPartsProvider(Duration.ofSeconds(1))
        recalcCuts()

        add(Box(BoxLayout.Y_AXIS).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

            add(SliderWithLabel(
                labelText = "Zoom",
                transform = { Zoom(it / 500.0) },
                formatValue = { "%.2fx".format(it.value) },
                onChange = {
                    input.zoom = it
                    cuts.zoom = it
                    target.zoom = it
                    revalidate()
                },
                JSlider().apply {
                    minimum = 10
                    maximum = 1000
                    value = 500
                }
            ).leftAlign())
            add(Box.createRigidArea(Dimension(0, 8)))
            add(minDurationSlider("Input min duration", inputVideoPartsProvider, input, { recalcCuts() }).leftAlign())
            add(Box.createRigidArea(Dimension(0, 8)))
            add(minDurationSlider("Target min duration", targetVideoPartsProvider, target, { recalcCuts() }).leftAlign())
            add(Box.createRigidArea(Dimension(0, 8)))
            add(
                JScrollPane(
                    Box(BoxLayout.Y_AXIS).apply {
                        add(JLabel("Input file (PATH)").leftAlign())
                        add(input.leftAlign())
                        add(cuts.leftAlign())
                        add(target.leftAlign())
                        add(JLabel("Target file (PATH)").leftAlign())
                        add(Box.createVerticalGlue())
                    },
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
                ).leftAlign().apply {
                    border = BorderFactory.createEmptyBorder()
                    maximumSize = Dimension(10000, preferredSize.height)
                }
            )
            add(Box.createVerticalGlue())
            add(Box.createRigidArea(Dimension(0, 8)))
            add(Box(BoxLayout.X_AXIS).apply {
                leftAlign()
                add(Box.createHorizontalGlue())
                add(JButton("Skip").apply {
                    addActionListener {
                        if(JOptionPane.showConfirmDialog(
                            this,
                            "Do you really wish to skip this group?",
                            "Confirm skipping",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                        ) == JOptionPane.YES_OPTION) {
                            onSelected(null)
                            this@CutsSelectionFrame.isVisible = false
                        }
                    }
                })
                add(Box.createRigidArea(Dimension(8, 0)))
                add(JButton("OK").apply {
                    addActionListener {
                        onSelected(cuts.cuts)
                        this@CutsSelectionFrame.isVisible = false
                    }
                })
            })
        })
        setSize(800, 400)
        setLocationRelativeTo(null)
    }
}


private fun minDurationSlider(
    labelText: String,
    videoPartsProvider: (Duration) -> VideoParts,
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
                videoPartsComponent.parts = videoPartsProvider(it)
                onChange()
            }
        },
    )
}