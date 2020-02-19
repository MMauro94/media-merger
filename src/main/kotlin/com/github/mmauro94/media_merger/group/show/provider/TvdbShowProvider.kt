package com.github.mmauro94.media_merger.group.show.provider

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.group.GroupInfoException
import com.github.mmauro94.media_merger.group.Service
import com.github.mmauro94.media_merger.group.show.info.EpisodeInfo
import com.github.mmauro94.media_merger.group.show.info.TvdbShow
import com.uwetrottmann.thetvdb.TheTvdb


object TvdbShowProvider : ShowProvider<TvdbShow> {

    override val service = Service.TVDB

    private val tvdb by lazy { TheTvdb(service.apiKey()) }

    override fun search(query: String): List<TvdbShow> {
        val search = try {
            tvdb
                .search()
                .series(query, null, null, null, Main.mainLanguages.first().iso639_1 ?: "en")
                .execute()
        } catch (e: Exception) {
            throw GroupInfoException(e.message)
        }
        val body = search.body()

        return body?.data?.map {
            TvdbShow(it)
        } ?: throw GroupInfoException(
            search.errorBody()?.string() ?: ""
        )
    }

    private val episodes = HashMap<TvdbShow, List<EpisodeInfo>>()

    /**
     * Downloads all the episodes info
     * @throws GroupInfoException if they cannot be downloaded
     */
    fun downloadEpisodes(show: TvdbShow, page: Int = 1): List<EpisodeInfo> {
        val response = try {
            tvdb.series().episodes(
                show.id,
                page,
                Main.mainLanguages.first().iso639_1 ?: "en"
            ).execute()
        } catch (e: Exception) {
            throw GroupInfoException(e.message)
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
            } ?: throw GroupInfoException(
            response.errorBody()?.string() ?: ""
        )
        return if (ret.size >= 100) {
            ret + downloadEpisodes(show, page + 1)
        } else ret
    }

    /**
     * Returns the episode info
     * @throws GroupInfoException if it cannot be downloaded
     */
    fun episodeInfo(show: TvdbShow, season: Int, episode: Int): EpisodeInfo {
        return episodes
            .getOrPut(show) {
                downloadEpisodes(show)
            }
            .singleOrNull { it.seasonNumber == season && it.episodeNumber == episode }
            ?: throw GroupInfoException("Cannot find episode S${season}E$episode")
    }

}