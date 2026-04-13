package fr.univ_lille.iut_info

import java.util.*
import kotlin.assert as assertThrow

abstract class Type : Visitable<Type> {
    val id = UUID.randomUUID().toString()

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Type

        return id == other.id
    }

    companion object {
        val any: AnyType
            get() = AnyType.instance

        val bottom: BottomType
            get() = BottomType.instance

        val string: PrimitiveType.StringType
            get() = PrimitiveType.StringType.instance

        val number: PrimitiveType.NumberType
            get() = PrimitiveType.NumberType.instance

        val boolean: PrimitiveType.BooleanType
            get() = PrimitiveType.BooleanType.instance

        val undefined: PrimitiveType.UndefinedType
            get() = PrimitiveType.UndefinedType.instance

        fun nullable(type: Type): NullableType {
            if (type is NullableType) return type
            return NullableType(type)
        }

        fun array(type: Type = any): ArrayType {
            return ArrayType(type)
        }

        fun objectT(
            identifier: String, children: Map<String, Type>, parent: Pair<String, List<Pair<String, Type>>>? = null
        ): PropertyType {
            return PropertyType(identifier, children.map { Pair(it.key, it.value) }, parent)
        }

        fun reference(
            identifier: String, cache: Type? = null
        ): ReferenceType {
            val reference = ReferenceType(identifier)
            reference.cache = cache
            return reference
        }
    }

}

class AnyType : Type() {
    override fun accept(visitor: Visitor<Type>): Type {
        return this
    }

    companion object {
        val instance = AnyType()
    }
}

class BottomType : Type() {
    override fun accept(visitor: Visitor<Type>): Type {
        return this
    }

    companion object {
        val instance = BottomType()
    }
}


class NullableType(val type: Type) : Type() {
    override fun toString(): String {
        return "Nullable<${type}>"
    }

    override fun accept(visitor: Visitor<Type>): Type {
        return NullableType(visitor.visit(type))
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

    class UndefinedType private constructor() : PrimitiveType() {
        override fun toString(): String {
            return "undefined"
        }

        companion object {
            val instance = UndefinedType()
        }
    }
}

data class PropertyType(
    val identifier: String,
    val children: List<Pair<String, Type>>,
    val parent: Pair<String, List<Pair<String, Type>>>? = null
) : Type() {

    var nameChecked: Boolean = false
    var indirectViews: Set<String> = emptySet()
    val allViews: Set<String>
        get() = setOf(identifier) + indirectViews

    val childrenMap: Map<String, Type>
        get() = children.associateBy({ it.first }, { it.second })

    override fun accept(visitor: Visitor<Type>): Type {
        return PropertyType(identifier, children.map { Pair(it.first, visitor.visit(it.second)) }, parent)
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
        assertThrow(type !is ArrayType, {})
    }

    override fun toString(): String {
        return "$type[]"
    }

    override fun accept(visitor: Visitor<Type>): Type {
        return ArrayType(visitor.visit(type))
    }
}