package com.github.mmauro94.media_merger.group

/**
 * Class thrown when there is a problem downloading the group info
 */
class GroupInfoException(override val message: String?) : Exception(message)