package fr.univ_lille.iut_info

import java.util.*

sealed class Expression : Visitable<Expression> {
    val id = UUID.randomUUID().toString()

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Expression

        return id == other.id
    }
}

sealed class ExpressionAccess : Expression() {
    abstract val parent: ExpressionAccess?

    class Index(
        override val parent: ExpressionAccess, val expression: Expression
    ) : ExpressionAccess() {
        override fun accept(visitor: Visitor<Expression>): Expression {
            return Index(visitor.visit(parent) as ExpressionAccess, visitor.visit(expression))
        }

    }

    class Member(
        override val parent: ExpressionAccess?, val identifier: String
    ) : ExpressionAccess() {
        override fun accept(visitor: Visitor<Expression>): Expression {
            return Member(parent?.run(visitor::visit) as ExpressionAccess?, identifier)
        }

    }
}

sealed class LiteralExpression : Expression() {

    override fun accept(visitor: Visitor<Expression>): Expression {
        return this
    }

    class EString(val value: String) : LiteralExpression()

    class ENumber(val value: Float) : LiteralExpression()

    class EBoolean(val value: Boolean) : LiteralExpression()

    class EUndefined : LiteralExpression() {
        override fun toString(): String {
            return "EUndefined"
        }
    }
}

sealed class BinaryExpression : Expression() {
    abstract val left: Expression
    abstract val right: Expression
    abstract val operandType: Type?
    abstract val resultType: Type


    class And(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return And(visitor.visit(left), visitor.visit(right))
        }

    }

    class Or(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Or(visitor.visit(left), visitor.visit(right))
        }

    }

    class Multiply(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Multiply(visitor.visit(left), visitor.visit(right))
        }

    }

    class Divide(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Divide(visitor.visit(left), visitor.visit(right))
        }


    }

    class Plus(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Plus(visitor.visit(left), visitor.visit(right))
        }

    }

    class Minus(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Minus(visitor.visit(left), visitor.visit(right))
        }

    }
}

class PatternMatchExpression(val left: Expression, val right: Pattern) : Expression() {
    override fun accept(visitor: Visitor<Expression>): Expression {
        return PatternMatchExpression(visitor.visit(left), right.visit {
            if (it is ExpressionPattern) {
                return@visit ExpressionPattern(visitor.visit(it.value), it.meta)
            }
            return@visit null
        })
    }

}

sealed class UnaryExpression : Expression() {
    abstract val operand: Expression
    abstract val operandAndResultType: Type

    class Negate(override val operand: Expression) : UnaryExpression() {
        override val operandAndResultType: Type
            get() = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Negate(visitor.visit(operand))
        }

    }

    class Opposite(override val operand: Expression) : UnaryExpression() {
        override val operandAndResultType: Type
            get() = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Opposite(visitor.visit(operand))
        }

    }
}

class ArrayExpression(val values: List<Expression>) : Expression() {
    override fun accept(visitor: Visitor<Expression>): Expression {
        return ArrayExpression(values.map(visitor::visit))
    }

}

class FunctionCallExpression(val name: String, val arguments: List<Pattern>) : Expression() {
    override fun accept(visitor: Visitor<Expression>): Expression {
        return FunctionCallExpression(name, arguments.map { arg ->
            arg.visit {
                if (it is ExpressionPattern) {
                    return@visit ExpressionPattern(visitor.visit(it.value), it.meta)
                }
                return@visit null
            }
        })
    }
}

class PropertyExpression(
    val identifier: String,
    val inlinedFields: List<Pair<String, Expression>>,
    val parent: Expression? = null
) : Expression() {

    val fields
        get() = inlinedFields.associate { it }

    override fun accept(visitor: Visitor<Expression>): Expression {
        return PropertyExpression(
            identifier,
            inlinedFields.map { Pair(it.first, visitor.visit(it.second)) },
            parent?.let { visitor.visit(it) })
    }

}