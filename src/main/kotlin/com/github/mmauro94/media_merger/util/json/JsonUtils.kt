package com.github.mmauro94.media_merger.util.json

import com.beust.klaxon.FieldRenamer
import com.beust.klaxon.JsonBase
import com.beust.klaxon.Klaxon
import com.github.mmauro94.media_merger.util.json.converters.BigDecimalConverter
import com.github.mmauro94.media_merger.util.json.converters.DurationConverter
import com.github.mmauro94.media_merger.util.json.converters.MkvToolnixLanguageConverter

val KLAXON = Klaxon()
    .converter(MkvToolnixLanguageConverter)
    .converter(DurationConverter)
    .converter(BigDecimalConverter)
    .fieldRenamer(object : FieldRenamer {
        override fun fromJson(fieldName: String) = FieldRenamer.underscoreToCamel(fieldName)
        override fun toJson(fieldName: String) = FieldRenamer.camelToUnderscores(fieldName)
    })

fun Klaxon.toPrettyJsonString(value: Any): String {
    val builder = StringBuilder(KLAXON.toJsonString(value))
    return (KLAXON.parser().parse(builder) as JsonBase).toJsonString(true)
}