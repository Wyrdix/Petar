package typing

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.steps.NameStep
import fr.univ_lille.iut_info.steps.TypecheckStep
import fr.univ_lille.iut_info.steps.typeCheck
import org.junit.jupiter.api.Test

class TypeCheckerTest {
    val emptyContext = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))

    @Test
    fun primitive() {
        assert(LiteralExpression.EString("").typeCheck(emptyContext, Type.string))
        assert(LiteralExpression.ENumber(1f).typeCheck(emptyContext, Type.number))
        assert(LiteralExpression.EBoolean(false).typeCheck(emptyContext, Type.boolean))
        assert(ExpressionPattern(LiteralExpression.EString(""), PatternMeta()).typeCheck(emptyContext, Type.string))
        assert(ExpressionPattern(LiteralExpression.ENumber(1f), PatternMeta()).typeCheck(emptyContext, Type.number))
        assert(
            ExpressionPattern(LiteralExpression.EBoolean(false), PatternMeta()).typeCheck(
                emptyContext,
                Type.boolean
            )
        )

        assert(PrimitiveTypePattern("Number", PatternMeta()).typeCheck(emptyContext, Type.number))
        assert(PrimitiveTypePattern("String", PatternMeta()).typeCheck(emptyContext, Type.string))
        assert(PrimitiveTypePattern("Boolean", PatternMeta()).typeCheck(emptyContext, Type.boolean))
    }

    @Test
    fun property() {
        val context = TypecheckStep(NameStep(Program(ProgramData(emptyList()))))
        val prop1 = Type.property("Prop1", mapOf("field1" to Type.string, "field2" to Type.number))
        context.typeNameMap["Prop1"] = prop1

        assert(
            ExpressionPattern(
                PropertyExpression(
                    "Prop1",
                    listOf("field1" to LiteralExpression.EString("A"), "field2" to LiteralExpression.ENumber(1f))
                ),
                meta = PatternMeta()
            ).typeCheck(context, prop1)
        )
    }

    @Test
    fun array() {
        assert(
            ArrayPattern(
                listOf(
                    ExpressionPattern(LiteralExpression.EString(""), meta = PatternMeta())
                ),
                PatternMeta()
            ).typeCheck(emptyContext, Type.array(Type.string))
        )


        assert(
            ArrayPattern(
                listOf(
                    ExpressionPattern(LiteralExpression.EString(""), meta = PatternMeta(modifier = PatternModifier.AT_LEAST_ONE))
                ),
                PatternMeta()
            ).typeCheck(emptyContext, Type.array(Type.string))
        )

        assert(
            ArrayPattern(
                listOf(
                    ExpressionPattern(LiteralExpression.EString(""), meta = PatternMeta(modifier = PatternModifier.AT_LEAST_ONE)),
                    ExpressionPattern(LiteralExpression.EString(""), meta = PatternMeta(modifier = PatternModifier.ONE)),
                    ExpressionPattern(LiteralExpression.EString(""), meta = PatternMeta(modifier = PatternModifier.AT_LEAST_ONE))
                ),
                PatternMeta()
            ).typeCheck(emptyContext, Type.array(Type.string))
        )
    }
}