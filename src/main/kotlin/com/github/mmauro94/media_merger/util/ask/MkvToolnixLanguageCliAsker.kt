package com.github.mmauro94.media_merger.util.ask

import com.github.mmauro94.media_merger.util.find
import com.github.mmauro94.mkvtoolnix_wrapper.MkvToolnixLanguage

object MkvToolnixLanguageCliAsker : AbstractCliAsker<MkvToolnixLanguage>() {
    override fun parse(str: String, default: MkvToolnixLanguage?) = MkvToolnixLanguage.find(str)
    override fun itemToString(item: MkvToolnixLanguage) = item.iso639_2
}

fun MkvToolnixLanguage.Companion.ask(
    question: String,
    default: MkvToolnixLanguage? = null,
    isValid: MkvToolnixLanguage.() -> Boolean = { true },
    itemToString: MkvToolnixLanguage.() -> String = { MkvToolnixLanguageCliAsker.itemToString(this) },
    defaultToString: MkvToolnixLanguage.() -> String = { MkvToolnixLanguageCliAsker.defaultToString(this) }
): MkvToolnixLanguage {
    return MkvToolnixLanguageCliAsker.ask(question, default, isValid, itemToString, defaultToString)
}

fun MkvToolnixLanguage.Companion.askLinkedHashSet(
    question: String,
    defaultValue: LinkedHashSet<MkvToolnixLanguage>? = null,
    isValid: LinkedHashSet<MkvToolnixLanguage>.() -> Boolean = { true }
): LinkedHashSet<MkvToolnixLanguage> {
    return MkvToolnixLanguageCliAsker.toMultiple(
        collectionCreator = { LinkedHashSet() },
    ).ask(
        question = question,
        default = defaultValue,
        isValid = isValid,
    )
}
