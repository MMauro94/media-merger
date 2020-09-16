package com.github.mmauro94.media_merger.util.iter

class TakeUntilIterator<T>(val iterator: Iterator<T>, val condition: (T) -> Boolean) : Iterator<T> {

    private inner class Holder(val item: T)

    private var nextHolder: Holder? = null
    private var stop = false

    private fun advanceIfNecessary() {
        nextHolder.let { nh ->
            if (nh == null && !stop) {
                if (iterator.hasNext()) {
                    val next = iterator.next()
                    if (!condition(next)) {
                        stop = true
                        return
                    } else {
                        nextHolder = Holder(next)
                    }
                }
            }
        }
    }

    override fun hasNext(): Boolean {
        advanceIfNecessary()
        return nextHolder != null
    }

    override fun next(): T {
        advanceIfNecessary()
        return nextHolder.let { nh ->
            if (nh == null) throw NoSuchElementException()
            else {
                nextHolder = null
                nh.item
            }
        }
    }
}

fun <T> Iterator<T>.takeUntil(condition: (T) -> Boolean): TakeUntilIterator<T> {
    return TakeUntilIterator(this, condition)
}