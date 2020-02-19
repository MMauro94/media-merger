package com.github.mmauro94.media_merger.group.movie.provider

import com.github.mmauro94.media_merger.group.GroupInfoProvider
import com.github.mmauro94.media_merger.group.movie.info.MovieInfo

/**
 * An interface that allows to download [MovieInfo] of a particular type
 * @param M the type of [MovieInfo] this provider downloads
 */
interface MovieProvider<M : MovieInfo> : GroupInfoProvider<M>

