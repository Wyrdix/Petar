package typing

import fr.univ_lille.iut_info.Program
import fr.univ_lille.iut_info.ProgramData
import fr.univ_lille.iut_info.Type
import fr.univ_lille.iut_info.steps.NameStep
import fr.univ_lille.iut_info.steps.TypecheckStep
import fr.univ_lille.iut_info.steps.check
import fr.univ_lille.iut_info.steps.isAssignableFrom
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse

class TypingAssignableFromTest {
    val emptyContext = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))

    fun Type.emptyIsAssignableFrom(type: Type): Boolean {
        return isAssignableFrom(emptyContext, type)
    }

    @Test
    fun testAnyAssignableFrom() {
        assert(Type.any.emptyIsAssignableFrom(Type.boolean))
        assert(Type.any.emptyIsAssignableFrom(Type.number))
        assert(Type.any.emptyIsAssignableFrom(Type.string))
        assert(Type.any.emptyIsAssignableFrom(Type.bottom))
        assert(Type.any.emptyIsAssignableFrom(Type.undefined))
        assert(Type.any.emptyIsAssignableFrom(Type.any))

        assertFalse(Type.boolean.emptyIsAssignableFrom(Type.any))
        assertFalse(Type.number.emptyIsAssignableFrom(Type.any))
        assertFalse(Type.string.emptyIsAssignableFrom(Type.any))
        assertFalse(Type.bottom.emptyIsAssignableFrom(Type.any))
        assertFalse(Type.undefined.emptyIsAssignableFrom(Type.any))
    }

    @Test
    fun testNullableAssignableFrom() {
        assert(Type.nullable(Type.string).emptyIsAssignableFrom(Type.string))
        assert(Type.nullable(Type.number).emptyIsAssignableFrom(Type.number))
        assert(Type.nullable(Type.boolean).emptyIsAssignableFrom(Type.boolean))
        assert(Type.nullable(Type.any).emptyIsAssignableFrom(Type.any))

        assertFalse(Type.string.emptyIsAssignableFrom(Type.nullable(Type.string)))
        assertFalse(Type.number.emptyIsAssignableFrom(Type.nullable(Type.number)))
        assertFalse(Type.boolean.emptyIsAssignableFrom(Type.nullable(Type.boolean)))
        assertFalse(Type.any.emptyIsAssignableFrom(Type.nullable(Type.any)))
    }

    @Test
    fun testArrayAssignableFrom() {
        assert(Type.any.emptyIsAssignableFrom(Type.array(Type.number)))
        assert(Type.array(Type.number).emptyIsAssignableFrom(Type.array(Type.number)))
        assert(Type.array(Type.any).emptyIsAssignableFrom(Type.array(Type.number)))
        assert(Type.array(Type.nullable(Type.number)).emptyIsAssignableFrom(Type.array(Type.number)))
    }

    @Test
    fun testPropertyAssignableFrom() {
        val type1 = Type.property("Root", mapOf(Pair("value", Type.number)))
        val type2 = Type.property("Root1", mapOf(Pair("value1", Type.nullable(Type.string))), Pair("Root", emptyList()))
        val type3 = Type.property("Root2", emptyMap(), Pair("Root1", listOf(Pair("value1", Type.string))))

        val context = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))
        context.typeNameMap[type1.identifier] = type1
        context.typeNameMap[type2.identifier] = type2
        context.typeNameMap[type3.identifier] = type3

        assert(type1.isAssignableFrom(context, type1))
        assert(type2.isAssignableFrom(context, type2))
        assert(type3.isAssignableFrom(context, type3))
        assertFalse(type3.isAssignableFrom(context, type1))
        assertFalse(type2.isAssignableFrom(context, type1))

        assert(type1.isAssignableFrom(context, type2))
        assert(type1.isAssignableFrom(context, type3))
        assertDoesNotThrow { type1.check(context) }
        assertDoesNotThrow { type2.check(context) }
        assertDoesNotThrow { type3.check(context) }
        assertThrows<IllegalStateException> {
            Type.property(
                "Root2",
                emptyMap(),
                Pair("Root1", listOf(Pair("value1", Type.number)))
            ).check(context)
        }
        assertThrows<IllegalStateException> {
            Type.property(
                "Root2",
                mapOf(Pair("value1", Type.string)),
                Pair("Root1", emptyList())
            ).check(context)
        }
    }
}