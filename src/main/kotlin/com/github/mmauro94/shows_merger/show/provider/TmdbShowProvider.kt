package com.github.mmauro94.shows_merger.show.provider

import com.github.mmauro94.shows_merger.MergeOptions
import com.github.mmauro94.shows_merger.show.info.EpisodeInfo
import com.github.mmauro94.shows_merger.show.info.ShowInfoException
import com.github.mmauro94.shows_merger.show.info.TmdbShow
import com.uwetrottmann.tmdb2.Tmdb

/**
 * [ShowProvider] for the TMDB service.
 */
object TmdbShowProvider : ShowProvider<TmdbShow> {

    private val tmdb by lazy {
        Tmdb(ShowProvider.apiKey("TMDB"))
    }

    override fun searchShow(query: String): List<TmdbShow> {
        val search = try {
            tmdb
                .searchService()
                .tv(query, 1, MergeOptions.MAIN_LANGUAGES.first().iso639_1 ?: "en", null, null)
                .execute()
        } catch (e: Exception) {
            throw ShowInfoException(e.message)
        }
        val body = search.body()

        return body?.results?.map {
            TmdbShow(it)
        } ?: throw ShowInfoException(
            search.errorBody()?.string() ?: ""
        )
    }

    private val seasons = HashMap<Pair<TmdbShow, Int>, List<EpisodeInfo>>()

    /**
     * Downloads a season
     * @throws ShowInfoException if the season cannot be downloaded
     */
    private fun downloadSeason(show: TmdbShow, season: Int): List<EpisodeInfo> {
        val response = try {
            tmdb.tvSeasonsService().season(
                show.id,
                season,
                MergeOptions.MAIN_LANGUAGES.first().iso639_1 ?: "en"
            ).execute()
        } catch (e: Exception) {
            throw ShowInfoException(e.message)
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
            ?: throw ShowInfoException(
                response.errorBody()?.string() ?: ""
            )
    }

    /**
     * Returns the episode info
     * @throws ShowInfoException if it cannot be downloaded
     */
    fun episodeInfo(show: TmdbShow, season: Int, episode: Int): EpisodeInfo {
        return seasons
            .getOrPut(show to season) {
                downloadSeason(show, season)
            }
            .singleOrNull { it.episodeNumber == episode }
            ?: throw ShowInfoException("Cannot find episode S${season}E$episode")
    }

}