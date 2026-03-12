package fr.univ_lille.iut_info.expression

import fr.univ_lille.iut_info.type.Type
import fr.univ_lille.iut_info.visitable.Visitable
import fr.univ_lille.iut_info.visitable.Visitor

interface Expression : Visitable<Expression>

interface ExpressionAccess : Expression {
    val parent: ExpressionAccess?

    data class Index(
        override val parent: ExpressionAccess, val expression: Expression
    ) : ExpressionAccess {
        override fun accept(visitor: Visitor<Expression>): Expression {
            return Index(visitor.visit(parent) as ExpressionAccess, visitor.visit(expression))
        }
    }

    data class Member(
        override val parent: ExpressionAccess?, val identifier: String
    ) : ExpressionAccess {
        override fun accept(visitor: Visitor<Expression>): Expression {
            return Member(parent?.run(visitor::visit) as ExpressionAccess?, identifier)
        }
    }
}

interface LiteralExpression : Expression {

    override fun accept(visitor: Visitor<Expression>): Expression {
        return this
    }

    data class EString(val value: String) : LiteralExpression
    data class ENumber(val value: Float) : LiteralExpression
    data class EBoolean(val value: Boolean) : LiteralExpression
}

interface BinaryExpression : Expression {
    val left: Expression
    val right: Expression
    val operandType: Type?
    val resultType: Type

    data class And(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return And(visitor.visit(left), visitor.visit(right))
        }
    }

    data class Or(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Or(visitor.visit(left), visitor.visit(right))
        }
    }

    data class Multiply(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Multiply(visitor.visit(left), visitor.visit(right))
        }
    }

    data class Divide(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Divide(visitor.visit(left), visitor.visit(right))
        }
    }

    data class Plus(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Plus(visitor.visit(left), visitor.visit(right))
        }
    }

    data class Minus(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Minus(visitor.visit(left), visitor.visit(right))
        }
    }

    data class Equal(override val left: Expression, override val right: Expression) : BinaryExpression {
        override val resultType: Type = Type.boolean
        override val operandType: Type? = null

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Equal(visitor.visit(left), visitor.visit(right))
        }
    }
}

interface UnaryExpression : Expression {
    val operand: Expression
    val type: Type

    data class Negate(override val operand: Expression) : UnaryExpression {
        override val type: Type
            get() = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Negate(visitor.visit(operand))
        }
    }

    data class Opposite(override val operand: Expression) : UnaryExpression {
        override val type: Type
            get() = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Opposite(visitor.visit(operand))
        }
    }
}

data class ArrayExpression(val values: List<Expression>) : Expression {
    override fun accept(visitor: Visitor<Expression>): Expression {
        return ArrayExpression(values.map(visitor::visit))
    }
}

data class ObjectExpression(val identifier: String, val fields: List<Pair<String, Expression>>) : Expression {
    override fun accept(visitor: Visitor<Expression>): Expression {
        return ObjectExpression(identifier, fields.map { Pair(it.first, visitor.visit(it.second)) })
    }
}