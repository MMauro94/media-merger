package com.github.mmauro94.media_merger.util.json.converters

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.KlaxonException
import java.math.BigDecimal

internal object BigDecimalConverter : Converter {

    override fun canConvert(cls: Class<*>) = cls == BigDecimal::class.java

    override fun fromJson(jv: JsonValue) = jv.string?.toBigDecimal() ?: jv.bigDecimal ?: jv.double?.toBigDecimal()

    override fun toJson(value: Any): String {
        return if (value is BigDecimal) value.toString()
        else throw KlaxonException("Must be BigDecimal")
    }
}