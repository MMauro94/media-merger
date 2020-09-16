package com.github.mmauro94.media_merger.util.cli.type

inline fun <reified A : T, reified B : T, reified T : Any> CliType<A>.or(other: CliType<B>): CliType<T> {
    return object : CliType<T>() {
        override fun parse(str: String): T? {
            return this@or.parse(str) ?: other.parse(str)
        }

        override fun toString(item: T): String {
            return when (item) {
                is A -> this@or.toString(item)
                is B -> other.toString(item)
                else -> error("Item must be either instance of " + A::class.simpleName + " or " + B::class.simpleName + ", " + item::class.simpleName + " found")
            }
        }
    }
}
