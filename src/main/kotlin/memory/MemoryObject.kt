package fr.univ_lille.iut_info.memory

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.type.assert

abstract class MemoryElement : Visitable<MemoryElement> {
    abstract val rawValue: Any?
    abstract fun type(): Type
    abstract fun toJson(): JsonElement
}

data class MemoryString(override val rawValue: Any?) : MemoryElement() {
    @Suppress("UNCHECKED_CAST")
    val value: String
        get() = asString(rawValue)!!

    init {
        type().assert(rawValue)
    }

    override fun type(): Type {
        return Type.string
    }

    override fun toString(): String {
        return "MemoryString(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryString(rawValue)
    }

    override fun toJson(): JsonElement {
        return JsonPrimitive(value)
    }

    companion object {
        fun asString(rawValue: Any?): String? {
            if (rawValue is JsonPrimitive && rawValue.isString) return rawValue.asString
            if (rawValue is String) return rawValue
            return null
        }
    }
}

data class MemoryNumber(override val rawValue: Any?) : MemoryElement() {
    @Suppress("UNCHECKED_CAST")
    val value: Float
        get() = asNumber(rawValue)!!

    init {
        type().assert(rawValue)
    }

    override fun type(): Type {
        return Type.number
    }

    override fun toString(): String {
        return "MemoryNumber(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryNumber(rawValue)
    }

    override fun toJson(): JsonElement {
        return JsonPrimitive(value)
    }

    companion object {
        fun asNumber(rawValue: Any?): Float? {
            if (rawValue is JsonPrimitive && rawValue.isNumber) return rawValue.asFloat
            if (rawValue is Float) return rawValue
            return null
        }
    }
}

data class MemoryBoolean(override val rawValue: Any?) : MemoryElement() {
    @Suppress("UNCHECKED_CAST")
    val value: Boolean
        get() = asBoolean(rawValue)!!

    init {
        type().assert(rawValue)
    }

    override fun type(): Type {
        return Type.number
    }

    override fun toString(): String {
        return "MemoryBoolean(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryBoolean(rawValue)
    }

    override fun toJson(): JsonElement {
        return JsonPrimitive(value)
    }

    companion object {
        fun asBoolean(rawValue: Any?): Boolean? {
            if (rawValue is JsonPrimitive && rawValue.isBoolean) return rawValue.asBoolean
            if (rawValue is Boolean) return rawValue
            return null
        }
    }
}

data class MemoryObject(val type: ObjectType, override val rawValue: Any?) : MemoryElement() {
    @Suppress("UNCHECKED_CAST")
    val value: Map<String, MemoryElement>
        get() = asObject(type, rawValue)!!

    init {
        type().assert(rawValue)
    }

    override fun type(): Type {
        return type
    }

    override fun toString(): String {
        return "MemoryObject(type=$type, value=${value.toSortedMap()})"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryObject(type, value.mapValues { (_, value) -> visitor.visit(value) })
    }

    override fun toJson(): JsonElement {
        val obj = JsonObject()
        value.forEach { (key, value) -> obj.add(key, value.toJson()) }
        return obj
    }

    companion object {
        fun asObject(type: ObjectType, rawValue: Any?): Map<String, MemoryElement>? {
            val providedFieldsRaw = rawValue as? Map<*, *> ?: if (rawValue is JsonObject) rawValue.asMap()
            else null
            if (providedFieldsRaw == null) return null

            val returnValue = providedFieldsRaw.mapValues { (key, value) ->
                createMemoryElement(type.childrenMap[key]!!, value)
            }.mapKeys {
                it.key as String
            }
            return returnValue
        }
    }
}

data class MemoryArray(val type: ArrayType, override val rawValue: Any?) : MemoryElement() {
    @Suppress("UNCHECKED_CAST")
    val value: List<MemoryElement>
        get() = asArray(type, rawValue)!!

    init {
        type().assert(rawValue)
    }

    override fun type(): Type {
        return type
    }

    override fun toString(): String {
        return "MemoryArray(type=$type, value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryArray(type, value.map { visitor.visit(it) })
    }

    override fun toJson(): JsonElement {
        val array = JsonArray()
        value.forEach { array.add(it.toJson()) }
        return array
    }

    companion object {
        fun asArray(type: ArrayType, value: Any?): List<MemoryElement>? {
            if (value == null) return null
            if (value !is Array<*> && value !is List<*> && value !is JsonArray) return null

            val list = if (value is Array<*>) value.toList() else value as? List<*> ?: (value as JsonArray).asList()

            return list.map { createMemoryElement(type.type, it) }
        }
    }
}

fun createMemoryElement(type: Type, value: Any?): MemoryElement {
    if (value is MemoryElement) return createMemoryElement(type, value.rawValue)
    if (type is PrimitiveType.StringType) return MemoryString(value)
    if (type is PrimitiveType.NumberType) return MemoryNumber(value)
    if (type is PrimitiveType.BooleanType) return MemoryBoolean(value)
    if (type is ObjectType) return MemoryObject(type, value)
    if (type is ArrayType) return MemoryArray(type, value)
    if (type is ReferenceType && type.cache != null) return createMemoryElement(type.cache!!, value)
    throw IllegalStateException("This type is not supported to create memory element.")
}