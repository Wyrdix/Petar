package typing

import fr.univ_lille.iut_info.Program
import fr.univ_lille.iut_info.ProgramData
import fr.univ_lille.iut_info.PropertyType
import fr.univ_lille.iut_info.Type
import fr.univ_lille.iut_info.steps.NameStep
import fr.univ_lille.iut_info.steps.StepError
import fr.univ_lille.iut_info.steps.TypecheckStep
import fr.univ_lille.iut_info.steps.check
import fr.univ_lille.iut_info.steps.getAllFields
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class TypingPropertyCheckTest {
    val emptyContext = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))

    @Test
    fun testMultipleKey() {
        assertThrows<StepError> {
            PropertyType("_", listOf(Pair("value", Type.number), Pair("value", Type.number))).check(emptyContext)
        }

        assertThrows<StepError> {
            PropertyType("_", listOf(Pair("value", Type.number), Pair("value", Type.string))).check(emptyContext)
        }

        assertDoesNotThrow {
            PropertyType(
                "_", listOf(Pair("value", Type.number), Pair("value1", Type.string), Pair("value2", Type.number))
            ).check(emptyContext)
        }
    }

    @Test
    fun testParentKey() {
        val type1 = Type.property("Root", mapOf(Pair("value", Type.number)))

        val context = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))
        context.typeNameMap[type1.identifier] = type1

        assertThrows<StepError> {
            PropertyType(
                "_", listOf(Pair("value", Type.number)), Pair("Root", emptyList())
            ).check(context)
        }

        assertDoesNotThrow {
            PropertyType(
                "_", listOf(Pair("value1", Type.number)), Pair("Root", emptyList())
            ).check(context)
        }

        assertDoesNotThrow {
            PropertyType(
                "_", listOf(Pair("value1", Type.number)), Pair("Root", listOf(Pair("value", Type.number)))
            ).check(context)
        }

        assertThrows<StepError> {
            PropertyType(
                "_", listOf(Pair("value1", Type.number)), Pair("Root", listOf(Pair("value", Type.string)))
            ).check(context)
        }
    }

    @Test
    fun getAllFieldsProperty() {
        val context = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))
        val type1 = Type.property("1", HashMap<String, Type>().apply {
            put("value", Type.number)
        })
        context.typeNameMap["1"] = type1

        assertEquals(type1.run { check(context); getAllFields(context) }, mapOf(Pair("value", Type.number)))

        assertEquals(
            Type.property("2", HashMap<String, Type>().apply {
            put("value1", Type.string)
        }, Pair("1", emptyList())).run { check(context); getAllFields(context) },
            mapOf(Pair("value", Type.number), Pair("value1", Type.string))
        )

        val type3 = Type.property("3", HashMap<String, Type>().apply {
            put("value", Type.optional(Type.number))
        })
        context.typeNameMap["3"] = type3

        assertEquals(
            Type.property("4", HashMap<String, Type>().apply {
            put("value3", Type.bottom)
        }, Pair("3", listOf(Pair("value", Type.number)))).run { check(context); getAllFields(context) },
            mapOf(Pair("value", Type.number), Pair("value3", Type.bottom))
        )
    }
}