package serializer

import assertIsSimilarTo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.serializer.JsonSerializer
import fr.univ_lille.iut_info.steps.NameStep
import fr.univ_lille.iut_info.steps.TypecheckStep
import fr.univ_lille.iut_info.steps.check
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class JsonSerializerTest {

    val emptyContext = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))

    fun parse(value: String): JsonElement {
        return Gson().fromJson("{\"value\":${value}}", JsonObject::class.java).get("value")
    }

    fun serializeEmpty(value: MemoryElement): String {
        return Gson().toJson(JsonSerializer.serialize(value))
    }

    fun deserializeEmpty(value: String, type: Type): MemoryElement {
        return JsonSerializer.deserialize(
            parse(value), emptyContext, type
        )
    }

    @Test
    fun deserializePrimitive() {
        assert(
            MemoryElement.string("Value").isSimilarTo(deserializeEmpty("\"Value\"", Type.string), emptyContext)
        )
        assert(
            MemoryElement.number(10).isSimilarTo(deserializeEmpty("10", Type.number), emptyContext)
        )
        assert(
            MemoryElement.boolean(true).isSimilarTo(deserializeEmpty("true", Type.boolean), emptyContext)
        )
        assert(
            !MemoryElement.boolean(true).isSimilarTo(deserializeEmpty("\"Value\"", Type.string), emptyContext)
        )
        assert(
            !MemoryElement.string("Value").isSimilarTo(deserializeEmpty("10", Type.number), emptyContext)
        )
        assert(
            !MemoryElement.number(10).isSimilarTo(deserializeEmpty("true", Type.boolean), emptyContext)
        )
    }

    @Test
    fun deserializeArray() {

        val type1 = PropertyType(
            "_", listOf(Pair("value", Type.number))
        )

        val type2 = PropertyType(
            "_", listOf(Pair("value1", Type.number), Pair("value2", Type.number))
        )

        assert(
            MemoryElement.array(
                Type.array(type1), listOf(MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10)))))
            ).isSimilarTo(deserializeEmpty("[{\"value\": 10}]", Type.array(type1)), emptyContext)
        )

        assert(
            MemoryElement.array(
                Type.array(type2), listOf(
                    MemoryElement.property(
                        type2, mapOf(Pair("value1", MemoryElement.number(10)), Pair("value2", MemoryElement.number(12)))
                    )
                )
            ).isSimilarTo(deserializeEmpty("[{\"value1\": 10, \"value2\": 12}]", Type.array(type2)), emptyContext)
        )

        assertDoesNotThrow { deserializeEmpty("[]", Type.array(type1)) }
        assertDoesNotThrow { deserializeEmpty("[]", Type.array(type2)) }
    }

    @Test
    fun deserializeProperty() {

        val type1 = PropertyType(
            "_", listOf(Pair("value", Type.number))
        )

        val type2 = PropertyType(
            "_", listOf(Pair("value1", Type.number), Pair("value2", Type.number))
        )

        assert(
            MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10)))).isSimilarTo(
                deserializeEmpty("{\"value\": 10}", type1), emptyContext
            )
        )

        assert(
            MemoryElement.property(
                type2, mapOf(Pair("value1", MemoryElement.number(11)), Pair("value2", MemoryElement.number(12)))
            ).isSimilarTo(deserializeEmpty("{\"value1\": 11, \"value2\":12}", type2), emptyContext)
        )

        assertThrows<IllegalStateException> {
            deserializeEmpty("{\"value\": 10}", type2)
        }

        assertThrows<IllegalStateException> {
            deserializeEmpty("{\"value1\": 11, \"value2\":12}", type1)
        }
    }

    @Test
    fun serializePrimitive() {
        assertEquals(
            serializeEmpty(MemoryElement.string("Value")), serializeEmpty(
                deserializeEmpty("\"Value\"", Type.string)
            )
        )
        assertEquals(
            serializeEmpty(MemoryElement.number(10)), serializeEmpty(
                deserializeEmpty("10", Type.number)
            )
        )
        assertEquals(
            serializeEmpty(MemoryElement.boolean(true)), serializeEmpty(
                deserializeEmpty("true", Type.boolean)
            )
        )
        assertNotEquals(
            serializeEmpty(MemoryElement.boolean(true)), serializeEmpty(
                deserializeEmpty("\"Value\"", Type.string)
            )
        )
        assertNotEquals(
            serializeEmpty(MemoryElement.string("Value")), serializeEmpty(
                deserializeEmpty("10", Type.number)
            )
        )
        assertNotEquals(
            serializeEmpty(MemoryElement.number(10)), serializeEmpty(
                deserializeEmpty("true", Type.boolean)
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

        val type1 = PropertyType(
            "_", listOf(Pair("value", Type.number))
        )

        val type2 = PropertyType(
            "_", listOf(Pair("value1", Type.number), Pair("value2", Type.number))
        )

        assertEquals(
            serializeEmpty(
                MemoryElement.array(
                    Type.array(type1),
                    listOf(MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10)))))
                )
            ), serializeEmpty(deserializeEmpty("[{\"value\": 10}]", Type.array(type1)))
        )

        assertEquals(
            serializeEmpty(
                MemoryElement.array(
                    Type.array(type2), listOf(
                        MemoryElement.property(
                            type2,
                            mapOf(Pair("value1", MemoryElement.number(10)), Pair("value2", MemoryElement.number(12)))
                        )
                    )
                )
            ), serializeEmpty(deserializeEmpty("[{\"value1\": 10, \"value2\": 12}]", Type.array(type2)))
        )
    }

    @Test
    fun serializeProperty() {

        val type1 = PropertyType(
            "_", listOf(Pair("value", Type.number))
        )

        val type2 = PropertyType(
            "_", listOf(Pair("value1", Type.number), Pair("value2", Type.number))
        )

        assertEquals(
            serializeEmpty(MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10))))),
            serializeEmpty(deserializeEmpty("{\"value\": 10}", type1))
        )

        assertEquals(
            serializeEmpty(
                MemoryElement.property(
                    type2, mapOf(Pair("value1", MemoryElement.number(11)), Pair("value2", MemoryElement.number(12)))
                )
            ), serializeEmpty(deserializeEmpty("{\"value1\": 11, \"value2\":12}", type2))
        )
    }
}