package fr.univ_lille.iut_info.memory

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.univ_lille.iut_info.*

abstract class MemoryElement : Visitable<MemoryElement> {
    abstract val rawValue: Any?
    abstract fun type(): Type
    abstract fun toJson(): JsonElement

    open fun viewAs(type: Type): List<MemoryElement> {
        if (type == type()) return listOf(this)
        return emptyList()
    }

    companion object {
        fun memory(value: String): MemoryString {
            return MemoryString(value)
        }

        fun memory(value: Number): MemoryNumber {
            return MemoryNumber(value)
        }

        fun memory(value: Boolean): MemoryBoolean {
            return MemoryBoolean(value)
        }

        fun memory(type: Type, vararg values: Any): MemoryArray {
            return MemoryArray(ArrayType(Type.any), values)
        }

        fun memory(type: PropertyType, values: Map<String, Any>): MemoryObject {
            return MemoryObject(type, values.map { Pair(it.key, it.value) })
        }
    }
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
        return Type.boolean
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

data class MemoryUndefined(override val rawValue: Any?) : MemoryElement() {
    init {
        type().assert(rawValue)
    }

    override fun type(): Type {
        return Type.undefined
    }

    override fun toString(): String {
        return "MemoryUndefined()"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryUndefined(rawValue)
    }

    override fun toJson(): JsonElement {
        return JsonNull.INSTANCE
    }

    companion object {
        fun asUndefined(rawValue: Any?): Nothing? {
            return null
        }
    }
}

data class MemoryObject(val type: PropertyType, override val rawValue: Any?) : MemoryElement() {
    @Suppress("UNCHECKED_CAST")
    val value: Map<String, MemoryElement>
        get() = asObject(type, rawValue)!!

    init {
        type().assert(rawValue)
    }

    override fun viewAs(type: Type): List<MemoryObject> {
        if (type !is PropertyType) return emptyList()

        val id = type.identifier

        if (!this@MemoryObject.type.nameChecked) throw IllegalStateException("Cannot use 'evaluateAs' before name checking.")

        val list: MutableList<MemoryObject> = ArrayList()

        if (id == this@MemoryObject.type.identifier) list.add(this)

        return list
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
        fun asObject(type: PropertyType, rawValue: Any?): Map<String, MemoryElement>? {
            val providedFieldsRaw = asMap(rawValue) ?: return null

            val returnValue = providedFieldsRaw.mapValues { (key, value) ->
                createMemoryElement(type.childrenMap[key]!!, value)
            }.mapKeys {
                it.key as String
            }
            return returnValue
        }

        fun asMap(rawValue: Any?): Map<String?, *>? {
            when (rawValue) {
                is Map<*, *> ->
                    @Suppress("UNCHECKED_CAST")
                    return if (rawValue.isEmpty() || rawValue.keys.first() is String) rawValue as Map<String?, *>? else null

                is JsonObject -> return rawValue.asMap() as Map<String?, *>?
                is List<*> -> {
                    val withoutNulls = rawValue.filterNotNull()
                    return if (withoutNulls.isEmpty()) emptyMap<String?, Any>() as Map<String?, *>?
                    else {
                        val el = withoutNulls.first()

                        if (el is Pair<*, *> && el.first is String) rawValue.associateBy(
                            { (it as Pair<*, *>).first as String? },
                            { (it as Pair<*, *>).second }) as Map<String?, *>?
                        else null as Map<String?, *>?
                    }
                }

                else -> return null
            }
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

data class MemoryUnorderedArray(val type: UnorderedArrayType, override val rawValue: Any?) : MemoryElement() {
    @Suppress("UNCHECKED_CAST")
    val value: List<MemoryElement>
        get() = asUnorderedArray(type, rawValue)!!

    init {
        type().assert(rawValue)
    }

    override fun type(): Type {
        return type
    }

    override fun toString(): String {
        return "MemoryUnorderedArray(type=$type, value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryUnorderedArray(type, value.map { visitor.visit(it) })
    }

    override fun toJson(): JsonElement {
        val array = JsonArray()
        value.forEach { array.add(it.toJson()) }
        return array
    }

    companion object {
        fun asUnorderedArray(type: UnorderedArrayType, value: Any?): List<MemoryElement>? {
            if (value == null) return null
            if (value !is Array<*> && value !is List<*> && value !is JsonArray) return null

            val list = if (value is Array<*>) value.toList() else value as? List<*> ?: (value as JsonArray).asList()

            return list.map { createMemoryElement(type.type, it) }
        }
    }
}


fun createMemoryElement(type: Type, value: Any?): MemoryElement {
    if(type is PrimitiveType.UndefinedType) return MemoryUndefined(value)
    if (value is MemoryElement) return createMemoryElement(type, value.rawValue)
    if (type is PrimitiveType.StringType) return MemoryString(value)
    if (type is PrimitiveType.NumberType) return MemoryNumber(value)
    if (type is PrimitiveType.BooleanType) return MemoryBoolean(value)
    if (type is PropertyType) return MemoryObject(type, value)
    if (type is ArrayType) return MemoryArray(type, value)
    if (type is UnorderedArrayType) return MemoryUnorderedArray(type, value)
    if (type is ReferenceType && type.cache != null) return createMemoryElement(type.cache!!, value)
    throw IllegalStateException("This type is not supported to create memory element.")
}