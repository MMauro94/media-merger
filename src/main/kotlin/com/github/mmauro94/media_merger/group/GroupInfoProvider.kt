package com.github.mmauro94.media_merger.group

/**
 * An interface that allows to download information of a particular type
 * @param INFO the type of info this provider downloads
 */
interface GroupInfoProvider<out INFO> {

    val service : Service

    /**
     * Searches for a show with the given [query] and returns a list of results.
     * @throws GroupInfoException if the search gives an error
     */
    fun search(query: String): List<INFO>
}

