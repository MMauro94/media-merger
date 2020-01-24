package com.github.mmauro94.shows_merger.show.info

/**
 * Class thrown when there is a problem downloading the show info
 */
class ShowInfoException(override val message: String?) : Exception(message)