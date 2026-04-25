package fr.univ_lille.iut_info.serializer

import com.google.gson.*
import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.steps.ITypingContext
import fr.univ_lille.iut_info.steps.resolveReference

class JsonSerializer : Serializer<JsonElement> {
    override fun serialize(data: MemoryElement): JsonElement {
        return when (data) {
            is MemoryString -> JsonPrimitive(data.value)
            is MemoryNumber -> JsonPrimitive(data.value)
            is MemoryBoolean -> JsonPrimitive(data.value)
            is MemoryArray -> JsonArray().also {
                data.value.map(this::serialize).forEach(it::add)
            }

            is MemoryObject -> JsonObject().also { obj ->
                data.value.mapValues { this.serialize(it.value) }.forEach { (key, value) -> obj.add(key, value) }
            }

            is MemoryUndefined -> JsonNull.INSTANCE
            else -> throw IllegalStateException("Could not deserialize data.")
        }
    }

    override fun deserialize(root: JsonElement, type: Type, context: ITypingContext): MemoryElement {
        val resolvedType = type.resolveReference(context)
        return when (root) {
            is JsonPrimitive if root.isString && resolvedType is PrimitiveType.StringType -> MemoryElement.string(root.asString)
            is JsonPrimitive if root.isNumber && resolvedType is PrimitiveType.NumberType -> MemoryElement.number(root.asNumber)
            is JsonPrimitive if root.isBoolean && resolvedType is PrimitiveType.BooleanType -> MemoryElement.boolean(
                root.asBoolean
            )

            is JsonArray if root.isJsonArray && resolvedType is ArrayType -> MemoryElement.array(
                resolvedType, root.asJsonArray.asList().map { this.deserialize(it, resolvedType.type, context) })

            is JsonObject if root.isJsonObject && resolvedType is PropertyType -> MemoryElement.property(
                resolvedType,
                root.asJsonObject.asMap()
                    .mapValues { this.deserialize(it.value, resolvedType.fields[it.key] ?: Type.bottom, context) })

            is JsonNull if root.isJsonNull && resolvedType is PrimitiveType.UndefinedType -> MemoryElement.undefined()
            else -> throw IllegalStateException("Could not deserialize data (type: ${type}, value: ${root})")
        }
    }

    companion object {
        val instance = JsonSerializer()

        fun serialize(data: MemoryElement) = instance.serialize(data)

        fun deserialize(root: JsonElement, type: Type, context: ITypingContext) =
            instance.deserialize(root, type, context)
    }
}