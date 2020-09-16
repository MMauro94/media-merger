package com.github.mmauro94.media_merger.util.iter

class TransformIterator<T>(val iterator: Iterator<T>, val transform: (T) -> List<T>) : Iterator<T> {

    var list: MutableList<T>? = null

    override fun hasNext(): Boolean {
        return iterator.hasNext() || (!list.isNullOrEmpty())
    }

    override fun next(): T {
        return list.let { list ->
            val next = if (list.isNullOrEmpty()) {
                transform(iterator.next()).toMutableList().also {
                    check(it.isNotEmpty())
                    this.list = it
                }
            } else list
            next.removeAt(0)
        }
    }
}

fun <T> Iterator<T>.transform(transform: (T) -> List<T>): TransformIterator<T> {
    return TransformIterator(this, transform)
}