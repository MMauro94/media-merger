package com.github.mmauro94.media_merger.util.iter

open class CachedIterator<T>(private val iterator: Iterator<T>) : ListIterator<T> {

    open val cache = mutableListOf<T>()

    var nextIndex = 0
        private set

    protected open fun addToCache(item: T) : T {
        cache.add(item)
        return item
    }

    override fun hasNext(): Boolean {
        return if (nextIndex in cache.indices) true
        else iterator.hasNext()
    }

    override fun hasPrevious(): Boolean {
        return nextIndex > 0
    }

    override fun next(): T {
        val ret = value {
            addToCache(iterator.next())
        }
        nextIndex++
        return ret
    }

    private inline fun value(otherwise: () -> T) =
        if (nextIndex in cache.indices) cache[nextIndex] else otherwise()

    override fun nextIndex(): Int {
        return nextIndex
    }

    override fun previous(): T {
        nextIndex--
        return value { throw NoSuchElementException() }
    }

    override fun previousIndex(): Int {
        return nextIndex - 1
    }

    fun peek(): T {
        return next().also { previous() }
    }

    fun reset() {
        nextIndex = 0
    }

    open fun copy(): CachedIterator<T> {
        val parent = this
        return object : CachedIterator<T>(this) {
            override val cache = parent.cache.toMutableList()
        }
    }

    inline fun skipIf(condition: (T) -> Boolean) {
        if (hasNext() && condition(peek())) {
            next()
        }
    }

    inline fun skipWhile(condition: (T) -> Boolean) {
        while (hasNext() && condition(peek())) {
            next()
        }
    }

    fun goTo(nextIndex: Int) {
        when {
            nextIndex < 0 -> error("nextIndex < 0")
            nextIndex in 0..cache.size -> this.nextIndex = nextIndex
            else -> {
                while (this.nextIndex != nextIndex) {
                    next()
                }
            }
        }
    }
}

fun <T> Iterator<T>.toCachedIterator() = CachedIterator(this)
