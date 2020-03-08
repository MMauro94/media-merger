package com.github.mmauro94.media_merger.group.any

import com.github.mmauro94.media_merger.group.Grouper
import com.github.mmauro94.media_merger.util.log.Logger

object AnyGrouper : Grouper<AnyGroup> {

    override fun detectGroup(filename: String, logger : Logger): AnyGroup = AnyGroup

}