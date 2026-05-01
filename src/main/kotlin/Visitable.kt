package fr.univ_lille.iut_info

import java.util.function.Function

interface Visitable<T : Visitable<T>> {
    fun accept(visitor: Visitor<T>): T
}

interface Visitor<T> {
    fun visit(obj: T): T
}

fun <T : Visitable<T>> T.visit(
    visitor: ((node: T, rec: ((root: T) -> T)) -> T?)
): T {
    class DefaultVisitor : Visitor<T> {
        override fun visit(obj: T): T {
            val visited = visitor(obj) { visit(it) }
            if (visited != null) return visited
            return obj.accept(this)
        }
    }

    return DefaultVisitor().visit(this)
}

fun <T : Visitable<T>> T.visit(
    visitor: ((node: T) -> T?)
): T {
    return this.visit { node, _ -> visitor.invoke(node) }
}


fun <T : Visitable<T>, U> T.mapFilter(func: Function<T, Pair<U, Boolean>>): List<U> {
    val accumulation: MutableList<U> = ArrayList()

    this.visit { node, _ ->

        val mapped = func.apply(node)
        accumulation.add(mapped.first)

        if (mapped.second) null else node
    }

    return accumulation
}

fun <T : Visitable<T>> T.find(
    visitor: ((node: T) -> Boolean)
): T? {
    var found: T? = null
    this.visit { node, _ ->
        if (found != null) node
        else if (visitor(node)) {
            found = node
            node
        } else null
    }
    return found
}

fun <T : Visitable<T>> T.any(
    visitor: ((node: T) -> Boolean)
): Boolean = find(visitor) != null

fun <T : Visitable<T>, U> T.map(func: Function<T, U>): List<U> {
    return this.mapFilter { node -> Pair(func.apply(node), true) }
}

fun <T : Visitable<T>, U : Any> T.mapNotNull(func: Function<T, U?>): List<U> {
    return this.map(func).filterNotNull()
}