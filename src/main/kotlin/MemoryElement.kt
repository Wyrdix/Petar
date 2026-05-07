package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.steps.ITypingContext
import fr.univ_lille.iut_info.steps.isAssignableFrom
import java.util.*

sealed class MemoryElement : Visitable<MemoryElement> {
    val id = UUID.randomUUID().toString()
    abstract val type: Type

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryElement

        return id == other.id
    }

    abstract fun isSimilarTo(other: MemoryElement, context: ITypingContext? = null): Boolean

    companion object {
        fun string(value: String): MemoryString {
            return MemoryString(value)
        }

        fun number(value: Number): MemoryNumber {
            return MemoryNumber(value)
        }

        fun boolean(value: Boolean): MemoryBoolean {
            return MemoryBoolean(value)
        }

        fun undefined(): MemoryUndefined {
            return MemoryUndefined()
        }

        fun property(type: PropertyType, value: Map<String, MemoryElement>): MemoryObject {
            return MemoryObject(type, value)
        }

        fun array(type: ArrayType, value: List<MemoryElement>): MemoryArray {
            return MemoryArray(type, value)
        }
    }
}

class MemoryUndefined : MemoryElement() {
    override val type = Type.undefined

    override fun toString(): String {
        return "MemoryUndefined()"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return this
    }

    override fun isSimilarTo(other: MemoryElement, context: ITypingContext?): Boolean {
        return other is MemoryUndefined
    }
}

data class MemoryString(val value: String) : MemoryElement() {
    override val type = Type.string

    override fun toString(): String {
        return "MemoryString(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return this
    }

    override fun isSimilarTo(other: MemoryElement, context: ITypingContext?): Boolean {
        return other is MemoryString && other.value == value
    }
}

data class MemoryNumber(val value: Number) : MemoryElement() {
    override val type = Type.number

    override fun toString(): String {
        return "MemoryNumber(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return this
    }

    override fun isSimilarTo(other: MemoryElement, context: ITypingContext?): Boolean {
        return other is MemoryNumber && other.value.toDouble() == value.toDouble()
    }
}

data class MemoryBoolean(val value: Boolean) : MemoryElement() {
    override val type = Type.boolean

    override fun toString(): String {
        return "MemoryBoolean(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return this
    }

    override fun isSimilarTo(other: MemoryElement, context: ITypingContext?): Boolean {
        return other is MemoryBoolean && other.value == value
    }
}

data class MemoryObject(override val type: PropertyType, val value: Map<String, MemoryElement>) : MemoryElement() {

    override fun toString(): String {
        return "MemoryObject(type=$type, value=${value.toSortedMap()})"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryObject(type, value.mapValues { (_, value) -> visitor.visit(value) })
    }

    override fun isSimilarTo(other: MemoryElement, context: ITypingContext?): Boolean {
        if (other !is MemoryObject) return false

        val typecheck = context?.let {
            type.isAssignableFrom(
                it, other.type
            )
        } ?: (type == other.type)

        return typecheck && (other.value.keys.union(value.keys)).map {
            Pair(
                other.value[it], value[it]
            )
        }.all { (it1, it2) -> it2 != null && it1?.isSimilarTo(it2, context) ?: false }
    }
}

data class MemoryArray(override val type: ArrayType, val value: List<MemoryElement>) : MemoryElement() {

    override fun toString(): String {
        return "MemoryArray(type=$type, value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryArray(type, value.map { visitor.visit(it) })
    }

    override fun isSimilarTo(other: MemoryElement, context: ITypingContext?): Boolean {
        val bool = other is MemoryArray
        val bool1 = type == other.type
        return bool && bool1 && other.value.zip(value).all { (it1, it2) -> it1.isSimilarTo(it2, context) }
    }
}