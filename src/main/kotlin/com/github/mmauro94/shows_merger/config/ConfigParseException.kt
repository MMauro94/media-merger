package com.github.mmauro94.shows_merger.config

/**
 * Exception thrown when then is a problem reading the config file.
 * @see Config
 */
class ConfigParseException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)