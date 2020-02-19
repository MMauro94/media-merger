package com.github.mmauro94.media_merger.show.info

/**
 * Class thrown when there is a problem downloading the show info
 */
class ShowInfoException(override val message: String?) : Exception(message)