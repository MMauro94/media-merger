package com.github.mmauro94.media_merger.util.cli.type

class CollectionCliType<T : Any, C : MutableCollection<T>>(
    private val itemType: CliType<T>,
    private val collectionCreator: () -> C,
    private val separator: Pair<String, Regex> = ", " to Regex(",\\s*")
) : CliType<C>() {

    override fun parse(str: String): C? {
        val things: C = collectionCreator()
        str.split(separator.second).forEach { item ->
            val thing = itemType.parse(item)
            if (thing == null) {
                return null
            } else {
                things.add(thing)
            }
        }
        return if (things.isNotEmpty()) things else null
    }

    override fun toString(item: C): String {
        return item.joinToString(separator = separator.first, transform = { itemType.toString(it) })
    }
}

