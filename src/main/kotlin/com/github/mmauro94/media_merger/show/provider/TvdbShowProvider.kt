package com.github.mmauro94.media_merger.show.provider

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.show.info.EpisodeInfo
import com.github.mmauro94.media_merger.show.info.ShowInfoException
import com.github.mmauro94.media_merger.show.info.TvdbShow
import com.uwetrottmann.thetvdb.TheTvdb


object TvdbShowProvider : ShowProvider<TvdbShow> {

    private val tvdb by lazy {
        TheTvdb(ShowProvider.apiKey("TVDB"))
    }

    override fun searchShow(query: String): List<TvdbShow> {
        val search = try {
            tvdb
                .search()
                .series(query, null, null, null, Main.mainLanguages.first().iso639_1 ?: "en")
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

    /**
     * Downloads all the episodes info
     * @throws ShowInfoException if they cannot be downloaded
     */
    fun downloadEpisodes(show: TvdbShow, page: Int = 1): List<EpisodeInfo> {
        val response = try {
            tvdb.series().episodes(
                show.id,
                page,
                Main.mainLanguages.first().iso639_1 ?: "en"
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

    /**
     * Returns the episode info
     * @throws ShowInfoException if it cannot be downloaded
     */
    fun episodeInfo(show: TvdbShow, season: Int, episode: Int): EpisodeInfo {
        return episodes
            .getOrPut(show) {
                downloadEpisodes(show)
            }
            .singleOrNull { it.seasonNumber == season && it.episodeNumber == episode }
            ?: throw ShowInfoException("Cannot find episode S${season}E$episode")
    }

}