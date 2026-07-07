package fr.univ_lille.iut_info

import java.util.function.Function

interface IIterator<T> : Iterator<T>, Iterable<T> {
    override fun iterator(): Iterator<T> {
        return this
    }

    fun <R> map(transform: (T) -> R): IIterator<R> {
        return MapIterator(this.copy().apply { reset() }, transform)
    }

    fun filterI(filter: (T) -> Boolean): IIterator<T> {
        return flatMapI { if (filter(it)) singleton(it) else empty() }
    }

    fun <R> foldI(initial: R, operation: (acc: R, T) -> R): R {
        var accumulator = initial
        for (element in this) accumulator = operation(accumulator, element)
        return accumulator
    }

    fun <R> flatMapI(transform: (T) -> IIterator<R>): IIterator<R> {
        val mapIterator =
            MapIterator<T, IIterator<R>>(this.copy().apply { reset() }) { p -> transform(p) }
        return mapIterator.flatten()
    }

    fun reset()

    fun copy(): IIterator<T>

    fun freshCopy() = copy().apply { reset() }

    companion object {

        fun <T> Iterable<T>.toIIterator(): IIterator<T> {
            if (this is List) return fromList(this)
            return fromList(toList())
        }

        fun <T> singleton(value: T): IIterator<T> {
            return ListIterator(listOf(value))
        }

        fun <T> empty(): IIterator<T> {
            return ListIterator(emptyList())
        }

        fun <T> fromList(list: List<T>): IIterator<T> {
            return ListIterator(list)
        }

        fun <T> iterator(vararg iterators: T): IIterator<T> {
            return fromList(iterators.toList())
        }

        fun <T> flat(vararg iterators: IIterator<T>): IIterator<T> {
            return fromList(iterators.toList().map { it.copy().apply { reset() } }).flatten()
        }

        fun <T> IIterator<IIterator<T>>.flatten(): IIterator<T> {
            return FlattenIterator(this)
        }
    }

    private class MapIterator<T, U>(val iter: IIterator<T>, val func: Function<T, U>) : IIterator<U> {
        override fun reset() {
            iter.reset()
        }

        override fun next(): U {
            return func.apply(iter.next())
        }

        override fun hasNext(): Boolean {
            return iter.hasNext()
        }

        override fun copy(): IIterator<U> {
            return MapIterator(iter.copy(), func)
        }

    }

    private class FlattenIterator<T>(val iter: IIterator<IIterator<T>>) : IIterator<T> {

        var current: IIterator<T>? = null

        fun updateCurrent() {
            var localCurrent = current
            while ((localCurrent == null || !localCurrent.hasNext()) && iter.hasNext()) localCurrent = iter.next().apply { reset() }
            current = if (localCurrent == null || !localCurrent.hasNext()) null else localCurrent
        }

        override fun reset() {
            iter.reset()
            current = null
        }

        override fun hasNext(): Boolean {
            updateCurrent()
            return current != null
        }

        override fun next(): T {
            updateCurrent()
            return current?.next() ?: throw NoSuchElementException()
        }

        override fun copy(): IIterator<T> {
            val iter = FlattenIterator(this.iter.copy())
            iter.current = current?.copy()
            return iter
        }
    }

    private class ListIterator<T>(val list: List<T>) : IIterator<T> {
        var index = 0

        override fun reset() {
            index = 0
        }

        override fun next(): T {
            return list[index++]
        }

        override fun hasNext(): Boolean {
            return index < list.size
        }

        override fun copy(): IIterator<T> {
            val iter = ListIterator(list)
            iter.index = index
            return iter
        }
    }
}