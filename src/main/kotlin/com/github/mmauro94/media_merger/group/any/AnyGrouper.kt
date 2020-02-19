package com.github.mmauro94.media_merger.group.any

import com.github.mmauro94.media_merger.group.Grouper

object AnyGrouper : Grouper<AnyGroup> {

    override fun detectGroup(filename: String): AnyGroup = AnyGroup

}