package com.github.mmauro94.media_merger.group.show.info

import com.github.mmauro94.media_merger.group.GroupInfoException
import com.github.mmauro94.media_merger.group.show.provider.TvdbShowProvider
import com.uwetrottmann.thetvdb.entities.Series
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.SignStyle
import java.time.temporal.ChronoField

/**
 * A show downloaded by THETVDB
 */
data class TvdbShow(private val tvdbShow: Series) : ShowInfo() {
    val id = tvdbShow.id!!

    val firstAired: LocalDate? = tvdbShow.firstAired?.let {
        try {
            LocalDate.parse(it, FIRST_AIRED_PARSER)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    override val year = firstAired?.let { Year.from(it) }

    override val givenName = tvdbShow.seriesName ?: ""

    override val name = computeName(
        "TVDB",
        id.toString(),
        tvdbShow.seriesName,
        year
    )

    override fun toString() = super.toString()

    @Throws(GroupInfoException::class)
    override fun episodeInfo(season: Int, episode: Int): EpisodeInfo {
        return TvdbShowProvider.episodeInfo(this, season, episode)
    }

    companion object {
        private val FIRST_AIRED_PARSER = DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NORMAL)
            .toFormatter()
    }
}