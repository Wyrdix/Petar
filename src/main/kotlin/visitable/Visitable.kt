package fr.univ_lille.iut_info.visitable

import java.util.function.BiFunction
import java.util.function.Function

interface Visitable<T : Visitable<T>> {
    fun accept(visitor: Visitor<T>): T
}

interface Visitor<T> {
    fun visit(obj: T): T

    fun <V> map(obj: T, function: BiFunction<T, Function<T, V>, V?>, default: V): V
}