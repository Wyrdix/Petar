package naming

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.steps.NameStep
import fr.univ_lille.iut_info.steps.initial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TypeNamingTest {
    val emptyContext = NameStep(Program(ProgramData(emptyList())))

    @Test
    fun primitiveRegistered() {
        assertEquals(Type.undefined, emptyContext.typeNameMap[Type.undefined.toString()])
        assertEquals(Type.string, emptyContext.typeNameMap[Type.string.toString()])
        assertEquals(Type.number, emptyContext.typeNameMap[Type.number.toString()])
        assertEquals(Type.boolean, emptyContext.typeNameMap[Type.boolean.toString()])
        assertEquals(Type.any, emptyContext.typeNameMap[Type.any.toString()])
    }

    @Test
    fun expressionParent() {
        val context = NameStep(Program(ProgramData(emptyList())))
        val e1 = BinaryExpression.Plus(LiteralExpression.ENumber(1f), LiteralExpression.ENumber(1f))
        initial(context, e1)
        assertEquals(e1, context.expressionParentMap[e1.left])
        assertEquals(e1, context.expressionParentMap[e1.right])
        assertEquals(listOf(e1.left, e1.right), context.expressionChildrenMap[e1])
    }

    @Test
    fun patternParent() {
        val context = NameStep(Program(ProgramData(emptyList())))
        val p1 = ArrayPattern(
            listOf(
                ExpressionPattern(value = LiteralExpression.ENumber(1f), meta = PatternMeta()),
                ExpressionPattern(value = LiteralExpression.ENumber(2f), meta = PatternMeta())
            ),
            meta = PatternMeta()
        )
        initial(context, p1)
        assertEquals(p1, context.patternParentMap[p1.values[0]])
        assertEquals(p1, context.patternParentMap[p1.values[1]])
        assertEquals(p1.values, context.patternChildrenMap[p1])
    }
}