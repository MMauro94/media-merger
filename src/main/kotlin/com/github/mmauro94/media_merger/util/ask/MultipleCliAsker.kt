package com.github.mmauro94.media_merger.util.ask

class MultipleCliAsker<T : Any, C : MutableCollection<T>>(
    private val itemAsker: AbstractCliAsker<T>,
    private val collectionCreator: () -> C,
    private val separator: Pair<String, Regex> = ", " to Regex(",\\s*")
) : AbstractCliAsker<C>() {

    override fun parse(str: String, default: C?): C? {
        val things: C = collectionCreator()
        str.split(separator.second).forEach { item ->
            val thing = itemAsker.parse(item, null)
            if (thing == null) {
                return null
            } else {
                things.add(thing)
            }
        }
        return if (things.isNotEmpty()) things else null
    }

    override fun itemToString(item: C): String {
        return item.joinToString(separator = separator.first, transform = { itemAsker.itemToString(it) })
    }
}

