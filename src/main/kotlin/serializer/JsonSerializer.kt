package fr.univ_lille.iut_info.serializer

import com.google.gson.*
import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.memory.*
import fr.univ_lille.iut_info.steps.*

class JsonSerializer : Serializer<JsonElement> {
    override fun serialize(data: MemoryElement, context: IEvaluatingContext?): JsonElement {

        val serializer = this

        return JsonObject().apply {
            if (data is MemoryReference) addProperty("reference", data.reference.toString())
            else add(
                "value",
                when (data) {
                    is MemoryString -> JsonPrimitive(data.value)
                    is MemoryNumber -> JsonPrimitive(data.value)
                    is MemoryBoolean -> JsonPrimitive(data.value)

                    is MemoryArray -> JsonArray().also { array ->
                        data.value.map { serializer.serialize(it, context) }.forEach(array::add)
                    }

                    is MemoryObject -> JsonObject().also { obj ->
                        data.value.mapValues { serializer.serialize(it.value, context) }
                            .forEach { (key, value) -> obj.add(key, value) }
                    }

                    is MemoryUndefined -> JsonNull.INSTANCE
                })
        }.apply {
            if (context == null) return@apply

            val annotations = context.getAnnotations(data)

            if (annotations.isNotEmpty()) {
                add(
                    "_annotations", JsonArray().also {
                        annotations.map { data ->
                            serializer.serialize(
                                data, context
                            )
                        }.forEach(it::add)
                    })
            }

            addProperty("_path", context.pathMemory.getReversed(data)!!.toString())
            addProperty("_type", data.type.toString())
        }
    }

    override fun deserialize(root: JsonElement, context: ITypingContext, typecheck: Type?): MemoryElement {
        val eContext = context as? IEvaluatingContext

        if(root.isJsonNull) return MemoryUndefined()
        if(!root.isJsonObject && !root.isJsonArray) throw IllegalStateException("Cannot deserialize non object json element : $root")

        val deserializer = this

        return root.asJsonObject.run {
            val type = parseType(context, get("_type").asString)
            val assignableFrom = type.findAssignableFrom(context)

            return@run if(has("reference")) {
                MemoryReference(type!!, MemoryPath.parse(eContext!!, get("reference").asString)!!)
            } else {
                assert(has("value"))

                var cachedException: Exception? = null
                val root = get("value")

                assignableFrom.map { type ->
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
                                type, root.asJsonArray.asList().map { deserializer.deserialize(it, context, type.type) })

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
                                        else deserializer.deserialize(field, context, type)

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
        }
    }

    companion object {
        val instance = JsonSerializer()

        fun serialize(data: MemoryElement, context: IEvaluatingContext? = null) = instance.serialize(data, context)

        fun deserialize(root: JsonElement, context: ITypingContext, typecheck: Type) =
            instance.deserialize(root, context, typecheck)
    }
}