package com.github.mmauro94.media_merger.config.converters

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.KlaxonException
import java.math.BigDecimal
import java.math.BigInteger

internal object BigDecimalConverter : Converter {

    override fun canConvert(cls: Class<*>) = cls == BigDecimal::class.java

    override fun fromJson(jv: JsonValue) = jv.string?.toBigDecimal() ?: jv.bigDecimal

    override fun toJson(value: Any): String {
        return if (value is BigDecimal) value.toString()
        else throw KlaxonException("Must be BigDecimal")
    }
}