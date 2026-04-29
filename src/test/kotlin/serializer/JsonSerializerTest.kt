package serializer

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.serializer.JsonSerializer
import fr.univ_lille.iut_info.steps.NameStep
import fr.univ_lille.iut_info.steps.TypecheckStep
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
            parse(value),  emptyContext, type
        )
    }

    @Test
    fun deserializePrimitive() {
        assertEquals(
            MemoryElement.string("Value"), deserializeEmpty("\"Value\"", Type.string)
        )
        assertEquals(
            MemoryElement.number(10), deserializeEmpty("10", Type.number)
        )
        assertEquals(
            MemoryElement.boolean(true), deserializeEmpty("true", Type.boolean)
        )
        assertNotEquals(
            MemoryElement.boolean(true), deserializeEmpty("\"Value\"", Type.string)
        )
        assertNotEquals(
            MemoryElement.string("Value"), deserializeEmpty("10", Type.number)
        )
        assertNotEquals(
            MemoryElement.number(10), deserializeEmpty("true", Type.boolean)
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

        assertEquals(
            MemoryElement.array(
                Type.array(type1), listOf(MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10)))))
            ), deserializeEmpty("[{\"value\": 10}]", Type.array(type1))
        )

        assertEquals(
            MemoryElement.array(
                Type.array(type2), listOf(
                    MemoryElement.property(
                        type2, mapOf(Pair("value1", MemoryElement.number(10)), Pair("value2", MemoryElement.number(12)))
                    )
                )
            ), deserializeEmpty("[{\"value1\": 10, \"value2\": 12}]", Type.array(type2))
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

        assertEquals(
            MemoryElement.property(type1, mapOf(Pair("value", MemoryElement.number(10)))),
            deserializeEmpty("{\"value\": 10}", type1)
        )

        assertEquals(
            MemoryElement.property(
                type2, mapOf(Pair("value1", MemoryElement.number(11)), Pair("value2", MemoryElement.number(12)))
            ), deserializeEmpty("{\"value1\": 11, \"value2\":12}", type2)
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