package com.github.mmauro94.shows_merger.show_provider

import com.github.mmauro94.shows_merger.MergeOptions
import com.github.mmauro94.shows_merger.show_info.EpisodeInfo
import com.github.mmauro94.shows_merger.show_info.ShowInfoException
import com.github.mmauro94.shows_merger.show_info.TvdbShow
import com.uwetrottmann.thetvdb.TheTvdb


object TvdbShowProvider : ShowProvider<TvdbShow> {

    private val tvdb by lazy {
        TheTvdb(ShowProvider.apiKey("TVDB"))
    }

    @Throws(ShowInfoException::class)
    override fun searchShow(query: String): List<TvdbShow> {
        val search = try {
            tvdb
                .search()
                .series(query, null, null, null, MergeOptions.MAIN_LANGUAGES.first().iso639_1 ?: "en")
                .execute()
        } catch (e: Exception) {
            throw ShowInfoException(e.message)
        }
        val body = search.body()

        return body?.data?.map {
            TvdbShow(it)
        } ?: throw ShowInfoException(
            search.errorBody()?.string() ?: ""
        )
    }

    private val episodes = HashMap<TvdbShow, List<EpisodeInfo>>()

    fun downloadEpisodes(show: TvdbShow, page: Int = 1): List<EpisodeInfo> {
        val response = try {
            tvdb.series().episodes(
                show.id,
                page,
                MergeOptions.MAIN_LANGUAGES.first().iso639_1 ?: "en"
            ).execute()
        } catch (e: Exception) {
            throw ShowInfoException(e.message)
        }
        val ret = response
            .body()
            ?.data
            ?.map {
                EpisodeInfo(
                    show,
                    it.airedSeason,
                    it.airedEpisodeNumber,
                    it.episodeName
                )
            } ?: throw ShowInfoException(
            response.errorBody()?.string() ?: ""
        )
        return if (ret.size >= 100) {
            ret + downloadEpisodes(show, page + 1)
        } else ret
    }

    @Throws(ShowInfoException::class)
    fun episodeInfo(show: TvdbShow, season: Int, episode: Int): EpisodeInfo {
        return episodes
            .getOrPut(show) {
                downloadEpisodes(show)
            }
            .singleOrNull { it.seasonNumber == season && it.episodeNumber == episode }
            ?: throw ShowInfoException("Cannot find episode S${season}E$episode")
    }

}