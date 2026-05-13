package serializer

import assertIsSimilarTo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.serializer.JsonSerializer
import fr.univ_lille.iut_info.steps.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class JsonSerializerTest {

    val emptyContext = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))

    fun parse(value: String): JsonElement {
        return Gson().fromJson("{\"value\":${value}}", JsonObject::class.java).get("value")
    }

    fun serialize(value: MemoryElement, context: IEvaluatingContext): String {
        return Gson().toJson(JsonSerializer.serialize(value, context))
    }

    fun deserialize(value: String, type: Type, context: ITypingContext): MemoryElement {
        return JsonSerializer.deserialize(
            parse(value), context, type
        )
    }

    @Test
    fun deserializePrimitive() {
        assert(
            MemoryElement.string("Value").isSimilarTo(deserialize("\"Value\"", Type.string, emptyContext), emptyContext)
        )
        assert(
            MemoryElement.number(10).isSimilarTo(deserialize("10", Type.number, emptyContext), emptyContext)
        )
        assert(
            MemoryElement.boolean(true).isSimilarTo(deserialize("true", Type.boolean, emptyContext), emptyContext)
        )
        assert(
            !MemoryElement.boolean(true).isSimilarTo(deserialize("\"Value\"", Type.string, emptyContext), emptyContext)
        )
        assert(
            !MemoryElement.string("Value").isSimilarTo(deserialize("10", Type.number, emptyContext), emptyContext)
        )
        assert(
            !MemoryElement.number(10).isSimilarTo(deserialize("true", Type.boolean, emptyContext), emptyContext)
        )
    }

    @Test
    fun deserializeArray() {

        val context = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))

        val type1 = PropertyType(
            "1", listOf(Pair("value", Type.number))
        )

        val type2 = PropertyType(
            "2", listOf(Pair("value1", Type.number), Pair("value2", Type.number))
        )

        context.typeNameMap[type1.identifier] = type1
        context.typeNameMap[type2.identifier] = type2

        type1.check(context)
        type2.check(context)

        assert(
            MemoryElement.array(
                Type.array(type1), listOf(MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10)))))
            ).isSimilarTo(deserialize("[{\"value\": 10}]", Type.array(type1), context), context)
        )

        assert(
            MemoryElement.array(
                Type.array(type2), listOf(
                    MemoryElement.property(
                        type2, mapOf(Pair("value1", MemoryElement.number(10)), Pair("value2", MemoryElement.number(12)))
                    )
                )
            ).isSimilarTo(deserialize("[{\"value1\": 10, \"value2\": 12}]", Type.array(type2), context), context)
        )

        assertDoesNotThrow { deserialize("[]", Type.array(type1), emptyContext) }
        assertDoesNotThrow { deserialize("[]", Type.array(type2), emptyContext) }
    }

    @Test
    fun deserializeProperty() {
        val context = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))

        val type1 = PropertyType(
            "1", listOf(Pair("value", Type.number))
        )

        val type2 = PropertyType(
            "2", listOf(Pair("value1", Type.number), Pair("value2", Type.number))
        )


        context.typeNameMap[type1.identifier] = type1
        context.typeNameMap[type2.identifier] = type2

        type1.check(context)
        type2.check(context)

        assert(
            MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10)))).isSimilarTo(
                deserialize("{\"value\": 10}", type1, context), context
            )
        )

        assert(
            MemoryElement.property(
                type2, mapOf(Pair("value1", MemoryElement.number(11)), Pair("value2", MemoryElement.number(12)))
            ).isSimilarTo(deserialize("{\"value1\": 11, \"value2\":12}", type2, context), context)
        )

        assertThrows<IllegalStateException> {
            deserialize("{\"value\": 10}", type2, context)
        }

        assertThrows<IllegalStateException> {
            deserialize("{\"value1\": 11, \"value2\":12}", type1, context)
        }
    }

    @Test
    fun serializePrimitive() {
        assertEquals(
            serialize(MemoryElement.string("Value"), emptyContext), serialize(
                deserialize("\"Value\"", Type.string, emptyContext), emptyContext
            )
        )
        assertEquals(
            serialize(MemoryElement.number(10), emptyContext), serialize(
                deserialize("10", Type.number, emptyContext), emptyContext
            )
        )
        assertEquals(
            serialize(MemoryElement.boolean(true), emptyContext), serialize(
                deserialize("true", Type.boolean, emptyContext), emptyContext
            )
        )
        assertNotEquals(
            serialize(MemoryElement.boolean(true), emptyContext), serialize(
                deserialize("\"Value\"", Type.string, emptyContext), emptyContext
            )
        )
        assertNotEquals(
            serialize(MemoryElement.string("Value"), emptyContext), serialize(
                deserialize("10", Type.number, emptyContext), emptyContext
            )
        )
        assertNotEquals(
            serialize(MemoryElement.number(10), emptyContext), serialize(
                deserialize("true", Type.boolean, emptyContext), emptyContext
            )
        )
    }

    @Test
    fun serializeThroughSpecialisation() {
        val type1 = Type.property("Root", mapOf(Pair("value", Type.number)))
        val type2 = Type.property("Root1", mapOf(Pair("value1", Type.optional(Type.string))), Pair("Root", emptyList()))
        val type3 = Type.property("Root2", emptyMap(), Pair("Root1", listOf(Pair("value1", Type.string))))

        val context = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))
        context.typeNameMap[type1.identifier] = type1
        context.typeNameMap[type2.identifier] = type2
        context.typeNameMap[type3.identifier] = type3

        type1.check(context)
        type2.check(context)
        type3.check(context)

        assertIsSimilarTo(
            MemoryObject(type2, mapOf("value" to MemoryNumber(10), "value1" to MemoryString("A"))),
            JsonSerializer.deserialize(
                parse("{value: 10, value1: \"A\"}"), context, type1
            ),
            context
        )

        assertIsSimilarTo(
            MemoryObject(type3, mapOf("value" to MemoryNumber(10), "value1" to MemoryString("A"))),
            JsonSerializer.deserialize(
                parse("{value: 10, value1: \"A\"}"), context, type1
            ),
            context
        )

        assertIsSimilarTo(
            MemoryObject(type2, mapOf("value" to MemoryNumber(10))),
            JsonSerializer.deserialize(
                parse("{value: 10}"), context, type1
            ),
            context
        )
    }

    @Test
    fun serializeArray() {

        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))

        val type1 = PropertyType(
            "1", listOf(Pair("value", Type.number))
        )

        val type2 = PropertyType(
            "2", listOf(Pair("value1", Type.number), Pair("value2", Type.number))
        )

        context.typeNameMap[type1.identifier] = type1
        context.typeNameMap[type2.identifier] = type2

        type1.check(context)
        type2.check(context)


        assertEquals(
            serialize(
                MemoryElement.array(
                    Type.array(type1),
                    listOf(MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10)))))
                ), context
            ), serialize(deserialize("[{\"value\": 10}]", Type.array(type1), context), context)
        )

        assertEquals(
            serialize(
                MemoryElement.array(
                    Type.array(type2), listOf(
                        MemoryElement.property(
                            type2,
                            mapOf(Pair("value1", MemoryElement.number(10)), Pair("value2", MemoryElement.number(12)))
                        )
                    )
                ), context
            ), serialize(deserialize("[{\"value1\": 10, \"value2\": 12}]", Type.array(type2), context), context)
        )
    }

    @Test
    fun serializeProperty() {

        val context = EvaluatingStep(TypecheckStep(NameStep(Program(ProgramData(emptyList())))))


        val type1 = PropertyType(
            "1", listOf(Pair("value", Type.number))
        )

        val type2 = PropertyType(
            "2", listOf(Pair("value1", Type.number), Pair("value2", Type.number))
        )
        context.typeNameMap[type1.identifier] = type1
        context.typeNameMap[type2.identifier] = type2

        type1.check(context)
        type2.check(context)

        assertEquals(
            serialize(MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10)))), context),
            serialize(deserialize("{\"value\": 10}", type1, context), context)
        )

        assertEquals(
            serialize(
                MemoryElement.property(
                    type2, mapOf(Pair("value1", MemoryElement.number(11)), Pair("value2", MemoryElement.number(12)))
                ), context
            ), serialize(deserialize("{\"value1\": 11, \"value2\":12}", type2, context), context)
        )
    }
}