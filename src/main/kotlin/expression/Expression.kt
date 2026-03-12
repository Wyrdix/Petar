package fr.univ_lille.iut_info.expression

import fr.univ_lille.iut_info.type.Type

interface Expression;

interface ExpressionAccess : Expression {
    val parent: ExpressionAccess?

    data class Index(
        override val parent: ExpressionAccess,
        val expression: Expression
    ) : ExpressionAccess

    data class Member(
        override val parent: ExpressionAccess?,
        val identifier: String
    ) : ExpressionAccess
}

interface LiteralExpression : Expression {
    data class LString(val value: String) : LiteralExpression
    data class LNumber(val value: Float) : LiteralExpression
    data class LBoolean(val value: Boolean) : LiteralExpression
}

interface BinaryExpression : Expression {
    val left: Expression
    val right: Expression
    val operandType: Type?
    val resultType: Type


    data class And(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean
    }

    data class Or(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean
    }

    data class Multiply(override val left: Expression, override val right: Expression) :
        BinaryExpression {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number
    }

    data class Divide(override val left: Expression, override val right: Expression) :
        BinaryExpression {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number
    }

    data class Plus(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number
    }

    data class Minus(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number
    }

    data class Equal(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.boolean
        override val operandType: Type? = null
    }
}

interface UnaryExpression : Expression {
    val operand: Expression
    val type: Type

    data class Negate(override val operand: Expression) : UnaryExpression {
        override val type: Type
            get() = Type.boolean
    }

    data class Opposite(override val operand: Expression) : UnaryExpression {
        override val type: Type
            get() = Type.number
    }
}

data class ArrayExpression(val values: List<Expression>) : Expression
data class ObjectExpression(val identifier: String, val fields: List<Pair<String, Expression>>) : Expression