import fr.univ_lille.iut_info.Type
import fr.univ_lille.iut_info.memory.MemoryElement.Companion.memory
import fr.univ_lille.iut_info.memory.safeCheck
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypecheckTest {
    @Test
    fun numberTypecheck() {
        assertTrue { Type.number.safeCheck(memory(10f)) }
        assertFalse { Type.number.safeCheck(memory(true)) }
        assertFalse { Type.number.safeCheck(memory("aaaa")) }
    }

    @Test
    fun stringTypecheck() {
        assertTrue { Type.string.safeCheck(memory("11")) }
        assertFalse { Type.string.safeCheck(memory(true)) }
        assertFalse { Type.string.safeCheck(memory(10f)) }
    }

    @Test
    fun booleanTypecheck() {
        assertTrue { Type.boolean.safeCheck(memory(true)) }
        assertFalse { Type.boolean.safeCheck(memory("11")) }
        assertFalse { Type.boolean.safeCheck(memory(10f)) }
    }

    @Test
    fun primitiveArrayTypecheck() {
        assertTrue { Type.array(Type.boolean).safeCheck(listOf(true, false, false, true)) }
        assertTrue { Type.array(Type.boolean).safeCheck(listOf(true, false, false, memory(false))) }
        assertFalse { Type.array(Type.boolean).safeCheck(listOf(true, false, false, "1")) }
        assertTrue { Type.array(Type.boolean).safeCheck(emptyList<String>()) }
        assertTrue { Type.array(Type.number).safeCheck(listOf(1f, 3f, 5f, -2f)) }
    }

    @Test
    fun objectTypecheck() {
        val type1 = Type.objectT("Object", mapOf(Pair("a", Type.number)))
        val type2 = Type.objectT("Object", mapOf(Pair("a", Type.number), Pair("b", Type.boolean)))
        val type3 = Type.objectT("Object3", mapOf(Pair("a", Type.number), Pair("b", Type.reference("Object", type2))))
        assertTrue { type1.safeCheck(memory(type1, mapOf(Pair("a", memory(1f))))) }
        assertTrue { type2.safeCheck(memory(type2, mapOf(Pair("a", memory(1f)), Pair("b", true)))) }
        assertFalse { type2.safeCheck(memory(type1, mapOf(Pair("a", memory(1f))))) }
        assertFalse { type1.safeCheck(memory(type2, mapOf(Pair("a", memory(1f)), Pair("b", true)))) }


        assertTrue {
            type3.safeCheck(
                memory(
                    type3,
                    mapOf(Pair("a", memory(1f)), Pair("b", mapOf(Pair("a", memory(1f)), Pair("b", memory(false)))))
                )
            )
        }

        assertFalse {
            type3.safeCheck(
                memory(
                    Type.objectT(
                        "Object3",
                        mapOf(
                            Pair("a", Type.number),
                            Pair("b", Type.objectT("Object", mapOf(Pair("a", Type.number), Pair("b", Type.number))))
                        )
                    ),
                    mapOf(Pair("a", memory(1f)), Pair("b", mapOf(Pair("a", memory(1f)), Pair("b", memory(1f)))))
                )
            )
        }
    }


}