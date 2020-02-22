package com.github.mmauro94.media_merger.config.converters

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.KlaxonException
import com.github.mmauro94.media_merger.util.toSecondsDuration
import com.github.mmauro94.media_merger.util.toTotalSeconds
import java.time.Duration

object DurationConverter : Converter {

    override fun canConvert(cls: Class<*>) = cls == Duration::class.java

    override fun fromJson(jv: JsonValue) = (jv.string?.toBigDecimal())?.toSecondsDuration()

    override fun toJson(value: Any): String {
        return if (value is Duration) "\"" + value.toTotalSeconds() + "\""
        else throw KlaxonException("Must be a Duration")
    }
}