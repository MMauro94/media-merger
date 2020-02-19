package com.github.mmauro94.media_merger.show

import com.github.mmauro94.media_merger.*
import com.github.mmauro94.media_merger.show.info.ShowInfo
import com.github.mmauro94.media_merger.show.info.ShowInfoException
import com.github.mmauro94.media_merger.show.provider.ShowProvider
import com.github.mmauro94.media_merger.show.provider.TmdbShowProvider
import com.github.mmauro94.media_merger.show.provider.TvdbShowProvider
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
                } catch (e: ShowInfoException) {
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
            return ShowGrouper(selectTvShow())
        }

        private fun selectTvShow(): ShowInfo? {
            if (!askYesNo("Select show?", true)) {
                return null
            }

            val showProvider = when (askOption("Select show info provider", listOf("tmdb", "tvdb"))) {
                "tmdb" -> TmdbShowProvider
                "tvdb" -> TvdbShowProvider
                else -> throw IllegalStateException()
            }
            return selectTvShow(showProvider)
        }

        private fun selectTvShow(showProvider: ShowProvider<*>): ShowInfo? {
            val q = askString("Name of TV show to search:")
            val results = try {
                showProvider.searchShow(q)
            } catch (e: ShowInfoException) {
                System.err.println("Error searching for show")
                if (e.message != null && e.message.isNotBlank()) {
                    System.err.println(e.message)
                }
                return selectTvShow()
            }
            menu(
                items = results.map { it.name } + "-- Search again --",
                onSelection = {
                    return if (it == results.size) {
                        selectTvShow(showProvider)
                    } else results[it]
                },
                exitName = "None"
            )
            return null
        }
    }
}