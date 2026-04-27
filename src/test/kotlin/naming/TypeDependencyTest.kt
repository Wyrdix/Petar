package naming

import fr.univ_lille.iut_info.Program
import fr.univ_lille.iut_info.ProgramData
import fr.univ_lille.iut_info.Type
import fr.univ_lille.iut_info.steps.NameStep
import fr.univ_lille.iut_info.steps.getTypeDependencies
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TypeDependencyTest {

    val emptyContext = NameStep(Program(ProgramData(emptyList())))

    @Test
    fun primitive() {
        assertEquals(setOf(Type.number.toString()), emptyContext.getTypeDependencies(Type.number))
        assertEquals(setOf(Type.string.toString()), emptyContext.getTypeDependencies(Type.string))
        assertEquals(setOf(Type.boolean.toString()), emptyContext.getTypeDependencies(Type.boolean))
        assertEquals(setOf(Type.undefined.toString()), emptyContext.getTypeDependencies(Type.undefined))
    }

    @Test
    fun reference() {

        val context = NameStep(Program(ProgramData(emptyList())))

        context.typeNameMap["test1"] = Type.number
        context.typeNameMap["test2"] = Type.reference("test1")

        assertEquals(setOf(Type.number.toString(), "test1"), context.getTypeDependencies(Type.reference("test1")))
        assertEquals(
            setOf(Type.number.toString(), "test1", "test2"), context.getTypeDependencies(Type.reference("test2"))
        )
    }

    @Test
    fun property() {

        val context = NameStep(Program(ProgramData(emptyList())))

        context.typeNameMap["test1"] = Type.property("test1", mapOf("a" to Type.number, "b" to Type.boolean))
        context.typeNameMap["test2"] =
            Type.property("test2", mapOf("a" to Type.reference("test1"), "b" to Type.optional(Type.reference("test2"))))

        assertEquals(
            setOf(Type.number.toString(), Type.boolean.toString(), "test1"),
            context.getTypeDependencies(Type.reference("test1"))
        )
        assertEquals(
            setOf(Type.number.toString(), Type.boolean.toString(), "test1", "test2", "undefined"),
            context.getTypeDependencies(Type.reference("test2"))
        )
    }
}