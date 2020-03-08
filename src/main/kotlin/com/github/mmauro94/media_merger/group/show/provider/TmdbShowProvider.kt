package com.github.mmauro94.media_merger.group.show.provider

import com.github.mmauro94.media_merger.Main
import com.github.mmauro94.media_merger.group.GroupInfoException
import com.github.mmauro94.media_merger.group.Service
import com.github.mmauro94.media_merger.group.show.info.EpisodeInfo
import com.github.mmauro94.media_merger.group.show.info.TmdbShow
import com.uwetrottmann.tmdb2.Tmdb

/**
 * [ShowProvider] for the TMDB service.
 */
object TmdbShowProvider : ShowProvider<TmdbShow> {

    override val service = Service.TMDB

    private val tmdb by lazy { Tmdb(service.apiKey()) }

    override fun search(query: String): List<TmdbShow> {
        val search = try {
            tmdb
                .searchService()
                .tv(
                    query,
                    1,
                    Main.config.infoLanguage?.iso639_1 ?: Main.mainLanguages.first().iso639_1 ?: "en",
                    null,
                    null
                )
                .execute()
        } catch (e: Exception) {
            throw GroupInfoException(e.message ?: "Unknown exception while searching on TMDB")
        }
        val body = search.body()

        return body?.results?.map {
            TmdbShow(it)
        } ?: throw GroupInfoException(
            search.errorBody()?.string() ?: "Unknown error from TMDB"
        )
    }

    private val seasons = HashMap<Pair<TmdbShow, Int>, List<EpisodeInfo>>()

    /**
     * Downloads a season
     * @throws GroupInfoException if the season cannot be downloaded
     */
    private fun downloadSeason(show: TmdbShow, season: Int): List<EpisodeInfo> {
        val response = try {
            tmdb.tvSeasonsService().season(
                show.id,
                season,
                Main.config.infoLanguage?.iso639_1 ?: Main.mainLanguages.first().iso639_1 ?: "en"
            ).execute()
        } catch (e: Exception) {
            throw GroupInfoException(e.message ?: "Unknown exception while downloading season from TMDB")
        }
        return response
            .body()
            ?.episodes
            ?.map {
                EpisodeInfo(
                    show,
                    season,
                    it.episode_number,
                    it.name
                )
            }
            ?: throw GroupInfoException(
                response.errorBody()?.string() ?: "Unknown error from TMDB"
            )
    }

    /**
     * Returns the episode info
     * @throws GroupInfoException if it cannot be downloaded
     */
    fun episodeInfo(show: TmdbShow, season: Int, episode: Int): EpisodeInfo {
        return seasons
            .getOrPut(show to season) {
                downloadSeason(show, season)
            }
            .singleOrNull { it.episodeNumber == episode }
            ?: throw GroupInfoException("Cannot find episode S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}")
    }

}