package com.github.mmauro94.media_merger.config

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.KlaxonException
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage
import com.github.mmauro94.media_merger.util.find

/**
 * Converter object needed to parse from a JSON a [MkvToolnixLanguage]
 */
object MkvToolnixLanguageConverter : Converter {

    override fun canConvert(cls: Class<*>) = cls == MkvToolnixLanguage::class.java

    override fun fromJson(jv: JsonValue) =
        jv.string?.let {
            MkvToolnixLanguage.find(it) ?: throw KlaxonException("Invalid language code $it")
        }

    override fun toJson(value: Any): String {
        require(value is MkvToolnixLanguage)
        return "\"" + value.iso639_2 + "\""
    }
}