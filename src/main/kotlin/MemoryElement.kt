package fr.univ_lille.iut_info

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
}

data class MemoryString(val value: String) : MemoryElement() {
    override val type = Type.string

    override fun toString(): String {
        return "MemoryString(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryString

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryNumber

        return value.toDouble() == other.value.toDouble()
    }

    override fun hashCode(): Int {
        return value.hashCode()
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryBoolean

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

data class MemoryObject(override val type: PropertyType, val value: Map<String, MemoryElement>) : MemoryElement() {

    override fun toString(): String {
        return "MemoryObject(type=$type, value=${value.toSortedMap()})"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryObject(type, value.mapValues { (_, value) -> visitor.visit(value) })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryObject

        return value.keys == other.value.keys && value.entries.all { (key, value) -> other.value[key] == value }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

data class MemoryArray(override val type: ArrayType, val value: List<MemoryElement>) : MemoryElement() {

    override fun toString(): String {
        return "MemoryArray(type=$type, value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryArray(type, value.map { visitor.visit(it) })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryArray

        return value.size == other.value.size && value.zip(other.value).all { (e1, e2) -> e1 == e2 }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}