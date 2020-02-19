package com.github.mmauro94.media_merger.group.show

import com.github.mmauro94.media_merger.*
import com.github.mmauro94.media_merger.group.Grouper
import com.github.mmauro94.media_merger.group.show.info.ShowInfo
import com.github.mmauro94.media_merger.group.GroupInfoException
import com.github.mmauro94.media_merger.group.show.provider.ShowProvider
import com.github.mmauro94.media_merger.group.show.provider.TmdbShowProvider
import com.github.mmauro94.media_merger.group.show.provider.TvdbShowProvider
import java.util.regex.Pattern

class ShowGrouper(val show: ShowInfo?) : Grouper<Episode> {

    override fun detectGroup(filename: String): Episode? {
        val m = PATTERN.matcher(filename)
        return if (m.find()) {
            val s = (m.group(1) ?: m.group(3))?.toInt() ?: throw IllegalStateException()
            val e = (m.group(2) ?: m.group(4))?.toInt() ?: throw IllegalStateException()
            if (m.find()) null else {
                val epInfo = try {
                    show?.episodeInfo(s, e)
                } catch (e: GroupInfoException) {
                    System.err.println(e.message)
                    null
                }
                Episode(s, e, epInfo)
            }
        } else null
    }

    companion object {
        private val PATTERN = Pattern.compile("(?:(\\d+)x(\\d+)|S(\\d+)E(\\d+))", Pattern.CASE_INSENSITIVE)!!


        fun create(): ShowGrouper {
            return ShowGrouper(Grouper.select("show", TvdbShowProvider, TmdbShowProvider))
        }
    }
}