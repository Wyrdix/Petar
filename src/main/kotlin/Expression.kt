package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.memory.*
import java.util.UUID
import kotlin.math.floor

typealias Context = Map<String, MemoryElement>

abstract class Expression : Visitable<Expression> {
    val id = UUID.randomUUID().node()

    override fun hashCode(): Int {
        return id.hashCode()
    }

    abstract fun evaluate(context: Context): MemoryElement?

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Expression

        if (id != other.id) return false

        return true
    }
}

abstract class ExpressionAccess : Expression() {
    abstract val parent: ExpressionAccess?

    data class Index(
        override val parent: ExpressionAccess, val expression: Expression
    ) : ExpressionAccess() {
        override fun accept(visitor: Visitor<Expression>): Expression {
            return Index(visitor.visit(parent) as ExpressionAccess, visitor.visit(expression))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val parentElement = parent.evaluate(context)
            if (parentElement !is MemoryArray) return null

            val indexElement = expression.evaluate(context)
            if (indexElement !is MemoryNumber) return null

            val array = parentElement.value
            val rawIndex = indexElement.value

            if (floor(rawIndex.toDouble()) != rawIndex.toDouble()) return null
            val intIndex = rawIndex.toInt()
            if (intIndex >= array.size || intIndex < -array.size) return null

            val index = (intIndex + array.size) % array.size
            return array[index]
        }

    }

    data class Member(
        override val parent: ExpressionAccess?, val identifier: String
    ) : ExpressionAccess() {
        override fun accept(visitor: Visitor<Expression>): Expression {
            return Member(parent?.run(visitor::visit) as ExpressionAccess?, identifier)
        }

        override fun evaluate(context: Context): MemoryElement? {
            val parent = this.parent?.evaluate(context) ?: return context[identifier]

            if (parent !is MemoryObject) return null
            return parent.value[identifier]
        }

    }
}

abstract class LiteralExpression : Expression() {

    override fun accept(visitor: Visitor<Expression>): Expression {
        return this
    }

    data class EString(val value: String) : LiteralExpression() {
        override fun evaluate(context: Context): MemoryElement {
            return MemoryString(value)
        }
    }

    data class ENumber(val value: Float) : LiteralExpression() {
        override fun evaluate(context: Context): MemoryElement {
            return MemoryNumber(value)
        }

    }

    data class EBoolean(val value: Boolean) : LiteralExpression() {
        override fun evaluate(context: Context): MemoryElement {
            return MemoryBoolean(value)
        }
    }

    class EUndefined : LiteralExpression() {
        override fun evaluate(context: Context): MemoryElement? {
            return null
        }
    }
}

abstract class BinaryExpression : Expression() {
    abstract val left: Expression
    abstract val right: Expression
    abstract val operandType: Type?
    abstract val resultType: Type


    data class And(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return And(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryBoolean((left as MemoryBoolean).value && (right as MemoryBoolean).value)
        }
    }

    data class Or(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Or(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryBoolean((left as MemoryBoolean).value || (right as MemoryBoolean).value)
        }
    }

    data class Multiply(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Multiply(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryNumber((left as MemoryNumber).value * (right as MemoryNumber).value)
        }
    }

    data class Divide(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Divide(visitor.visit(left), visitor.visit(right))
        }


        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null
            if ((right as MemoryNumber).value == 0f) return null

            return MemoryNumber((left as MemoryNumber).value * right.value)
        }
    }

    data class Plus(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Plus(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryNumber((left as MemoryNumber).value + (right as MemoryNumber).value)
        }
    }

    data class Minus(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Minus(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryNumber((left as MemoryNumber).value - (right as MemoryNumber).value)
        }
    }
}

data class PatternMatchExpression(val left: Expression, val right: Pattern) : Expression() {
    override fun accept(visitor: Visitor<Expression>): Expression {
        return PatternMatchExpression(visitor.visit(left), right.visit {
            if (it is ExpressionPattern) {
                return@visit ExpressionPattern(visitor.visit(it.value), name = it.name)
            }
            return@visit null
        })
    }

    override fun evaluate(context: Context): MemoryElement? {
        TODO()
    }
}

abstract class UnaryExpression : Expression() {
    abstract val operand: Expression
    abstract val operandAndResultType: Type

    data class Negate(override val operand: Expression) : UnaryExpression() {
        override val operandAndResultType: Type
            get() = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Negate(visitor.visit(operand))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val operand = operand.evaluate(context) ?: return null

            if (operand !is MemoryBoolean) return null
            return MemoryBoolean(!operand.value)
        }
    }

    data class Opposite(override val operand: Expression) : UnaryExpression() {
        override val operandAndResultType: Type
            get() = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Opposite(visitor.visit(operand))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val operand = operand.evaluate(context) ?: return null

            if (operand !is MemoryNumber) return null
            return MemoryNumber(-operand.value)
        }
    }
}

data class ArrayExpression(val values: List<Expression>) : Expression() {
    override fun accept(visitor: Visitor<Expression>): Expression {
        return ArrayExpression(values.map(visitor::visit))
    }

    override fun evaluate(context: Context): MemoryElement? {
        val nullableEvaluatedValues = values.map { it.evaluate(context) }
        if (nullableEvaluatedValues.contains(null)) return null
        val type = checkedType ?: return null

        val evaluatedValues = nullableEvaluatedValues.map { value -> value!! }
        return MemoryArray(type as ArrayType, evaluatedValues)
    }

}

data class UnorderedArrayExpression(val values: List<Expression>) : Expression() {
    override fun accept(visitor: Visitor<Expression>): Expression {
        return UnorderedArrayExpression(values.map(visitor::visit))
    }

    override fun evaluate(context: Context): MemoryElement? {
        val nullableEvaluatedValues = values.map { it.evaluate(context) }
        if (nullableEvaluatedValues.contains(null)) return null
        val type = checkedType ?: return null

        val evaluatedValues = nullableEvaluatedValues.map { value -> value!! }
        return MemoryArray(type as ArrayType, evaluatedValues)
    }

}

data class PropertyExpression(
    val identifier: String,
    val fields: List<Pair<String, Expression>>,
    val parent: Expression? = null
) : Expression() {

    val mapFields
        get() = fields.associateBy({ it.first }, { it.second })

    override fun accept(visitor: Visitor<Expression>): Expression {
        return PropertyExpression(
            identifier,
            fields.map { Pair(it.first, visitor.visit(it.second)) },
            parent?.let { visitor.visit(it) })
    }

    override fun evaluate(context: Context): MemoryObject? {
        val nullableEvaluatedFields = mapFields.mapValues { (_, value) -> value.evaluate(context) }
        if (nullableEvaluatedFields.containsValue(null)) return null
        val type = checkedType ?: return null

        val evaluatedFields = nullableEvaluatedFields.mapValues { (_, value) -> value!! }
        return MemoryObject(type as PropertyType, evaluatedFields)
    }

}