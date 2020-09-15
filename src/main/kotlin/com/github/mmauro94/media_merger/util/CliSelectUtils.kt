package com.github.mmauro94.media_merger.util

import com.github.mmauro94.media_merger.util.ask.ask
import org.fusesource.jansi.Ansi.ansi
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.functions

fun <T : Any> select(
    question: String,
    options: List<T>,
    defaultValue: T? = null,
    long: Boolean = false,
    nameProvider: (T) -> String = { it.toString() }
): T {
    require(defaultValue == null || defaultValue in options)
    if (long) {
        println(question)
        options.forEachIndexed { i, value ->
            println(ansi().fgBrightCyan().a("${i + 1}").reset().a(") ${nameProvider(value)}"))
        }
        return options[Int.ask(
            question = "Selection:",
            isValid = { this in 1..(options.size) },
            default = defaultValue?.let { options.indexOf(it) + 1 },
            itemToString = { "$this (${nameProvider(options[this - 1])})" },
            defaultToString = { toString() }
        ) - 1].also {
            println()
        }
    } else {
        val reverseMap = options.associateBy(nameProvider)
        while (true) {
            val str = String.ask(
                question = ansi().fgDefault().a(question).fgBrightCyan().a(" (" + reverseMap.keys.joinToString(", ") + ")").reset().toString(),
                default = defaultValue?.let(nameProvider) ?: "",
                isValid = { this in reverseMap }
            )
            return reverseMap.getValue(str)
        }
    }
}

inline fun <reified E : Enum<E>> selectEnum(
    question: String,
    defaultValue: E? = null,
    long: Boolean = false,
    noinline nameProvider: (E) -> String = { it.name.toLowerCase() }
): E {
    return select(
        question = question,
        options = enumValues<E>().asList(),
        defaultValue = defaultValue,
        long = long,
        nameProvider = nameProvider
    )
}

inline fun <reified T : Any> selectSealedClass(
    question: String,
    defaultValue: KClass<T>? = null,
    long: Boolean = false,
    noinline nameProvider: (KClass<out T>) -> String = {
        it.annotations.filterIsInstance(CliDescriptor::class.java).singleOrNull()?.description ?: it.toString()
    }
): T {
    val selectedKClass = select(
        question = question,
        defaultValue = defaultValue,
        options = T::class.sealedSubclasses,
        long = long,
        nameProvider = nameProvider
    )
    selectedKClass.objectInstance?.let { return it }
    val companionObject = selectedKClass.companionObject!!
    return companionObject.functions.single { it.name == "ask" }.call(companionObject.objectInstance) as T
}

@Target(AnnotationTarget.CLASS)
annotation class CliDescriptor(val description: String)
