package com.github.mmauro94.media_merger.util.json.converters

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.KlaxonException
import com.github.mmauro94.media_merger.util.parseTimeStringOrNull
import com.github.mmauro94.media_merger.util.toSecondsDuration
import com.github.mmauro94.media_merger.util.toTimeString
import java.time.Duration

object DurationConverter : Converter {

    override fun canConvert(cls: Class<*>) = cls == Duration::class.java

    override fun fromJson(jv: JsonValue): Duration? {
        return if (jv.inside == null) null
        else
            jv.string?.toBigDecimalOrNull()?.toSecondsDuration()
                ?: jv.string?.parseTimeStringOrNull()
                ?: jv.int?.toBigDecimal()?.toSecondsDuration()
         ?: throw KlaxonException("Invalid duration")
    }

    override fun toJson(value: Any): String {
        return if (value is Duration) "\"" + value.toTimeString() + "\""
        else throw KlaxonException("Must be a Duration")
    }
}