package com.github.mmauro94.media_merger.ui

import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JSlider

open class SliderWithLabel<T>(
    private val labelText: String,
    private val transform: (Int) -> T,
    private val formatValue: (T) -> String,
    private val onChange: (T) -> Unit,
    private val slider: JSlider
) : Box(BoxLayout.Y_AXIS) {

    private val label = JLabel()

    init {
        add(label.leftAlign())
        add(createRigidArea(Dimension(0, 2)))
        add(slider.apply {
            leftAlign()
            addChangeListener {
                val value = transform(slider.value)
                refreshLabel(value)
                onChange(value)
            }
        })
        refreshLabel(transform(slider.value))
    }

    final override fun add(comp: Component?): Component {
        return super.add(comp)
    }

    private fun refreshLabel(value: T) {
        label.text = labelText + ": (" + formatValue(value) + ")"
    }
}