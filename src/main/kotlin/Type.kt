package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.steps.typeEquality
import kotlin.assert as assertThrow

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
        val any: AnyType
            get() = AnyType.instance

        val string: PrimitiveType.StringType
            get() = PrimitiveType.StringType.instance

        val number: PrimitiveType.NumberType
            get() = PrimitiveType.NumberType.instance

        val boolean: PrimitiveType.BooleanType
            get() = PrimitiveType.BooleanType.instance

        val undefined: PrimitiveType.UndefinedType
            get() = PrimitiveType.UndefinedType.instance

        fun nullable(type: Type): NullableType {
            if(type is NullableType) return type
            return NullableType(type)
        }

        fun array(type: Type): ArrayType {
            return ArrayType(type)
        }

        fun unordered(type: Type): UnorderedArrayType {
            return UnorderedArrayType(type)
        }

        fun objectT(
            identifier: String,
            children: Map<String, Type>,
            parent: Pair<String, List<Pair<String, Type>>>? = null
        ): PropertyType {
            return PropertyType(identifier, children.map { Pair(it.key, it.value) }, parent)
        }

        fun reference(
            identifier: String,
            cache: Type? = null
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

class NullableType(val type: Type): Type() {
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

    class UndefinedType private constructor(): PrimitiveType() {
        override fun toString(): String {
            return "undefined"
        }

        companion object {
            val instance = UndefinedType()
        }
    }
}

data class PropertyType(
    val identifier: String, val children: List<Pair<String, Type>>, val parent: Pair<String, List<Pair<String, Type>>>? = null
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

data class UnorderedArrayType(val type: Type): Type() {
    init {
        assertThrow(type !is UnorderedArrayType, {})
    }

    override fun toString(): String {
        return "$type[||]"
    }

    override fun accept(visitor: Visitor<Type>): Type {
        return UnorderedArrayType(visitor.visit(type))
    }
}