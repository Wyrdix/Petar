package fr.univ_lille.iut_info.parsing

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.univ_lille.iut_info.type.*

interface MemoryElement {
    fun type(): Type
}

data class MemoryString(val rawValue: Any?) : MemoryElement {
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

    companion object {
        fun asString(rawValue: Any?): String? {
            if (rawValue is JsonPrimitive && rawValue.isString) return rawValue.asString
            if (rawValue is String) return rawValue
            return null
        }
    }
}

data class MemoryNumber(val rawValue: Any?) : MemoryElement {
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

    companion object {
        fun asNumber(rawValue: Any?): Float? {
            if (rawValue is JsonPrimitive && rawValue.isNumber) return rawValue.asFloat
            if (rawValue is Float) return rawValue
            return null
        }
    }
}

data class MemoryObject(val type: ObjectType, val rawValue: Any?) : MemoryElement {
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
        return "MemoryObject(type=$type, value=$value)"
    }

    companion object {
        fun asObject(type: ObjectType, rawValue: Any?): Map<String, MemoryElement>? {
            val providedFieldsRaw = rawValue as? Map<*, *> ?: if (rawValue is JsonObject) rawValue.asMap()
            else null
            if (providedFieldsRaw == null) return null

            return providedFieldsRaw.keys.map { key ->
                Pair(
                    key,
                    createMemoryElement(type.childrenMap[key]!!, providedFieldsRaw[key])
                )
            }.associateBy({ it.first as String }, { it.second })
        }
    }
}

data class MemoryArray(val type: ArrayType, val rawValue: Any?) : MemoryElement {
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

    companion object {
        fun asArray(type: ArrayType, value: Any?): List<MemoryElement>? {
            if (value == null) return null
            if (value !is Array<*> && value !is List<*> && value !is JsonArray) return null

            val list =
                if (value is Array<*>) value.toList() else value as? List<*> ?: (value as JsonArray).asList()

            return list.map { createMemoryElement(type.type, it) }
        }
    }
}

fun createMemoryElement(type: Type, value: Any?): MemoryElement {
    if (type is StringType) return MemoryString(value)
    if (type is NumberType) return MemoryNumber(value)
    if (type is ObjectType) return MemoryObject(type, value)
    if (type is ArrayType) return MemoryArray(type, value)
    if (type is ReferenceType && type.cache != null) return createMemoryElement(type.cache!!, value)
    throw IllegalStateException("This type is not supported to create memory element.")
}