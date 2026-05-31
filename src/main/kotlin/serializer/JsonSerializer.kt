package fr.univ_lille.iut_info.serializer

import com.google.gson.*
import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.memory.*
import fr.univ_lille.iut_info.steps.*

class JsonSerializer : Serializer<JsonElement> {
    override fun serialize(data: MemoryElement, context: IEvaluatingContext?): JsonElement {
        return when (data) {
            is MemoryString -> JsonPrimitive(data.value)
            is MemoryNumber -> JsonPrimitive(data.value)
            is MemoryBoolean -> JsonPrimitive(data.value)
            is MemoryReference -> {
                JsonObject().also { obj ->
                    obj.add("_type", JsonPrimitive(data.type.toString()))
                    obj.addProperty("_reference", data.reference.toString())
                }
            }
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
                        "_annotations", JsonArray().also {
                            annotations.map { data ->
                                this.serialize(
                                    data, context
                                )
                            }.forEach(it::add)
                        })
                }

                if(context != null) {
                    obj.addProperty("_path", context.pathMemory.getReversed(data)!!.toString())
                }
            }

            is MemoryUndefined -> JsonNull.INSTANCE
        }
    }

    override fun deserialize(root: JsonElement, context: ITypingContext, typecheck: Type?): MemoryElement {

        val findAssignableFrom = if (root.isJsonObject && root.asJsonObject.has("_type")) {
            val rawType = (root as JsonObject).get("_type").asString
            val type = ReferenceType(rawType).resolveReference(context)
            if (typecheck?.isAssignableFrom(context, type) ?: true) listOf(type)
            else emptyList()
        } else typecheck?.resolveReference(context).findAssignableFrom(context)

        var cachedException: Exception? = null

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

                    is JsonObject if root.isJsonObject && type is PropertyType -> {
                        val obj = root.asJsonObject
                        return@map MemoryElement.property(
                            type,
                            type.getAllFields(context).mapValues { (key, type) ->

                                val field = obj.get(key)
                                if ((field == null || field.isJsonNull) && !type.isAssignableFrom(
                                        context, Type.undefined
                                    )
                                ) throw IllegalStateException("Missing field $key.")

                                if (field == null || field.isJsonNull) MemoryUndefined()
                                else this.deserialize(field, context, type)

                            })
                    }

                    is JsonNull if root.isJsonNull && type is PrimitiveType.UndefinedType -> MemoryElement.undefined()
                    else -> null
                }
            } catch (e: Exception) {
                cachedException = e
                return@map null
            }
        }.filterNotNull().sortedByDescending {
            val size = it.type.ascendants(context).size
            size
        }.firstOrNull { it.type != Type.bottom }
            ?: throw cachedException
            ?: throw IllegalStateException("Could not deserialize data (type: ${typecheck}, value: ${root})")
    }

    companion object {
        val instance = JsonSerializer()

        fun serialize(data: MemoryElement, context: IEvaluatingContext? = null) = instance.serialize(data, context)

        fun deserialize(root: JsonElement, context: ITypingContext, typecheck: Type) =
            instance.deserialize(root, context, typecheck)
    }
}