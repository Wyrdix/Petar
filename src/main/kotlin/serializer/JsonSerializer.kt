package fr.univ_lille.iut_info.serializer

import com.google.gson.*
import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.steps.*

class JsonSerializer : Serializer<JsonElement> {
    override fun serialize(data: MemoryElement, context: IEvaluatingContext?): JsonElement {
        return when (data) {
            is MemoryString -> JsonPrimitive(data.value)
            is MemoryNumber -> JsonPrimitive(data.value)
            is MemoryBoolean -> JsonPrimitive(data.value)
            is MemoryArray -> JsonArray().also { array ->
                data.value.map { this.serialize(it, context) }.forEach(array::add)
            }

            is MemoryObject -> JsonObject().also { obj ->
                data.value.mapValues { this.serialize(it.value, context) }
                    .forEach { (key, value) -> obj.add(key, value) }
                obj.add("_type", JsonPrimitive(data.type.identifier))
            }.also { obj ->
                val annotations = context?.getAnnotations(data)
                if (context != null && !annotations.isNullOrEmpty()) {
                    obj.add(
                        "_annotations",
                        JsonArray().also {
                            annotations.map { data ->
                                this.serialize(
                                    data,
                                    context
                                )
                            }.forEach(it::add)
                        })
                }
            }

            is MemoryUndefined -> JsonNull.INSTANCE
            else -> throw IllegalStateException("Could not deserialize data.")
        }
    }

    override fun deserialize(root: JsonElement, context: ITypingContext, typecheck: Type?): MemoryElement {
        val findAssignableFrom = typecheck?.resolveReference(context).findAssignableFrom(context)
        return findAssignableFrom.map { type ->
            try {
                return@map when (root) {
                    is JsonPrimitive if root.isString && (type == null || type is PrimitiveType.StringType) -> MemoryElement.string(
                        root.asString
                    )

                    is JsonPrimitive if root.isNumber && (type == null || type is PrimitiveType.NumberType) -> MemoryElement.number(
                        root.asNumber
                    )

                    is JsonPrimitive if root.isBoolean && (type == null || type is PrimitiveType.BooleanType) -> MemoryElement.boolean(
                        root.asBoolean
                    )

                    is JsonArray if root.isJsonArray && type is ArrayType -> MemoryElement.array(
                        type, root.asJsonArray.asList().map { this.deserialize(it, context, type.type) })

                    is JsonObject if root.isJsonObject && type is PropertyType -> MemoryElement.property(
                        type,
                        root.asJsonObject.asMap()
                            .mapValues { it ->
                                this.deserialize(
                                    it.value,
                                    context,
                                    type.getAllFields(context)[it.key] ?: Type.bottom
                                )
                            })

                    is JsonNull if root.isJsonNull && type is PrimitiveType.UndefinedType -> MemoryElement.undefined()
                    else -> null
                }
            } catch (_: Exception) {
                return@map null
            }
        }.filterNotNull().sortedByDescending { it.type.ascendants(context).size }.firstOrNull { it.type != Type.bottom }
            ?: throw IllegalStateException("Could not deserialize data (type: ${typecheck}, value: ${root})")
    }

    companion object {
        val instance = JsonSerializer()

        fun serialize(data: MemoryElement, context: IEvaluatingContext? = null) = instance.serialize(data, context)

        fun deserialize(root: JsonElement, context: ITypingContext, typecheck: Type) =
            instance.deserialize(root, context, typecheck)
    }
}