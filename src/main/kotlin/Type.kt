package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.steps.typeEquality
import fr.univ_lille.iut_info.type.assert

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
        val string: PrimitiveType.StringType
            get() = PrimitiveType.StringType.instance

        val number: PrimitiveType.NumberType
            get() = PrimitiveType.NumberType.instance

        val boolean: PrimitiveType.BooleanType
            get() = PrimitiveType.BooleanType.instance
    }
}

abstract class PrimitiveType : Type() {
    override fun accept(visitor: Visitor<Type>): Type {
        return this
    }

    class StringType private constructor() : PrimitiveType() {
        override fun toString(): String {
            return "String"
        }

        companion object {
            val instance = StringType()
        }
    }

    class NumberType private constructor() : PrimitiveType() {
        override fun toString(): String {
            return "Number"
        }

        companion object {
            val instance = NumberType()
        }
    }

    class BooleanType private constructor() : PrimitiveType() {
        override fun toString(): String {
            return "Boolean"
        }

        companion object {
            val instance = BooleanType()
        }
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