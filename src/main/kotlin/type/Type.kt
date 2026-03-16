package fr.univ_lille.iut_info.type

import fr.univ_lille.iut_info.expression.ObjectExpression
import fr.univ_lille.iut_info.visitable.Visitable
import fr.univ_lille.iut_info.visitable.Visitor

abstract class Type : Visitable<Type> {

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Type) return false
        return typeEquality(this, other) && typeEquality(other, this)
    }

    fun resolve(): Type {
        if (this is ReferenceType) {
            val cache = this.cache
            if (cache != null) return cache
            return this
        }
        return this
    }

    companion object {
        val string: StringType
            get() = StringType.instance

        val number: NumberType
            get() = NumberType.instance

        val boolean: BooleanType
            get() = BooleanType.instance
    }
}

class StringType private constructor() : Type() {
    override fun toString(): String {
        return "String"
    }

    companion object {
        val instance = StringType()
    }

    override fun accept(visitor: Visitor<Type>): Type {
        return this
    }
}

class NumberType : Type() {
    override fun toString(): String {
        return "Number"
    }

    companion object {
        val instance = NumberType()
    }

    override fun accept(visitor: Visitor<Type>): Type {
        return this
    }
}

class BooleanType : Type() {
    override fun toString(): String {
        return "Boolean"
    }

    companion object {
        val instance = BooleanType()
    }

    override fun accept(visitor: Visitor<Type>): Type {
        return this
    }
}

data class ObjectType(
    val identifier: String, val children: List<Pair<String, Type>>, val parents: List<ObjectExpression>
) : Type() {
    val childrenMap: Map<String, Type>
        get() = children.associateBy({ it.first }, { it.second })


    override fun accept(visitor: Visitor<Type>): Type {
        return ObjectType(identifier, children.map { Pair(it.first, visitor.visit(it.second)) }, parents)
    }
}

data class ReferenceType(val value: String) : Type() {
    var cache: Type? = null

    override fun toString(): String {
        return cache?.toString() ?: "<empty reference to $value>"
    }

    override fun accept(visitor: Visitor<Type>): Type {
        return ReferenceType(value)
    }
}

data class ArrayType(val type: Type) : Type() {
    init {
        assert(type !is ArrayType)
    }

    override fun toString(): String {
        return "$type[]"
    }

    override fun accept(visitor: Visitor<Type>): Type {
        return ArrayType(visitor.visit(type))
    }
}