package com.github.mmauro94.media_merger.group.any

import com.github.mmauro94.media_merger.group.Group

object AnyGroup : Group<AnyGroup> {

    override fun outputName() : Nothing? = null

    override fun toString() = "All files"

    override fun compareTo(other: AnyGroup) = 0
}