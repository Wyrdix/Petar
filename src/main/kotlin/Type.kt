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

        fun array(type: Type): ArrayType {
            return ArrayType(type)
        }

        fun objectT(
            identifier: String,
            children: Map<String, Type>,
            views: List<ObjectExpression> = emptyList()
        ): ObjectType {
            return ObjectType(identifier, children.map { Pair(it.key, it.value) }, views)
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

class AnyType private constructor() : Type() {
    override fun accept(visitor: Visitor<Type>): Type {
        return this
    }

    companion object {
        val instance = AnyType()
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
    val identifier: String, val children: List<Pair<String, Type>>, val views: List<ObjectExpression>
) : Type() {

    var nameChecked: Boolean = false
    var directViews: Set<String> = views.map { it.identifier }.toSet()
    var indirectViews: Set<String> = emptySet()
    val allViews: Set<String>
        get() = setOf(identifier) + directViews + indirectViews

    val childrenMap: Map<String, Type>
        get() = children.associateBy({ it.first }, { it.second })

    override fun accept(visitor: Visitor<Type>): Type {
        return ObjectType(identifier, children.map { Pair(it.first, visitor.visit(it.second)) }, views)
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