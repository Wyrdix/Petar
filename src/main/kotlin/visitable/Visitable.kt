package fr.univ_lille.iut_info.visitable

interface Visitable<T : Visitable<T>> {
    fun accept(visitor: Visitor<T>): T
}

interface Visitor<T> {
    fun visit(obj: T): T
}

fun <T: Visitable<T>> T.visit(
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
