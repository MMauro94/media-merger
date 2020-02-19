package com.github.mmauro94.media_merger.group.show.provider

import com.github.mmauro94.media_merger.group.GroupInfoProvider
import com.github.mmauro94.media_merger.group.show.info.ShowInfo

/**
 * An interface that allows to download [ShowInfo] of a particular type
 * @param S the type of [ShowInfo] this provider downloads
 */
interface ShowProvider<S : ShowInfo> : GroupInfoProvider<S>

